package com.example.eac_backend_project.files;

import com.example.eac_backend_project.entity.inventory.User;
import com.example.eac_backend_project.repository.UserRepository;
import com.example.eac_backend_project.Service.PageService;
import com.example.eac_backend_project.Service.ActivityLogService;
import com.example.eac_backend_project.Service.email.EmailService;
import com.example.eac_backend_project.Service.FrontendUrlService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.*;

@Service @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED,rollbackFor=Exception.class)
public class FileSuiteService {
    @org.springframework.beans.factory.annotation.Autowired private FileWorkReturnRepository workReturns;
    private final OfficeSheetGuard sheetGuard;
    private final OfficeRememberedPasswordRepository rememberedPasswords;
    private final FileItemRepository items;
    private final FileVersionRepository versions;
    private final FilePermissionRepository permissions;
    private final FileStarRepository stars;
    private final FileSettingsRepository settings;
    private final FileEventRepository events;
    private final UserRepository users;
    @org.springframework.beans.factory.annotation.Autowired private com.example.eac_backend_project.repository.EmployeeRepository employeeDirectory;
    private final PageService pages;
    private final ActivityLogService activity;
    private final EmailService email;
    private final FileContentStore store;
    private final FileCommentRepository comments;
    private final FileGuestRepository guests;
    private final FileCollaborationRepository collaboration;
    private final FileQuotaService quotas;
    private final FileOperationService operations;
    private final FileNotificationService notifications;
    private final FileNotificationStateRepository notificationState;
    private final String collaborationKey;
    private final com.fasterxml.jackson.databind.ObjectMapper mapper;
    private final String frontend;

    public FileSuiteService(FileItemRepository items,FileVersionRepository versions,FilePermissionRepository permissions,
            FileStarRepository stars,FileSettingsRepository settings,FileEventRepository events,UserRepository users,
            PageService pages,ActivityLogService activity,EmailService email,FileContentStore store,FileCommentRepository comments,FileGuestRepository guests,
            @Value("${app.frontend.base-url:http://localhost:5173}") String frontend,FileCollaborationRepository collaboration,
            @Value("${app.files.collaboration-key:}")String collaborationKey,com.fasterxml.jackson.databind.ObjectMapper mapper,FileQuotaService quotas,FileOperationService operations,FileNotificationService notifications,FileNotificationStateRepository notificationState,OfficeSheetGuard sheetGuard,OfficeRememberedPasswordRepository rememberedPasswords) {
        this.rememberedPasswords=rememberedPasswords;this.sheetGuard=sheetGuard;this.items=items; this.versions=versions; this.permissions=permissions; this.stars=stars; this.settings=settings;
        this.events=events; this.users=users; this.pages=pages; this.activity=activity; this.email=email; this.store=store; this.comments=comments; this.guests=guests; this.frontend=frontend;
        this.collaboration=collaboration;this.collaborationKey=collaborationKey;this.mapper=mapper;
        this.quotas=quotas;this.operations=operations;this.notifications=notifications;this.notificationState=notificationState;
    }
    public record ItemView(Long id,String name,boolean folder,Long parentId,Long ownerId,String owner,String mediaType,
            long sizeBytes,long currentVersion,long revision,Instant createdAt,Instant updatedAt,Instant deletedAt,String deletedBy,
            String access,boolean starred,boolean editorEnabled,boolean canEdit,boolean hasContents,boolean canDownload) {}
    public record VersionView(long number,long sizeBytes,String checksum,String editor,Instant createdAt,String reason,String mediaType) {}
    public record ShareView(Long id,Long userId,String name,String email,String access,Instant grantedAt,Instant expiresAt,boolean allowDownload) {}
    public record Person(Long userId,String name,String email,String employeeId,String department) {}

