package com.example.eac_backend_project.files;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/files/admin/retention")
public class FileRetentionController {
 private final FileSuiteService files;private final FileRetentionService retention;private final com.example.eac_backend_project.Service.ActivityLogService activity;
 public FileRetentionController(FileSuiteService files,FileRetentionService retention,com.example.eac_backend_project.Service.ActivityLogService activity){this.files=files;this.retention=retention;this.activity=activity;}
 @PostMapping("/run")public Object run(){var user=files.currentUser();if(!FileSuiteService.admin(user))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Administrator required");var result=retention.run();activity.logSuccess("Manual file retention: "+result,"FILE_SUITE",user.getUserName(),1L);return result;}
}
