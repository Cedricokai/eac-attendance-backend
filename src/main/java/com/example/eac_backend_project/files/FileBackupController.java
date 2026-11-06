package com.example.eac_backend_project.files;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.nio.file.*;
import java.io.IOException;
@RestController @RequestMapping("/api/files/backups")
public class FileBackupController {
 private final FileBackupService backups;private final FileNotificationService notifications;
 public FileBackupController(FileBackupService backups,FileNotificationService notifications){this.backups=backups;this.notifications=notifications;}
 public record Create(String destination){}
 @GetMapping public Object list(){return backups.list();}
 @GetMapping("/policy") public Object policy(){return backups.policy();}
 @PutMapping("/policy") public Object policy(@RequestBody FileBackupPolicy policy){return backups.policy(policy);}
 @PostMapping public Object create(@RequestBody Create request)throws IOException{try{return backups.create(request.destination());}catch(IOException|RuntimeException ex){if(ex instanceof org.springframework.web.server.ResponseStatusException rejected&&rejected.getStatusCode().is4xxClientError())throw ex;try{notifications.backupFailed();}catch(RuntimeException ignored){}throw ex;}}
 @PostMapping("/{id}/verify") public Object verify(@PathVariable String id)throws IOException{return backups.verify(id);}
 @PostMapping("/{id}/restore") public Object restore(@PathVariable String id)throws IOException{return backups.restore(id);}
 @GetMapping("/{id}/download") public void download(@PathVariable String id,jakarta.servlet.http.HttpServletResponse response)throws IOException{
  Path temp=backups.download(id);
  try{response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);response.setContentLengthLong(Files.size(temp));response.setHeader(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"eac-files-"+id+".zip\"");response.setHeader(HttpHeaders.CACHE_CONTROL,"no-store");try(var in=Files.newInputStream(temp)){in.transferTo(response.getOutputStream());}}finally{Files.deleteIfExists(temp);}
 }
}