    public User currentUser() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) throw fail(401,"Sign in required");
        User user=users.findByUserName(auth.getName()).orElseThrow(()->fail(401,"Account not found"));
        if(!Boolean.TRUE.equals(user.getEnabled())) throw fail(403,"Account disabled");
        if(!admin(user) && pages.getAccessiblePages(auth).stream().noneMatch(p->"/files".equals(p.getPath()))) throw fail(403,"File Management page access is required");
        operations.require(user,"FILE_VIEW");return user;
    }
    public static boolean admin(User u) { return u.getRole()!=null && "ROLE_ADMIN".equals(u.getRole().getName()); }
    private static ResponseStatusException fail(int status,String message) { return new ResponseStatusException(HttpStatus.valueOf(status),message); }
    private FileItem item(Long id) { return items.findById(id).orElseThrow(()->fail(404,"File not found")); }
    private FileItem locked(Long id) { return items.lock(id).orElseThrow(()->fail(404,"File not found")); }
    private FileSettings config() { return settings.findById(1L).orElseGet(()->settings.saveAndFlush(new FileSettings())); }
    private FileSettings quotaLock() { config(); return settings.lock().orElseThrow(); }
    /** Ownership and grants inherit through ancestors; a deleted ancestor blocks all live operations. */
    String access(FileItem file,User user,boolean includeDeleted) {
        int rank=-1; Set<Long> seen=new HashSet<>(); FileItem node=file;
        while(node!=null) {
            if(!seen.add(node.getId())) throw fail(409,"Invalid folder cycle");
            if(!includeDeleted && node.getDeletedAt()!=null) throw fail(404,"File is in the recycle bin");
            if(admin(user) || node.getOwner().getId().equals(user.getId())) rank=4;
            var grant=permissions.findByItem_IdAndRecipient_Id(node.getId(),user.getId());
            if(grant.isPresent() && (grant.get().getExpiresAt()==null || grant.get().getExpiresAt().isAfter(Instant.now()))) rank=Math.max(rank,grant.get().getAccess().ordinal());
            node=node.getParent();
        }
        return rank<0?null:rank==4?"OWNER":FilePermission.Access.values()[rank].name();
    }
    /** A download denial on any active direct/ancestor grant overrides other grants. */
    boolean downloadAllowed(FileItem file,User user) {
        if("OWNER".equals(access(file,user,true))) return true;
        Set<Long> seen=new HashSet<>();
        for(FileItem node=file;node!=null;node=node.getParent()) {
            if(!seen.add(node.getId())) throw fail(409,"Invalid folder cycle");
            if(node.getOwner().getId().equals(user.getId())) return true;
            var grant=permissions.findByItem_IdAndRecipient_Id(node.getId(),user.getId());
            if(grant.isPresent()&&(grant.get().getExpiresAt()==null||grant.get().getExpiresAt().isAfter(Instant.now()))&&grant.get().isDownloadBlocked()) return false;
        }
        return true;
    }
    void requireDownloadAllowed(FileItem file,User user) { if(!downloadAllowed(file,user)) throw fail(403,"The owner has disabled original-file downloads for your account. Open this document online in EAC."); }
    private void require(FileItem file,User user,String needed,boolean deleted) {
        String role=access(file,user,deleted);
        int actual=role==null?-1:"OWNER".equals(role)?4:FilePermission.Access.valueOf(role).ordinal();
        if(actual<FilePermission.Access.valueOf(needed).ordinal()) throw fail(403,"You do not have permission for this file operation");
    }
    private ItemView view(FileItem file,User user,boolean deleted) {
        return new ItemView(file.getId(),file.getName(),file.isFolder(),file.getParent()==null?null:file.getParent().getId(),
            file.getOwner().getId(),file.getOwner().getName(),file.getMediaType(),file.getSizeBytes(),file.getCurrentVersion(),file.getRevision(),
            file.getCreatedAt(),file.getUpdatedAt(),file.getDeletedAt(),file.getDeletedBy(),access(file,user,deleted),stars.existsByItem_IdAndUser_Id(file.getId(),user.getId()),editorEnabled(file),editorEnabled(file)&&operations.allows(user,"FILE_EDIT")&&List.of("OWNER","MANAGER","EDITOR").contains(Objects.toString(access(file,user,deleted),"")),file.isFolder()&&(deleted?items.existsByParent_Id(file.getId()):items.existsByParent_IdAndDeletedAtIsNull(file.getId())),downloadAllowed(file,user)&&operations.allows(user,"FILE_DOWNLOAD")&&sheetGuard.wholeWorkbookAllowed(file,user));
    }
    private void event(FileItem file,User user,String action) {
        event(file,user,action,user.getUserName());
    }
    private void event(FileItem file,User user,String action,String actor) {
        FileEvent event=new FileEvent(); event.setItemId(file.getId()); event.setOwnerId(file.getOwner().getId()); event.setFilename(file.getName()); event.setUsername(actor); event.setAction(action);
        var request=org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if(request instanceof org.springframework.web.context.request.ServletRequestAttributes servlet) event.setIpAddress(servlet.getRequest().getRemoteAddr());
        events.save(event);
        activity.logSuccess("File "+action+": "+file.getName(),"FILE_SUITE",actor,file.getId());
    }
    private String filename(String name) {
        if(name==null || name.isBlank() || name.length()>config().getMaxFilenameLength() || name.matches(".*[\\\\/:*?\"<>|\\p{Cntrl}].*") || name.equals(".") || name.equals("..") || name.endsWith(".") || name.endsWith(" ")) throw fail(400,"Use a valid filename of at most "+config().getMaxFilenameLength()+" characters");
        String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT):"";
        if(Arrays.stream(config().getBlockedExtensions().split(",")).map(String::trim).anyMatch(ext::equals)) throw fail(400,"This file extension is blocked");
        return name.trim();
    }
    private FileItem parent(Long id,User user) {
        if(id==null) return null; FileItem parent=locked(id); require(parent,user,"EDITOR",false);
        if(!parent.isFolder()) throw fail(400,"Destination must be a folder"); return parent;
    }
    private void revision(FileItem file,Long expected) { if(expected==null || expected!=file.getRevision()) throw fail(409,"This file changed in another session. Reload before saving; your changes have not been overwritten."); }
    private boolean live(FileItem f){return f.getCollaborationRoom()!=null&&f.getCollaborationLeaseUntil()!=null&&f.getCollaborationLeaseUntil().isAfter(Instant.now());}
    private void noLive(FileItem f){if(live(f))throw fail(409,"Live collaboration is active. Join the live session before editing, or wait for it to close.");}
    // ONLYOFFICE uses the same ACL, quota, immutable content and version services.
    FileItem officeLock(Long id){quotaLock();return locked(id);}
    ItemView officeView(FileItem f){User user=currentUser();require(f,user,"VIEWER",false);return view(f,user,false);}
    void officeEvent(FileItem f,String action){event(f,currentUser(),action);}
    Download officeSource(FileItem f,long number){User user=currentUser();require(f,user,"VIEWER",false);FileVersion v=versions.findByItem_IdAndNumber(f.getId(),number).orElseThrow(()->fail(404,"Document version unavailable"));return new Download(store.path(v.getStorageKey()),f.getName(),v.getMediaType());}
    void officeAppend(FileItem f,byte[] data,String media,String key,long expected)throws IOException{User user=currentUser();operations.require(user,"FILE_EDIT");require(f,user,"EDITOR",false);editingAllowed(f);if(f.getCurrentVersion()!=expected||!Objects.equals(f.getCollaborationRoom(),"onlyoffice:"+key))throw fail(409,"Office session conflicts with the saved document; no content was overwritten");append(f,user,new ByteArrayInputStream(data),media,"ONLYOFFICE checkpoint");event(f,user,"ONLYOFFICE_SAVE");}
    private String epoch(FileItem f){return f.getCollaborationEpoch()==null?"initial-"+f.getId()+"-"+f.getCurrentVersion():f.getCollaborationEpoch();}
    private void newEpoch(FileItem f){f.setCollaborationEpoch(UUID.randomUUID().toString());f.setCollaborationRoom(null);f.setCollaborationLeaseUntil(null);items.saveAndFlush(f);}
    public Page<ItemView> list(String scope,Long parentId,String q,int page,int size) {
        User user=currentUser(); Pageable paging=PageRequest.of(Math.max(0,page),Math.max(1,Math.min(100,size)),Sort.by(Sort.Order.desc("folder"),Sort.Order.asc("name")));
        q=q==null?"":q.substring(0,Math.min(180,q.length()));
        if(parentId!=null) { FileItem parent=item(parentId); require(parent,user,"VIEWER",false); if(!parent.isFolder()) throw fail(400,"Not a folder"); }
        if("trash".equals(scope)) return (admin(user)?items.organizationTrash(q,paging):items.owned(user.getId(),true,q,paging)).map(f->view(f,user,true));
        if("shared".equals(scope)||"shared-by-me".equals(scope)||"starred".equals(scope)) {
            List<FileItem> source="starred".equals(scope)?stars.starred(user.getId()):("shared".equals(scope)?permissions.received(user.getId()):permissions.findByItem_Owner_Id(user.getId())).stream().map(FilePermission::getItem).distinct().toList();
            final String query=q.toLowerCase(Locale.ROOT);
            List<ItemView> result=source.stream().filter(f->f.getDeletedAt()==null && f.getName().toLowerCase(Locale.ROOT).contains(query)).filter(f->{try{return access(f,user,false)!=null;}catch(ResponseStatusException ex){return false;}}).map(f->view(f,user,false)).sorted(Comparator.comparing(ItemView::folder).reversed().thenComparing(ItemView::name,String.CASE_INSENSITIVE_ORDER)).toList();
            int start=(int)Math.min(result.size(),paging.getOffset()); return new PageImpl<>(result.subList(start,Math.min(result.size(),start+paging.getPageSize())),paging,result.size());
        }
        if("recent".equals(scope) || (parentId==null && !q.isBlank())) return items.owned(user.getId(),false,q,PageRequest.of(paging.getPageNumber(),paging.getPageSize(),Sort.by(Sort.Direction.DESC,"updatedAt"))).map(f->view(f,user,false));
        return items.children(user.getId(),parentId,q,paging).map(f->view(f,user,false));
    }
    public ItemView get(Long id) { User user=currentUser(); FileItem file=item(id); require(file,user,"VIEWER",false); event(file,user,"OPEN"); return view(file,user,false); }
    public List<ItemView> breadcrumbs(Long id) {
        User user=currentUser(); List<ItemView> result=new ArrayList<>(); FileItem file=id==null?null:item(id); Set<Long> seen=new HashSet<>();
        while(file!=null && seen.add(file.getId())) { if(access(file,user,false)==null) break; result.add(view(file,user,false)); file=file.getParent(); } Collections.reverse(result); return result;
    }
    public ItemView createFolder(String name,Long parentId) { quotaLock(); User user=currentUser();operations.require(user,"FILE_CREATE"); FileItem file=new FileItem(); file.setName(filename(name)); file.setFolder(true); file.setOwner(user); file.setParent(parent(parentId,user)); items.saveAndFlush(file); event(file,user,"CREATE_FOLDER"); return view(file,user,false); }
    private void append(FileItem file,User editor,InputStream stream,String mediaType,String reason) throws IOException {
        FileSettings cfg=quotaLock(); long remaining=Math.min(FileQuotaService.remaining(quotas.userLimit(file.getOwner().getId(),cfg.getUserQuotaBytes()),items.used(file.getOwner().getId())),FileQuotaService.remaining(cfg.getOrganizationQuotaBytes(),items.totalUsed()));remaining=Math.min(remaining,quotas.remainingFolder(file));
        if(remaining<=0) throw fail(413,"Storage quota reached; versions and recycled files count toward storage");
        var content=store.write(stream,Math.min(remaining,cfg.getMaxUploadBytes()));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){ @Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED) store.remove(content.key());} });
        FileVersion version=new FileVersion(); version.setItem(file); version.setNumber(file.getCurrentVersion()+1); version.setStorageKey(content.key()); version.setChecksum(content.checksum()); version.setSizeBytes(content.size()); version.setEditor(editor); version.setActorLabel(Objects.toString(editor.getName(),editor.getUserName())); version.setMediaType(mediaType); version.setReason(reason); versions.save(version); sheetGuard.markReplacement(file,reason);
        file.setCurrentVersion(version.getNumber()); file.setSizeBytes(content.size()); file.setMediaType(mediaType); file.setUpdatedAt(Instant.now()); items.saveAndFlush(file);storageNotices(cfg,file.getOwner());
    }
    private void storageNotices(FileSettings cfg,User owner){
        if(!cfg.isEmailNotifications())return;Instant now=Instant.now();long used=items.used(owner.getId()),limit=quotas.userLimit(owner.getId(),cfg.getUserQuotaBytes());
        if(limit>0&&(double)used/limit>=0.9){var state=notificationState.findById(owner.getId()).orElseGet(FileNotificationState::new);if(state.getLastStorageNotice()==null||state.getLastStorageNotice().isBefore(now.minusSeconds(86400))){if(notifications.storage(owner,"Your file storage is at least 90% full. Versions and recycled files count toward your quota.")){state.setUser(owner);state.setLastStorageNotice(now);notificationState.save(state);}}}
        if(cfg.getOrganizationQuotaBytes()>0&&(double)items.totalUsed()/cfg.getOrganizationQuotaBytes()>=0.9&&(cfg.getLastOrganizationStorageNotice()==null||cfg.getLastOrganizationStorageNotice().isBefore(now.minusSeconds(86400)))){notifications.organizationStorage("Organization file storage is at least 90% full. Review current files, versions, and recycled content.");cfg.setLastOrganizationStorageNotice(now);settings.save(cfg);}
    }
    public ItemView upload(MultipartFile upload,Long parentId) throws IOException { quotaLock();
        User user=currentUser();operations.require(user,"FILE_UPLOAD");operations.require(user,"FILE_CREATE"); FileItem file=new FileItem(); file.setOwner(user); file.setParent(parent(parentId,user)); file.setName(filename(upload.getOriginalFilename())); items.saveAndFlush(file);
        try(InputStream in=upload.getInputStream()) { append(file,user,in,safeMedia(upload.getContentType()),"Original upload"); } event(file,user,"UPLOAD"); return view(file,user,false);
    }
    ItemView recoverContent(Long id,String name,Long parentId,String mediaType,InputStream in,String reason)throws IOException{ quotaLock();
        User user=currentUser();if(!admin(user))throw fail(403,"Administrator required");FileItem file;
        if(id==null){file=new FileItem();file.setOwner(user);file.setParent(parent(parentId,user));file.setName(filename(name));items.saveAndFlush(file);}else{file=locked(id);require(file,user,"MANAGER",false);noLive(file);}
        append(file,user,in,safeMedia(mediaType),reason);event(file,user,"BACKUP_RECOVERY");return view(file,user,false);
    }
    static String safeMedia(String media) { return media!=null && media.matches("[\\w.+-]+/[\\w.+-]+")?media:"application/octet-stream"; }
    public ItemView save(Long id,MultipartFile upload,Long expected) throws IOException { quotaLock();
        User user=currentUser();operations.require(user,"FILE_EDIT"); FileItem file=locked(id); require(file,user,"EDITOR",false); sheetGuard.requireWorkbookEdit(file,user); noLive(file); editingAllowed(file); revision(file,expected); if(file.isFolder()) throw fail(400,"Cannot edit a folder");
        try(InputStream in=upload.getInputStream()){append(file,user,in,safeMedia(upload.getContentType()),"Editor save");} newEpoch(file); event(file,user,"EDIT"); return view(file,user,false);
    }
    public ItemView rename(Long id,String name,Long expected) { quotaLock(); User user=currentUser();operations.require(user,"FILE_EDIT"); FileItem file=locked(id); noLive(file); require(file,user,"EDITOR",false); revision(file,expected); file.setName(filename(name)); file.setUpdatedAt(Instant.now()); items.saveAndFlush(file); event(file,user,"RENAME"); return view(file,user,false); }
    public ItemView move(Long id,Long destination,Long expected) { quotaLock();
        User user=currentUser();operations.require(user,"FILE_EDIT"); FileItem file=locked(id); require(file,user,"MANAGER",false); revision(file,expected); FileItem target=parent(destination,user);
        // Moving can change inherited access; only owners/admins may transfer a subtree across permission boundaries.
        if(!admin(user) && !file.getOwner().getId().equals(user.getId())) throw fail(403,"Only the owner can move this file");
        for(FileItem node=target;node!=null;node=node.getParent()) if(node.getId().equals(id)) throw fail(400,"A folder cannot be moved inside itself");
        file.setParent(target); file.setUpdatedAt(Instant.now()); items.saveAndFlush(file);if(quotas.remainingFolder(file)<0)throw fail(413,"Destination folder quota exceeded"); event(file,user,"MOVE"); return view(file,user,false);
    }
    public ItemView copy(Long id,Long destination) throws IOException { quotaLock();
        User user=currentUser();operations.require(user,"FILE_CREATE"); FileItem original=item(id); require(original,user,"VIEWER",false);FileItem target=parent(destination,user);
        Set<Long> ancestors=new HashSet<>();for(FileItem node=target;node!=null;node=node.getParent()){if(!ancestors.add(node.getId())||node.getId().equals(id))throw fail(400,"Choose a destination outside the source folder");}
        // Snapshot the source tree before creating anything, so copying cannot
        // accidentally traverse newly created destination folders.
        List<FileItem> source=new ArrayList<>();Deque<FileItem> pending=new ArrayDeque<>();pending.add(original);Set<Long> seen=new HashSet<>();
        while(!pending.isEmpty()){FileItem node=pending.removeFirst();if(node.getDeletedAt()!=null)continue;if(!seen.add(node.getId()))throw fail(409,"Invalid folder cycle");require(node,user,"VIEWER",false);source.add(node);if(source.size()>1000)throw fail(413,"Copy at most 1,000 items at a time");if(node.isFolder())pending.addAll(items.findByParent_Id(node.getId()));}
        Map<Long,FileItem> copied=new HashMap<>();for(FileItem node:source){FileItem copy=new FileItem();copy.setOwner(user);copy.setParent(node.getId().equals(id)?target:copied.get(node.getParent().getId()));copy.setName(filename(node.getName()));copy.setFolder(node.isFolder());items.saveAndFlush(copy);
            if(!node.isFolder()){requireDownloadAllowed(node,user);sheetGuard.requireWholeWorkbook(node,user);FileVersion version=versions.findByItem_IdAndNumber(node.getId(),node.getCurrentVersion()).orElseThrow(()->fail(404,"Source version not found"));try(InputStream in=Files.newInputStream(store.path(version.getStorageKey()))){append(copy,user,in,version.getMediaType(),"Copied file");}}
            copied.put(node.getId(),copy);event(copy,user,"COPY");
        }return view(copied.get(id),user,false);
    }
    public void trash(Long id,Long expected) { quotaLock();
        User user=currentUser();operations.require(user,"FILE_DELETE"); FileItem file=locked(id); require(file,user,"MANAGER",false); revision(file,expected);
        Deque<FileItem> pending=new ArrayDeque<>(); pending.add(file); Set<Long> seen=new HashSet<>(); Instant deleted=Instant.now();
        while(!pending.isEmpty()) { FileItem node=pending.removeFirst(); if(!seen.add(node.getId())) throw fail(409,"Invalid folder cycle"); if(node.getDeletedAt()!=null) continue; node.setDeletedAt(deleted); node.setDeletedBy(user.getUserName()); node.setDeletionGroup(id); if(node.isFolder()) pending.addAll(items.findByParent_Id(node.getId())); items.save(node); }
        items.flush(); event(file,user,"DELETE");
    }
    public ItemView restore(Long id) { quotaLock();
        User user=currentUser();operations.require(user,"FILE_RESTORE"); FileItem file=locked(id); require(file,user,"MANAGER",true);
        if(file.getDeletionGroup()!=null && !id.equals(file.getDeletionGroup())) throw fail(409,"Restore the containing folder first");
        if(file.getParent()!=null) require(file.getParent(),user,"EDITOR",false);
        List<FileItem> group=items.findByDeletionGroup(id); if(group.isEmpty()) group=List.of(file);
        for(FileItem node:group) {node.setDeletedAt(null);node.setDeletedBy(null);node.setDeletionGroup(null);node.setUpdatedAt(Instant.now());items.save(node);} items.flush(); event(file,user,"RESTORE"); return view(file,user,false);
    }
    public void purge(Long id) { quotaLock();
        User user=currentUser();operations.require(user,"FILE_DELETE"); FileItem file=locked(id); if(!admin(user) && !file.getOwner().getId().equals(user.getId())) throw fail(403,"Only the owner can permanently delete this file");
        if(file.getDeletedAt()==null) throw fail(400,"Move the file to the recycle bin first");
        List<FileItem> subtree=new ArrayList<>();Deque<FileItem> pending=new ArrayDeque<>();Set<Long> seen=new HashSet<>();pending.add(file);
        while(!pending.isEmpty()){FileItem node=pending.removeFirst();if(!seen.add(node.getId()))throw fail(409,"Invalid folder cycle");if(node.getDeletedAt()==null)throw fail(409,"Move all folder contents to the recycle bin before deleting");if(!admin(user)&&!node.getOwner().getId().equals(user.getId()))throw fail(403,"Only owners or administrators can permanently delete these contents");subtree.add(node);if(node.isFolder())pending.addAll(items.findByParent_Id(node.getId()));}
        List<String> keys=new ArrayList<>();Collections.reverse(subtree);
        for(FileItem node:subtree){Long nodeId=node.getId();List<FileVersion> history=versions.findByItem_IdOrderByNumberDesc(nodeId);history.forEach(v->keys.add(v.getStorageKey()));event(node,user,"PERMANENT_DELETE");permissions.deleteByItem_Id(nodeId);stars.deleteByItem_Id(nodeId);comments.deleteByItem_Id(nodeId);guests.deleteByItem_Id(nodeId);collaboration.deleteById(nodeId);rememberedPasswords.deleteById(nodeId);workReturns.deleteByFile_Id(nodeId);versions.deleteAll(history);items.delete(node);items.flush();}
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){keys.forEach(store::remove);}});
    }
    public void star(Long id,boolean value) { User user=currentUser(); FileItem file=locked(id); require(file,user,"VIEWER",false); if(!value) stars.deleteByItem_IdAndUser_Id(id,user.getId()); else if(!stars.existsByItem_IdAndUser_Id(id,user.getId())){ FileStar star=new FileStar(); star.setItem(file); star.setUser(user); stars.save(star); } }
    public record Download(Path path,String name,String mediaType) {}
    public Download download(Long id,Long number) { User user=currentUser();operations.require(user,"FILE_DOWNLOAD"); FileItem file=item(id); require(file,user,"VIEWER",false); if(file.isFolder()) throw fail(400,"Folders cannot be downloaded as files"); requireDownloadAllowed(file,user); sheetGuard.requireWholeWorkbook(file,user); FileVersion version=versions.findByItem_IdAndNumber(id,number==null?file.getCurrentVersion():number).orElseThrow(()->fail(404,"Version not found")); event(file,user,"DOWNLOAD"); return new Download(store.path(version.getStorageKey()),file.getName(),version.getMediaType()); }
    public List<VersionView> history(Long id) { User user=currentUser(); FileItem file=item(id); require(file,user,"VIEWER",false); return versions.findByItem_IdOrderByNumberDesc(id).stream().map(v->new VersionView(v.getNumber(),v.getSizeBytes(),v.getChecksum(),v.getActorLabel()==null?v.getEditor().getName():v.getActorLabel(),v.getCreatedAt(),v.getReason(),v.getMediaType())).toList(); }
    public ItemView restoreVersion(Long id,long number,Long expected) throws IOException { quotaLock(); User user=currentUser();operations.require(user,"FILE_RESTORE"); FileItem file=locked(id); require(file,user,"EDITOR",false); sheetGuard.requireWorkbookEdit(file,user); noLive(file); revision(file,expected); FileVersion version=versions.findByItem_IdAndNumber(id,number).orElseThrow(()->fail(404,"Version not found")); try(InputStream in=Files.newInputStream(store.path(version.getStorageKey()))){append(file,user,in,version.getMediaType(),"Restored from v"+number);} newEpoch(file); event(file,user,"RESTORE_VERSION"); return view(file,user,false); }
    public List<Person> sharingDirectory(String query,int limit){
        checkOperation("FILE_SHARE");
        String term=Objects.toString(query,"").trim().toLowerCase(Locale.ROOT);
        var accounts=users.findByEnabledTrue().stream().filter(u->u.getEmployee()!=null).collect(Collectors.groupingBy(u->u.getEmployee().getId()));
        return employeeDirectory.findAll().stream().flatMap(e->{
            String name=(Objects.toString(e.getFirstName(),"")+" "+Objects.toString(e.getLastName(),"")).trim();
            String department=e.getCategory()==null?"":e.getCategory().getName();
            var linked=Boolean.FALSE.equals(e.getActive())?List.<User>of():accounts.getOrDefault(e.getId(),List.of());
            if(linked.isEmpty())return Stream.of(new Person(null,name,e.getEmail(),e.getEmployeeId(),department));
            return linked.stream().map(u->new Person(u.getId(),name.isBlank()?u.getName():name,e.getEmail()==null?u.getEmail():e.getEmail(),e.getEmployeeId(),department));
        }).filter(p->(p.name()+" "+p.email()+" "+p.employeeId()+" "+p.department()).toLowerCase(Locale.ROOT).contains(term))
          .sorted(Comparator.comparing(p->Objects.toString(p.name(),""),String.CASE_INSENSITIVE_ORDER)).limit(Math.max(1,Math.min(2000,limit))).toList();
    }
    public List<Person> people(String query) { return people(query,30); }
    public List<Person> people(String query,int limit) { checkOperation("FILE_SHARE"); String q=query==null?"":query.toLowerCase(Locale.ROOT); return users.findByEnabledTrue().stream().filter(u->u.getEmployee()!=null&&!Boolean.FALSE.equals(u.getEmployee().getActive())).map(u->{var e=u.getEmployee();String name=Stream.of(e.getFirstName(),e.getLastName()).filter(Objects::nonNull).collect(Collectors.joining(" ")).trim();return new Person(u.getId(),name.isBlank()?u.getName():name,e.getEmail()==null?u.getEmail():e.getEmail(),e.getEmployeeId(),e.getCategory()==null?"":e.getCategory().getName());}).filter(p->(p.name()+" "+p.email()+" "+p.employeeId()+" "+p.department()).toLowerCase(Locale.ROOT).contains(q)).limit(Math.max(1,Math.min(2000,limit))).toList(); }
    public List<ShareView> shares(Long id) { User user=currentUser();if(!operations.allows(user,"FILE_SHARE")&&!operations.allows(user,"FILE_MANAGE_ACCESS"))operations.require(user,"FILE_MANAGE_ACCESS"); FileItem file=item(id); require(file,user,"MANAGER",false); return permissions.findByItem_Id(id).stream().map(p->new ShareView(p.getId(),p.getRecipient().getId(),p.getRecipient().getName(),p.getRecipient().getEmail(),p.getAccess().name(),p.getGrantedAt(),p.getExpiresAt(),!p.isDownloadBlocked())).toList(); }
    public Map<String,Object> share(Long id,Long recipient,FilePermission.Access access,Instant expiry,boolean notify) { return share(id,recipient,access,expiry,notify,null); }
    public Map<String,Object> shareSaved(Long id,Long recipient,Instant expiry,boolean notify,Boolean allowDownload){
        User actor=currentUser();operations.require(actor,"FILE_SHARE");FileItem file=locked(id);require(file,actor,"MANAGER",false);
        User target=users.findById(recipient).orElseThrow(()->fail(404,"Employee account not found"));
        var existing=permissions.findByItem_IdAndRecipient_Id(id,recipient);
        var access=sheetGuard.savedShareAccess(file,target,existing.map(FilePermission::getAccess).orElse(null));
        return share(id,recipient,access,expiry,notify,allowDownload==null?existing.map(p->!p.isDownloadBlocked()).orElse(false):allowDownload);
    }
    public Map<String,Object> share(Long id,Long recipient,FilePermission.Access access,Instant expiry,boolean notify,Boolean allowDownload) {
        User user=currentUser();operations.require(user,"FILE_SHARE"); FileItem file=locked(id); require(file,user,"MANAGER",false); if(!config().isInternalSharing()) throw fail(403,"Internal sharing is disabled");
        User target=users.findById(recipient).orElseThrow(()->fail(404,"Employee account not found"));
        if(target.getEmployee()==null || !Boolean.TRUE.equals(target.getEnabled()) || Boolean.FALSE.equals(target.getEmployee().getActive())) throw fail(400,"Recipient needs an active linked employee account");
        if(expiry!=null && !expiry.isAfter(Instant.now())) throw fail(400,"Expiry must be in the future");
        var existingPermission=permissions.findByItem_IdAndRecipient_Id(id,recipient);if(existingPermission.isPresent())operations.require(user,"FILE_MANAGE_ACCESS");FilePermission permission=existingPermission.orElseGet(FilePermission::new); permission.setItem(file); permission.setRecipient(target); permission.setAccess(access); permission.setExpiresAt(expiry); if(allowDownload!=null)permission.setDownloadBlocked(!allowDownload); if(!downloadAllowed(file,user))permission.setDownloadBlocked(true); permissions.save(permission); event(file,user,"SHARE_"+access.name()+(permission.isDownloadBlocked()?"_NO_DOWNLOAD":""));
        String delivery="not_requested";
        if(notify) delivery=notifications.explicitNotice(target,existingPermission.isPresent()?"File access updated":"File shared with you",user.getName()+" shared "+file.getName()+" with "+access.name()+" access.",id)?"queued":"failed";
        return Map.of("shared",true,"emailDelivery",delivery);
    }
    public void revoke(Long id,Long shareId) { User user=currentUser();operations.require(user,"FILE_MANAGE_ACCESS"); FileItem file=locked(id); require(file,user,"MANAGER",false); requireDownloadAllowed(file,user); FilePermission permission=permissions.findById(shareId).orElseThrow(()->fail(404,"Share not found")); if(!permission.getItem().getId().equals(id)) throw fail(404,"Share not found"); notifications.notice(permission.getRecipient(),"Access revoked","Your access to "+file.getName()+" was revoked by "+user.getName()+".",id);permissions.delete(permission); event(file,user,"REVOKE_ACCESS"); }
    public Map<String,Object> storage() { User user=currentUser(); FileSettings cfg=config(); return Map.of("usedBytes",items.used(user.getId()),"quotaBytes",quotas.userLimit(user.getId(),cfg.getUserQuotaBytes()),"files",items.countByOwner_IdAndFolderAndDeletedAtIsNull(user.getId(),false),"folders",items.countByOwner_IdAndFolderAndDeletedAtIsNull(user.getId(),true),"maxUploadBytes",cfg.getMaxUploadBytes(),"admin",admin(user),"disabledEditors",Objects.toString(cfg.getDisabledEditors(),"")); }
    public FileSettings getSettings() { User user=currentUser(); if(!admin(user)) throw fail(403,"Administrator required"); return config(); }
    public FileSettings updateSettings(FileSettings input) { User user=currentUser(); if(!admin(user)) throw fail(403,"Administrator required"); FileSettings cfg=quotaLock(); if(input.getRevision()!=cfg.getRevision()) throw fail(409,"Settings changed; reload before saving"); if(input.getMaxUploadBytes()<1 || input.getMaxUploadBytes()>524288000L || input.getUserQuotaBytes()<0 || input.getOrganizationQuotaBytes()<0 || (input.getUserQuotaBytes()>0 && input.getUserQuotaBytes()<input.getMaxUploadBytes()) || input.getMaxFilenameLength()<1 || input.getMaxFilenameLength()>180 || input.getBlockedExtensions()==null || input.getBlockedExtensions().length()>1000) throw fail(400,"Invalid storage or upload limits"); cfg.setMaxUploadBytes(input.getMaxUploadBytes()); cfg.setUserQuotaBytes(input.getUserQuotaBytes()); cfg.setOrganizationQuotaBytes(input.getOrganizationQuotaBytes()); cfg.setMaxFilenameLength(input.getMaxFilenameLength()); cfg.setBlockedExtensions(input.getBlockedExtensions()); cfg.setInternalSharing(input.isInternalSharing()); cfg.setExternalSharing(input.isExternalSharing()); cfg.setCollaborationEnabled(input.isCollaborationEnabled());String disabled=Objects.toString(input.getDisabledEditors(),"");Set<String> editors=new TreeSet<>();for(String type:disabled.split(",")){type=type.trim().toLowerCase(Locale.ROOT);if(type.isBlank())continue;if(!Set.of("document","spreadsheet","presentation","pdf","image","text").contains(type))throw fail(400,"Invalid editor policy");editors.add(type);}for(int days:new int[]{input.getRecycleRetentionDays(),input.getVersionRetentionDays(),input.getAuditRetentionDays()})if(days<0||days>3650)throw fail(400,"Retention must be between 0 and 3,650 days");cfg.setDisabledEditors(String.join(",",editors));cfg.setRecycleRetentionDays(input.getRecycleRetentionDays());cfg.setVersionRetentionDays(input.getVersionRetentionDays());cfg.setAuditRetentionDays(input.getAuditRetentionDays());cfg.setEmailNotifications(input.isEmailNotifications());var policyEvent=new FileEvent();policyEvent.setOwnerId(user.getId());policyEvent.setUsername(user.getUserName());policyEvent.setFilename("File settings");policyEvent.setAction("SETTINGS_UPDATE");events.save(policyEvent);return settings.saveAndFlush(cfg); }
    public Page<FileEvent> activity(Long id,int page) { User user=currentUser();operations.require(user,"FILE_VIEW_AUDIT"); if(id!=null) {require(item(id),user,"VIEWER",false);return events.findByItemIdOrderByCreatedAtDesc(id,PageRequest.of(Math.max(0,page),50));} return admin(user)?events.findAll(PageRequest.of(Math.max(0,page),50,Sort.by(Sort.Direction.DESC,"createdAt"))):events.findByOwnerIdOrderByCreatedAtDesc(user.getId(),PageRequest.of(Math.max(0,page),50)); }
    public record CommentView(Long id,Long threadId,String body,String author,Instant createdAt,boolean resolved) {}
    private boolean mentionRecipient(FileItem file,User target){
        if(!Boolean.TRUE.equals(target.getEnabled())||!operations.allows(target,"FILE_VIEW"))return false;
        if(!admin(target)){var authorities=target.getRole()==null?List.<org.springframework.security.core.GrantedAuthority>of():List.<org.springframework.security.core.GrantedAuthority>of(new org.springframework.security.core.authority.SimpleGrantedAuthority(target.getRole().getName()));var auth=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(target.getUserName(),null,authorities);if(pages.getAccessiblePages(auth).stream().noneMatch(p->"/files".equals(p.getPath())))return false;}
        try{return access(file,target,false)!=null;}catch(ResponseStatusException ex){return false;}
    }
    private void mentionNotices(FileItem file,User actor,String body){
        if(!notifications.enabled())return;var match=java.util.regex.Pattern.compile("(?<![A-Za-z0-9._-])@([A-Za-z0-9._-]{1,80})").matcher(body);Set<Long> sent=new HashSet<>();int attempts=0;
        while(match.find()&&attempts++<30&&sent.size()<10){var target=users.findByUserName(match.group(1));if(target.isEmpty()||target.get().getId().equals(actor.getId())||sent.contains(target.get().getId())||!mentionRecipient(file,target.get()))continue;sent.add(target.get().getId());notifications.notice(target.get(),"You were mentioned",actor.getName()+" mentioned you in a comment on "+file.getName()+".",file.getId());}
    }
    private CommentView commentView(FileComment c){return new CommentView(c.getId(),c.getThreadId(),c.getBody(),c.getAuthor().getName(),c.getCreatedAt(),c.isResolved());}
    public List<CommentView> comments(Long id){User user=currentUser();require(item(id),user,"VIEWER",false);return comments.findByItem_IdOrderByCreatedAtAsc(id).stream().map(this::commentView).toList();}
    public CommentView comment(Long id,String body,Long threadId){User user=currentUser();FileItem file=locked(id);require(file,user,"COMMENTER",false);if(body==null||body.isBlank()||body.length()>4000)throw fail(400,"Comments must contain 1–4000 characters");if(threadId!=null){FileComment thread=comments.findById(threadId).orElseThrow(()->fail(404,"Thread not found"));if(!thread.getItem().getId().equals(id)||thread.getThreadId()!=null)throw fail(400,"Invalid comment thread");if(thread.isResolved())throw fail(409,"Reopen the resolved thread before replying");}FileComment comment=new FileComment();comment.setItem(file);comment.setAuthor(user);comment.setBody(body.trim());comment.setThreadId(threadId);comments.saveAndFlush(comment);event(file,user,"COMMENT");mentionNotices(file,user,body);return commentView(comment);}
    public void resolve(Long id,Long commentId,boolean value){User user=currentUser();FileItem file=locked(id);require(file,user,"COMMENTER",false);FileComment comment=comments.findById(commentId).orElseThrow(()->fail(404,"Comment not found"));if(!comment.getItem().getId().equals(id)||comment.getThreadId()!=null)throw fail(404,"Thread not found");if(!comment.getAuthor().getId().equals(user.getId()))require(file,user,"MANAGER",false);comment.setResolved(value);comments.save(comment);event(file,user,value?"RESOLVE_COMMENT":"REOPEN_COMMENT");}
    FileItem accessManagedItem(Long id){User user=currentUser();operations.require(user,"FILE_MANAGE_ACCESS");FileItem f=item(id);require(f,user,"MANAGER",false);return f;}
    FileItem managedItem(Long id){User user=currentUser();operations.require(user,"FILE_SHARE");FileItem file=item(id);require(file,user,"MANAGER",false);return file;}
    boolean externalSharing(){return config().isExternalSharing();}
    void guestAvailable(FileGuestInvitation invitation){operations.require(invitation.getGrantedBy(),"FILE_VIEW");operations.require(invitation.getGrantedBy(),"FILE_SHARE");
        if(!externalSharing() || !Boolean.TRUE.equals(invitation.getGrantedBy().getEnabled())) throw fail(403,"Guest access is unavailable");
        require(invitation.getItem(),invitation.getGrantedBy(),"MANAGER",false);requireDownloadAllowed(invitation.getItem(),invitation.getGrantedBy());
        if(!admin(invitation.getGrantedBy())) {var auth=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(invitation.getGrantedBy().getUserName(),null,List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(invitation.getGrantedBy().getRole().getName())));if(pages.getAccessiblePages(auth).stream().noneMatch(p->"/files".equals(p.getPath())))throw fail(403,"Guest access is unavailable");}
    }
    ItemView guestView(FileGuestInvitation invite){FileItem f=invite.getItem();return new ItemView(f.getId(),f.getName(),false,null,null,"EAC",f.getMediaType(),f.getSizeBytes(),f.getCurrentVersion(),f.getRevision(),f.getCreatedAt(),f.getUpdatedAt(),null,null,invite.getAccess(),false,editorEnabled(f),editorEnabled(f)&&"EDITOR".equals(invite.getAccess())&&operations.allows(invite.getGrantedBy(),"FILE_EDIT"),false,true);}
    Download guestDownload(FileGuestInvitation invite){guestAvailable(invite);operations.require(invite.getGrantedBy(),"FILE_DOWNLOAD");FileItem f=invite.getItem();sheetGuard.requireGuestAllowed(f);FileVersion v=versions.findByItem_IdAndNumber(f.getId(),f.getCurrentVersion()).orElseThrow(()->fail(404,"File version missing"));event(f,invite.getGrantedBy(),"GUEST_DOWNLOAD","Guest: "+invite.getEmail());return new Download(store.path(v.getStorageKey()),f.getName(),v.getMediaType());}
    ItemView guestSave(FileGuestInvitation invite,MultipartFile upload,Long expected) throws IOException { quotaLock();operations.require(invite.getGrantedBy(),"FILE_EDIT");FileItem f=locked(invite.getItem().getId());guestAvailable(invite);sheetGuard.requireGuestAllowed(f);if(!"EDITOR".equals(invite.getAccess()))throw fail(403,"Guest has view-only access");noLive(f);editingAllowed(f);revision(f,expected);try(InputStream in=upload.getInputStream()){append(f,invite.getGrantedBy(),in,safeMedia(upload.getContentType()),"Guest editor save");}FileVersion v=versions.findByItem_IdAndNumber(f.getId(),f.getCurrentVersion()).orElseThrow();v.setActorLabel("Guest: "+invite.getEmail());versions.save(v);newEpoch(f);event(f,invite.getGrantedBy(),"GUEST_EDIT","Guest: "+invite.getEmail());return guestView(invite);}
    void guestEvent(FileGuestInvitation invite,String action){event(invite.getItem(),invite.getGrantedBy(),action,"Guest: "+invite.getEmail());}
    void invitationEvent(FileGuestInvitation invite,String action){event(invite.getItem(),currentUser(),action);}
    ItemView guestUpload(FileGuestInvitation invite,MultipartFile upload)throws IOException{ quotaLock();operations.require(invite.getGrantedBy(),"FILE_UPLOAD");operations.require(invite.getGrantedBy(),"FILE_CREATE");FileItem folder=locked(invite.getItem().getId());guestAvailable(invite);if(!invite.isUploadOnly()||!folder.isFolder())throw fail(403,"This invitation does not allow uploads");if(upload.getSize()>invite.getMaxUploadBytes())throw fail(413,"File exceeds the request size limit");FileItem file=new FileItem();file.setOwner(folder.getOwner());file.setParent(folder);file.setName(filename(upload.getOriginalFilename()));items.saveAndFlush(file);try(InputStream in=upload.getInputStream()){append(file,invite.getGrantedBy(),in,safeMedia(upload.getContentType()),"File request submission");}FileVersion v=versions.findByItem_IdAndNumber(file.getId(),1).orElseThrow();v.setActorLabel("Guest: "+invite.getEmail());versions.save(v);event(file,invite.getGrantedBy(),"REQUEST_UPLOAD","Guest: "+invite.getEmail());notifications.notice(folder.getOwner(),"File request received","A file was submitted to your request folder: "+file.getName()+".",folder.getId());return view(file,invite.getGrantedBy(),false);}
    void checkOperation(String code){operations.require(currentUser(),code);}
    private boolean editorEnabled(FileItem f){if(f.isFolder())return false;String ext=f.getName().substring(f.getName().lastIndexOf('.')+1).toLowerCase(Locale.ROOT);String type=List.of("eacdoc","docx").contains(ext)?"document":List.of("eacsheet","xlsx","csv").contains(ext)?"spreadsheet":List.of("eacslides","pptx").contains(ext)?"presentation":"pdf".equals(ext)?"pdf":f.getMediaType().startsWith("image/")?"image":"text";return !Arrays.asList(Objects.toString(config().getDisabledEditors(),"").split(",")).contains(type);}
    private void editingAllowed(FileItem f){if(!editorEnabled(f))throw fail(403,"This editor is disabled by your administrator. You can still download the original file.");}
    private boolean collaborativeFormat(FileItem f){String name=f.getName().toLowerCase(Locale.ROOT);return !f.isFolder()&&(name.endsWith(".eacdoc")||name.endsWith(".eacsheet")||name.endsWith(".eacslides"));}
    private void bridgeKey(String key){if(collaborationKey.isBlank())throw fail(503,"The collaboration service is not configured");if(key==null||!java.security.MessageDigest.isEqual(key.getBytes(java.nio.charset.StandardCharsets.UTF_8),collaborationKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)))throw fail(403,"Collaboration service authentication required");}
    public Map<String,Object> collaborationAccess(Long id){User user=currentUser();FileItem f=item(id);require(f,user,"VIEWER",false);String role=access(f,user,false);return Map.of("available",config().isCollaborationEnabled()&&!collaborationKey.isBlank()&&collaborativeFormat(f)&&editorEnabled(f),"active",live(f),"epoch",epoch(f),"fileId",id,"userId",user.getId(),"userName",user.getName(),"canEdit",List.of("OWNER","MANAGER","EDITOR").contains(role)&&operations.allows(user,"FILE_EDIT"),"access",role,"version",f.getCurrentVersion());}
    public Map<String,Object> collaborationLease(Long id,String key,String requestedEpoch,String roomId,boolean release){
        bridgeKey(key);User user=currentUser();FileItem f=locked(id);require(f,user,"VIEWER",false);if(!config().isCollaborationEnabled()||!collaborativeFormat(f)||!editorEnabled(f))throw fail(403,"Live editing is unavailable for this file");
        if(!Objects.equals(epoch(f),requestedEpoch)||roomId==null||!roomId.matches("[a-f0-9-]{36}"))throw fail(409,"The saved file changed outside this collaboration session. Reload before reconnecting");
        if(live(f)&&!roomId.equals(f.getCollaborationRoom()))throw fail(409,"Another collaboration service owns this file session");
        if(release){if(roomId.equals(f.getCollaborationRoom())){f.setCollaborationRoom(null);f.setCollaborationLeaseUntil(null);items.saveAndFlush(f);}return Map.of("released",true);}
        operations.require(user,"FILE_EDIT");require(f,user,"EDITOR",false);f.setCollaborationEpoch(requestedEpoch);f.setCollaborationRoom(roomId);f.setCollaborationLeaseUntil(Instant.now().plusSeconds(60));items.saveAndFlush(f);return Map.of("version",f.getCurrentVersion(),"revision",f.getRevision(),"epoch",epoch(f));
    }
    public Map<String,Object> collaborationState(Long id,String key)throws IOException{
        bridgeKey(key);User user=currentUser();FileItem f=item(id);require(f,user,"VIEWER",false);if(!collaborativeFormat(f)||!config().isCollaborationEnabled()||!editorEnabled(f))throw fail(403,"Live editing is unavailable for this file");
        FileVersion version=versions.findByItem_IdAndNumber(id,f.getCurrentVersion()).orElseThrow(()->fail(404,"Current version missing"));if(version.getSizeBytes()>20971520)throw fail(413,"Live editing is limited to 20 MB documents");
        var state=collaboration.findById(id).filter(s->Objects.equals(s.getEpoch(),epoch(f))&&s.getFileVersion()==f.getCurrentVersion());
        return Map.of("nativeJson",Files.readString(store.path(version.getStorageKey())),"yState",state.map(s->Base64.getEncoder().encodeToString(s.getState())).orElse(""),"version",f.getCurrentVersion(),"epoch",epoch(f),"item",view(f,user,false));
    }
    public ItemView collaborationCheckpoint(Long id,String key,String requestedEpoch,String roomId,long expectedVersion,MultipartFile content,MultipartFile state)throws IOException{ quotaLock();
        bridgeKey(key);User user=currentUser();operations.require(user,"FILE_EDIT");FileItem f=locked(id);require(f,user,"EDITOR",false);if(!config().isCollaborationEnabled()||!editorEnabled(f)||!live(f)||!Objects.equals(f.getCollaborationRoom(),roomId)||!Objects.equals(epoch(f),requestedEpoch)||f.getCurrentVersion()!=expectedVersion)throw fail(409,"Collaboration session changed. Reload the saved version before reconnecting");
        if(content.getSize()>20971520||state.getSize()>20971520)throw fail(413,"Collaboration snapshots are limited to 20 MB");
        var json=mapper.readTree(content.getBytes());String format=f.getName().toLowerCase(Locale.ROOT).endsWith(".eacdoc")?"eac-document":f.getName().toLowerCase(Locale.ROOT).endsWith(".eacsheet")?"eac-workbook":"eac-presentation";if(json==null||!format.equals(json.path("format").asText())||json.path("schemaVersion").asInt()!=1)throw fail(400,"Invalid collaborative document structure");
        try(InputStream in=content.getInputStream()){append(f,user,in,f.getMediaType(),"Collaborative checkpoint");}
        FileCollaborationSnapshot snapshot=collaboration.findById(id).orElseGet(FileCollaborationSnapshot::new);snapshot.setItem(f);snapshot.setEpoch(requestedEpoch);snapshot.setFileVersion(f.getCurrentVersion());snapshot.setState(state.getBytes());snapshot.setUpdatedAt(Instant.now());collaboration.saveAndFlush(snapshot);f.setCollaborationLeaseUntil(Instant.now().plusSeconds(60));items.saveAndFlush(f);event(f,user,"COLLABORATIVE_CHECKPOINT");return view(f,user,false);
    }
}
