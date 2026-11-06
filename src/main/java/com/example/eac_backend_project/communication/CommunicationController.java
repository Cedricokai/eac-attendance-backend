package com.example.eac_backend_project.communication;
import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import org.springframework.web.multipart.MultipartFile;import java.util.*;import java.io.*;import java.nio.charset.StandardCharsets;
@RestController @RequestMapping("/api/communication") public class CommunicationController {
 private final CommunicationService service;private final CommunicationBackupService backups;
 public CommunicationController(CommunicationService service,CommunicationBackupService backups){this.service=service;this.backups=backups;}
 public record EmployeeLink(Long employeeId){}
 @GetMapping("/account/employee-link")public Object linkCandidate(){return service.linkCandidate();}
 @PostMapping("/account/employee-link")public void linkEmployee(@RequestBody EmployeeLink input){service.linkSelf(input.employeeId());}
 @GetMapping("/me")public Object me(){return service.me();}
 @GetMapping("/notifications")public ResponseEntity<?> notifications(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.notifications());}
 @GetMapping("/directory")public Object directory(@RequestParam(defaultValue="")String q){return service.directory(q);}
 @GetMapping("/conversations")public Object list(@RequestParam(defaultValue="all")String filter){return service.list(filter);}
 public record Direct(Long userId){}public record Text(String text){}public record Reason(String reason){}public record Receipt(long sequence,boolean read){}public record Action(String action,String emoji){}
 @PostMapping("/conversations/direct")public Object direct(@RequestBody Direct input){return service.direct(input.userId());}
 @PostMapping("/groups")public Object group(@RequestBody CommunicationService.GroupInput input){return service.group(input);}
 @GetMapping("/conversations/{id}")public Object get(@PathVariable Long id){return service.get(id);}
 @PatchMapping("/conversations/{id}/preferences")public Object preference(@PathVariable Long id,@RequestBody Map<String,Boolean> input){return service.preferences(id,input);}
 @PatchMapping("/groups/{id}")public Object manage(@PathVariable Long id,@RequestBody Map<String,Object> input){return service.manageGroup(id,input);}
 @PostMapping("/groups/{id}/leave")public void leave(@PathVariable Long id){service.leave(id);}
 @GetMapping("/conversations/{id}/messages")public Object history(@PathVariable Long id,@RequestParam(defaultValue="0")long before,@RequestParam(defaultValue="")String q){return service.history(id,before,q);}
 @PostMapping("/conversations/{id}/messages")public Object send(@PathVariable Long id,@RequestBody CommunicationService.MessageInput input)throws IOException{return service.send(id,input);}
 @PostMapping("/conversations/{id}/receipts")public void receipt(@PathVariable Long id,@RequestBody Receipt input){service.receipt(id,input.sequence(),input.read());}
 @PostMapping("/conversations/{id}/typing")public void typing(@PathVariable Long id){service.typing(id);}
 @PostMapping(value="/conversations/{id}/attachments",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)public Object upload(@PathVariable Long id,@RequestParam MultipartFile file,@RequestParam(defaultValue="false")boolean voice,@RequestParam(defaultValue="0")int seconds)throws IOException{return service.upload(id,file,voice,seconds);}
 @PatchMapping("/messages/{id}")public Object edit(@PathVariable Long id,@RequestBody Text input){return service.edit(id,input.text());}
 @PostMapping("/messages/{id}/actions")public void action(@PathVariable Long id,@RequestBody Action input)throws IOException{service.action(id,input.action(),input.emoji());}
 @DeleteMapping("/messages/{id}")public void remove(@PathVariable Long id,@RequestBody Reason input){service.deleteMessage(id,input.reason());}
 @DeleteMapping("/conversations/{id}")public void removeConversation(@PathVariable Long id,@RequestBody Reason input){service.deleteConversation(id,input.reason());}
 @GetMapping("/messages/{id}/attachments/{fileId}")public ResponseEntity<byte[]> attachment(@PathVariable Long id,@PathVariable Long fileId,@RequestHeader(value="X-Communication-Review-Reason",required=false)String reason)throws IOException{var f=service.attachment(id,fileId,reason);return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.mime())).header(HttpHeaders.CACHE_CONTROL,"no-store").header("X-Content-Type-Options","nosniff").header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(f.name(),StandardCharsets.UTF_8).build().toString()).body(f.bytes());}
 @GetMapping("/conversations/{id}/search")public Object search(@PathVariable Long id,@RequestParam String q,@RequestParam(defaultValue="0")long before){return service.search(id,q,before);}
 @GetMapping("/admin/group-directory")public Object adminDirectory(@RequestHeader("X-Communication-Review-Reason")String reason){return service.adminDirectory(reason);}
 @GetMapping("/admin/groups")public Object adminGroups(@RequestHeader("X-Communication-Review-Reason")String reason){return service.adminGroups(reason);}
 @PatchMapping("/admin/groups/{id}")public void adminGroup(@PathVariable Long id,@RequestHeader("X-Communication-Review-Reason")String reason,@RequestBody Map<String,Object> input){service.adminGroup(id,input,reason);}
 @GetMapping("/admin/conversations")public Object oversight(@RequestHeader("X-Communication-Review-Reason")String reason,@RequestParam(defaultValue="0")int page){return service.oversight(reason,page);}
 @GetMapping("/admin/conversations/{id}/messages")public Object review(@PathVariable Long id,@RequestHeader("X-Communication-Review-Reason")String reason,@RequestParam(defaultValue="0")long before){return service.review(id,reason,before);}
 @GetMapping("/admin/conversations/{id}/export")public Object export(@PathVariable Long id,@RequestHeader("X-Communication-Review-Reason")String reason,@RequestParam(defaultValue="0")long before){return service.export(id,reason,before);}
 @GetMapping("/admin/policy")public Object policy(@RequestHeader("X-Communication-Review-Reason")String reason){var u=service.access().account();service.authorizeReview(u,"COMMUNICATION_POLICY_MANAGE",reason,null,"POLICY_READ");return service.config();}
 @PutMapping("/admin/policy")public Object policy(@RequestBody CommunicationPolicy input,@RequestHeader("X-Communication-Review-Reason")String reason)throws IOException{return service.updatePolicy(input,reason);}
 @GetMapping("/admin/audit")public Object audit(@RequestHeader("X-Communication-Review-Reason")String reason,@RequestParam(defaultValue="0")int page){var u=service.access().account();service.authorizeReview(u,"COMMUNICATION_ADMIN",reason,null,"AUDIT_READ");return service.audit().list(page);}
 @GetMapping("/admin/recovery")public Object recovery(@RequestHeader("X-Communication-Review-Reason")String reason,@RequestParam(defaultValue="0")int page){return service.recovery(reason,page);}
 @PostMapping("/admin/recovery/{kind}/{id}")public void restore(@PathVariable String kind,@PathVariable Long id,@RequestBody Reason input){if(!Set.of("message","conversation").contains(kind))throw CommunicationAccess.fail(400,"Invalid recovery kind");service.restore(id,kind.equals("conversation"),input.reason());}
 @GetMapping("/admin/backups")public Object backupList(@RequestHeader("X-Communication-Review-Reason")String reason){return backups.list(reason);}
 @PostMapping("/admin/backups")public Object backupCreate(@RequestBody Reason input)throws IOException{return backups.create(input.reason());}
 @PostMapping("/admin/backups/{id}/verify")public Object backupVerify(@PathVariable String id,@RequestBody Reason input)throws IOException{return backups.verify(id,input.reason());}
 @PostMapping("/admin/backups/{id}/restore-preview")public Object backupPreview(@PathVariable String id,@RequestBody Reason input)throws IOException{return backups.preview(id,input.reason());}
 @PostMapping("/admin/backups/{id}/restore")public Object backupRestore(@PathVariable String id,@RequestBody Map<String,String> input)throws IOException{return backups.restore(id,input.get("reason"),input.get("confirmation"));}
}
@RestControllerAdvice(assignableTypes={CommunicationController.class}) @org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE) class CommunicationErrors {
 @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)ResponseEntity<?> status(org.springframework.web.server.ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",Objects.requireNonNullElse(e.getReason(),"Communication request failed")));}
 @ExceptionHandler({IOException.class,org.springframework.dao.DataAccessException.class,org.springframework.transaction.TransactionException.class})ResponseEntity<?> unavailable(){return ResponseEntity.status(503).body(Map.of("message","Communication storage is temporarily unavailable. Please retry."));}
 @ExceptionHandler({IllegalArgumentException.class,org.springframework.http.converter.HttpMessageNotReadableException.class})ResponseEntity<?> invalid(){return ResponseEntity.badRequest().body(Map.of("message","Invalid communication request"));}
}
