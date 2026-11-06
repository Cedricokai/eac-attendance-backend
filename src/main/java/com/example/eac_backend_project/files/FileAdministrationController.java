package com.example.eac_backend_project.files;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.time.Instant;
@RestController @RequestMapping("/api/files/admin")
public class FileAdministrationController {
 private final FileAdministrationService service;
 public FileAdministrationController(FileAdministrationService service){this.service=service;}
 @GetMapping("/analytics")public Object analytics(){return service.analytics();}
 @GetMapping("/company-files")public Object company(@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="0")int page){return service.companyFiles(q,page);}
 @GetMapping("/audit")public Object audit(@RequestParam(defaultValue="")String q,@RequestParam(required=false)String action,@RequestParam(required=false)String status,@RequestParam(required=false)Instant from,@RequestParam(required=false)Instant to,@RequestParam(defaultValue="0")int page){return service.audit(q,action,status,from,to,page);}
 @GetMapping("/audit/export")public ResponseEntity<String> export(@RequestParam(defaultValue="")String q,@RequestParam(required=false)String action,@RequestParam(required=false)String status,@RequestParam(required=false)Instant from,@RequestParam(required=false)Instant to){return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"eac-file-audit.csv\"").header(HttpHeaders.CACHE_CONTROL,"no-store").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(service.export(q,action,status,from,to));}
}
