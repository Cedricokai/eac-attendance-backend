package com.example.eac_backend_project.files;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/files/admin/quotas")
public class FileQuotaController {
 private final FileQuotaService quotas;private final FileSuiteService files;
 public FileQuotaController(FileQuotaService quotas,FileSuiteService files){this.quotas=quotas;this.files=files;}
 public record Request(String type,Long targetId,long limitBytes){}
 @GetMapping public Object list(){return quotas.list(files.currentUser());}
 @GetMapping("/targets")public Object targets(@RequestParam String type,@RequestParam(defaultValue="")String q){return quotas.targets(files.currentUser(),type,q);}
 @PutMapping public void set(@RequestBody Request request){quotas.set(files.currentUser(),request.type(),request.targetId(),request.limitBytes());}
 @DeleteMapping("/{id}")public void remove(@PathVariable Long id){quotas.remove(files.currentUser(),id);}
}
