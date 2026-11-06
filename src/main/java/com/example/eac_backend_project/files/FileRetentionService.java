package com.example.eac_backend_project.files;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import java.time.*;
import java.util.*;
@Service @Transactional(rollbackFor=Exception.class)
public class FileRetentionService {
 private final FileSettingsRepository settings;private final FileItemRepository items;private final FileVersionRepository versions;private final FilePermissionRepository permissions;private final FileStarRepository stars;private final FileCommentRepository comments;private final FileGuestRepository guests;private final FileCollaborationRepository snapshots;private final FileEventRepository events;private final FileStorageQuotaRepository quotas;private final FileContentStore store;
 public FileRetentionService(FileSettingsRepository settings,FileItemRepository items,FileVersionRepository versions,FilePermissionRepository permissions,FileStarRepository stars,FileCommentRepository comments,FileGuestRepository guests,FileCollaborationRepository snapshots,FileEventRepository events,FileStorageQuotaRepository quotas,FileContentStore store){this.settings=settings;this.items=items;this.versions=versions;this.permissions=permissions;this.stars=stars;this.comments=comments;this.guests=guests;this.snapshots=snapshots;this.events=events;this.quotas=quotas;this.store=store;}
 private Instant cutoff(int days){return Instant.now().minusSeconds(days*86400L);}
 private int depth(FileItem file){Set<Long> seen=new HashSet<>();int depth=0;for(var parent=file.getParent();parent!=null;parent=parent.getParent()){if(!seen.add(parent.getId()))throw new IllegalStateException("Invalid folder hierarchy");depth++;}return depth;}
 private void audit(FileItem file,String action){var event=new FileEvent();event.setItemId(file.getId());event.setOwnerId(file.getOwner().getId());event.setFilename(file.getName());event.setUsername("system-retention");event.setAction(action);events.save(event);}
 public Map<String,Long> run(){
  var cfg=settings.lock().orElse(null);if(cfg==null)return Map.of("files",0L,"versions",0L,"events",0L);
  long purgedFiles=0,purgedVersions=0,purgedEvents=0;Set<String> keys=new HashSet<>();
  if(cfg.getRecycleRetentionDays()>0){var expired=items.findAll().stream().filter(f->f.getDeletedAt()!=null&&f.getDeletedAt().isBefore(cutoff(cfg.getRecycleRetentionDays()))).sorted(Comparator.comparingInt(this::depth).reversed()).toList();for(var file:expired){if(file.isFolder()&&!items.findByParent_Id(file.getId()).isEmpty())continue;var history=versions.findByItem_IdOrderByNumberDesc(file.getId());history.forEach(v->keys.add(v.getStorageKey()));purgedVersions+=history.size();permissions.deleteByItem_Id(file.getId());stars.deleteByItem_Id(file.getId());comments.deleteByItem_Id(file.getId());guests.deleteByItem_Id(file.getId());snapshots.deleteById(file.getId());quotas.findByTargetTypeAndTargetId("FOLDER",file.getId()).ifPresent(quotas::delete);versions.deleteAll(history);audit(file,"RETENTION_PURGE");items.delete(file);items.flush();purgedFiles++;}}
  if(cfg.getVersionRetentionDays()>0){var expired=versions.findAll().stream().filter(v->v.getNumber()!=1&&v.getNumber()!=v.getItem().getCurrentVersion()&&v.getCreatedAt().isBefore(cutoff(cfg.getVersionRetentionDays()))).toList();for(var version:expired){keys.add(version.getStorageKey());audit(version.getItem(),"VERSION_RETENTION_PURGE");versions.delete(version);purgedVersions++;}}
  if(cfg.getAuditRetentionDays()>0){var expired=events.findAll().stream().filter(e->e.getCreatedAt().isBefore(cutoff(cfg.getAuditRetentionDays()))).toList();purgedEvents=expired.size();events.deleteAll(expired);}
  if(!keys.isEmpty())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCommit(){for(var key:keys)try{store.remove(key);}catch(RuntimeException ex){org.slf4j.LoggerFactory.getLogger(FileRetentionService.class).error("Retention content cleanup failed; administrator storage cleanup required");}}});
  return Map.of("files",purgedFiles,"versions",purgedVersions,"events",purgedEvents);
 }
}
