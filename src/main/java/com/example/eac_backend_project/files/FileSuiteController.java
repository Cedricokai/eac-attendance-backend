package com.example.eac_backend_project.files;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.OptimisticLockingFailureException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@RestController @RequestMapping("/api/files")
public class FileSuiteController {
    private final FileSuiteService files;
    private final FileGuestService guests;
    public FileSuiteController(FileSuiteService files,FileGuestService guests) { this.files=files; this.guests=guests; }
    public record InvitationRequest(String name,String email,String access,Instant expiresAt,String message,boolean notifyEmail){}
    public record FileRequest(Long folderId,String title,String description,String name,String email,Instant expiresAt,long maxUploadBytes,boolean notifyEmail){}
    public record CollaborationLease(String epoch,String roomId,boolean release){}
    @GetMapping("/{id}/collaboration/access") public Object collaborationAccess(@PathVariable Long id){return files.collaborationAccess(id);}
    @GetMapping("/{id}/collaboration/state") public Object collaborationState(@PathVariable Long id,@RequestHeader("X-EAC-Collaboration-Key")String key)throws IOException{return files.collaborationState(id,key);}
    @PostMapping("/{id}/collaboration/lease") public Object collaborationLease(@PathVariable Long id,@RequestHeader("X-EAC-Collaboration-Key")String key,@RequestBody CollaborationLease request){return files.collaborationLease(id,key,request.epoch(),request.roomId(),request.release());}
    @PostMapping(value="/{id}/collaboration/checkpoint",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Object collaborationCheckpoint(@PathVariable Long id,@RequestHeader("X-EAC-Collaboration-Key")String key,@RequestParam String epoch,@RequestParam String roomId,@RequestParam long version,@RequestParam MultipartFile file,@RequestParam MultipartFile state)throws IOException{return files.collaborationCheckpoint(id,key,epoch,roomId,version,file,state);}
    @GetMapping("/requests") public Object requests(){return guests.requests();}
    @PostMapping("/requests") public Object request(@RequestBody FileRequest r){if(r.folderId()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a destination folder");return guests.createRequest(r.folderId(),r.title(),r.description(),r.name(),r.email(),r.expiresAt(),r.maxUploadBytes(),r.notifyEmail());}
    @DeleteMapping("/requests/{id}") public void closeRequest(@PathVariable Long id){guests.closeRequest(id);}
    @GetMapping("/{id}/guests") public Object guests(@PathVariable Long id){return guests.list(id);}
    @PostMapping("/{id}/guests") public Object invite(@PathVariable Long id,@RequestBody InvitationRequest request){return guests.create(id,request.name(),request.email(),request.access(),request.expiresAt(),request.message(),request.notifyEmail());}
    @DeleteMapping("/{id}/guests/{invitationId}") public void revokeGuest(@PathVariable Long id,@PathVariable Long invitationId){guests.revoke(id,invitationId);}
    public record FolderRequest(String name,Long parentId) {}
    public record RenameRequest(String name,Long revision) {}
    public record MoveRequest(Long parentId,Long revision) {}
    public record RevisionRequest(Long revision) {}
    public record ShareRequest(Long userId,FilePermission.Access access,Instant expiresAt,boolean notifyEmail,Boolean allowDownload,Boolean useSavedAccess) {}
    public record CommentRequest(String body,Long threadId) {}
    @GetMapping("/{id}/comments") public Object comments(@PathVariable Long id){return files.comments(id);}
    @PostMapping("/{id}/comments") public Object comment(@PathVariable Long id,@RequestBody CommentRequest request){return files.comment(id,request.body(),request.threadId());}
    @PutMapping("/{id}/comments/{commentId}") public void resolve(@PathVariable Long id,@PathVariable Long commentId,@RequestParam boolean resolved){files.resolve(id,commentId,resolved);}
    @GetMapping public Object list(@RequestParam(defaultValue="files") String scope,@RequestParam(required=false) Long parentId,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size) { return files.list(scope,parentId,q,page,size); }
    @GetMapping("/storage") public Object storage() { return files.storage(); }
    @GetMapping("/people") public Object people(@RequestParam(defaultValue="") String q) { return files.people(q); }
    @GetMapping("/activity") public Object activity(@RequestParam(required=false) Long itemId,@RequestParam(defaultValue="0") int page) { return files.activity(itemId,page); }
    @GetMapping("/settings") public Object settings() { return files.getSettings(); }
    @PutMapping("/settings") public Object settings(@RequestBody FileSettings settings) { return files.updateSettings(settings); }
    @GetMapping("/breadcrumbs") public Object breadcrumbs(@RequestParam(required=false) Long parentId) { return files.breadcrumbs(parentId); }
    @GetMapping("/{id}") public Object get(@PathVariable Long id) { return files.get(id); }
    @PostMapping("/folders") public Object folder(@RequestBody FolderRequest request) { return files.createFolder(request.name(),request.parentId()); }
    @PostMapping(value="/upload",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Object upload(@RequestParam MultipartFile file,@RequestParam(required=false) Long parentId) throws IOException { return files.upload(file,parentId); }
    @PostMapping(value="/{id}/content",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Object save(@PathVariable Long id,@RequestParam MultipartFile file,@RequestParam Long revision) throws IOException { return files.save(id,file,revision); }
    @PatchMapping("/{id}") public Object rename(@PathVariable Long id,@RequestBody RenameRequest request) { return files.rename(id,request.name(),request.revision()); }
    @PostMapping("/{id}/move") public Object move(@PathVariable Long id,@RequestBody MoveRequest request) { return files.move(id,request.parentId(),request.revision()); }
    @PostMapping("/{id}/copy") public Object copy(@PathVariable Long id,@RequestBody MoveRequest request) throws IOException { return files.copy(id,request.parentId()); }
    @DeleteMapping("/{id}") public void delete(@PathVariable Long id,@RequestParam Long revision) { files.trash(id,revision); }
    @PostMapping("/{id}/restore") public Object restore(@PathVariable Long id) { return files.restore(id); }
    @DeleteMapping("/{id}/permanent") public void permanent(@PathVariable Long id) { files.purge(id); }
    @PutMapping("/{id}/star") public void star(@PathVariable Long id,@RequestParam boolean value) { files.star(id,value); }
    @GetMapping("/{id}/versions") public Object versions(@PathVariable Long id) { return files.history(id); }
    @PostMapping("/{id}/versions/{number}/restore") public Object restoreVersion(@PathVariable Long id,@PathVariable long number,@RequestBody RevisionRequest request) throws IOException { return files.restoreVersion(id,number,request.revision()); }
    @GetMapping("/{id}/download") public ResponseEntity<FileSystemResource> download(@PathVariable Long id,@RequestParam(required=false) Long version) {
        var content=files.download(id,version);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(content.name(),StandardCharsets.UTF_8).build().toString())
            .header("X-Content-Type-Options","nosniff").header(HttpHeaders.CACHE_CONTROL,"no-store")
            .contentType(MediaType.parseMediaType(content.mediaType())).body(new FileSystemResource(content.path()));
    }
    @GetMapping("/{id}/shares") public Object shares(@PathVariable Long id) { return files.shares(id); }
    @PostMapping("/{id}/share") public Object share(@PathVariable Long id,@RequestBody ShareRequest request) { if(request.userId()==null || (!Boolean.TRUE.equals(request.useSavedAccess()) && request.access()==null)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Recipient and permission are required"); return Boolean.TRUE.equals(request.useSavedAccess())?files.shareSaved(id,request.userId(),request.expiresAt(),request.notifyEmail(),request.allowDownload()):files.share(id,request.userId(),request.access(),request.expiresAt(),request.notifyEmail(),request.allowDownload()); }
    @DeleteMapping("/{id}/shares/{shareId}") public void revoke(@PathVariable Long id,@PathVariable Long shareId) { files.revoke(id,shareId); }
}

@RestControllerAdvice(assignableTypes={FileSuiteController.class,FileWorkReviewController.class,FileGuestController.class,FileBackupController.class,FileAdministrationController.class,FileQuotaController.class,FileRetentionController.class,FilePolicyController.class,EmployeeFileSharingController.class})
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
class FileSuiteErrors {
    private final FileFailureAudit audit;
    FileSuiteErrors(FileFailureAudit audit){this.audit=audit;}
    private void rejected(int status){try{audit.record(status);}catch(RuntimeException ex){org.slf4j.LoggerFactory.getLogger(getClass()).warn("File rejection audit could not be recorded");}}
    @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException ex) { rejected(ex.getStatusCode().value());return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message",Objects.requireNonNullElse(ex.getReason(),"File operation failed"))); }
    @ExceptionHandler(OptimisticLockingFailureException.class) ResponseEntity<?> conflict() { rejected(409);return ResponseEntity.status(409).body(Map.of("message","This file changed in another session. Reload before saving.")); }
    @ExceptionHandler(IOException.class) ResponseEntity<?> storage() { rejected(503);return ResponseEntity.status(503).body(Map.of("message","File storage is unavailable. Retry or contact your administrator.")); }
    @ExceptionHandler({org.springframework.dao.DataAccessException.class,org.springframework.transaction.TransactionException.class})ResponseEntity<?> database(){rejected(503);return ResponseEntity.status(503).body(Map.of("message","File metadata is temporarily unavailable. Retry or contact your administrator."));}
}
