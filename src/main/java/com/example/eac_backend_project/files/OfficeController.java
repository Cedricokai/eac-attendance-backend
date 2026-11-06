package com.example.eac_backend_project.files;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.core.io.FileSystemResource;
import java.util.*;
import java.io.IOException;

@RestController
public class OfficeController {
 private final OfficeService office;private final OfficeTokens tokens;private final OfficeSettings settings;private final OfficeCreation creation;private final OfficeSheetAccessService sheets;
 public OfficeController(OfficeService office,OfficeTokens tokens,OfficeSettings settings,OfficeCreation creation,OfficeSheetAccessService sheets){this.office=office;this.tokens=tokens;this.settings=settings;this.creation=creation;this.sheets=sheets;}
 @PostMapping("/api/files/{id}/onlyoffice/cell-locks/status") public Object cellLockStatus(@PathVariable Long id,@RequestBody Map<String,String> body)throws IOException{return office.cellLockStatus(id,body.get("sheet"));}
 @PostMapping("/api/files/{id}/onlyoffice/cell-locks") public Object cellLocks(@PathVariable Long id,@RequestBody OfficeCellLocks.Request request)throws IOException{var saved=office.save(request.sessionId());if(Boolean.TRUE.equals(saved.get("requested")))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT,"Workbook changes are still saving. Wait for synchronization, then retry; no cell edit is required.");return office.cellLocks(id,request);}
 public record PasswordRequest(String password){}
 @PostMapping("/api/files/{id}/onlyoffice/password") public Object password(@PathVariable Long id,@RequestBody PasswordRequest request)throws IOException{return office.verifyPassword(id,request.password());}
 public record CreateRequest(String name,Long parentId,String type){}
 public record TemplateRequest(Long templateId,String name,Long parentId){}
 @GetMapping("/api/files/onlyoffice/templates") public Object templates(@RequestParam(defaultValue="0")int page){return creation.templates(page);}
 @PostMapping("/api/files/onlyoffice/from-template") public Object fromTemplate(@RequestBody TemplateRequest request)throws IOException{return creation.fromTemplate(request.templateId(),request.parentId(),request.name());}
 @PostMapping("/api/files/onlyoffice/create") public Object create(@RequestBody CreateRequest request)throws IOException{return creation.create(request.name(),request.parentId(),Objects.toString(request.type(),""));}
 @GetMapping("/api/files/onlyoffice/status") public Object status(){return office.availability();}
 @PostMapping("/api/files/{id}/onlyoffice/config") public Object config(@PathVariable Long id,@RequestHeader(value="Origin",required=false)String origin)throws IOException{return office.config(id,origin);}
 @GetMapping("/api/files/{id}/onlyoffice/sheets") public Object sheets(@PathVariable Long id)throws IOException{return sheets.state(id);}
 @PutMapping("/api/files/{id}/onlyoffice/sheets") public Object assignSheets(@PathVariable Long id,@RequestBody OfficeSheetAccessService.Update request)throws IOException{return sheets.update(id,request);}
 @PutMapping("/api/files/{id}/onlyoffice/sheets/batch") public Object assignSheetBatch(@PathVariable Long id,@RequestBody OfficeSheetAccessService.BatchUpdate request)throws IOException{return sheets.updateBatch(id,request);}
 @GetMapping("/api/files/{id}/onlyoffice/users") public Object users(@PathVariable Long id){return office.candidates(id);}
 @PostMapping("/api/files/onlyoffice/sessions/{id}/heartbeat") public Object heartbeat(@PathVariable String id){return office.heartbeat(id);}
 @PostMapping("/api/files/onlyoffice/sessions/{id}/save") public Object save(@PathVariable String id)throws IOException{return office.save(id);}
 @GetMapping("/api/onlyoffice/source") public ResponseEntity<FileSystemResource> source(@RequestParam String token,@RequestHeader(value="Authorization",required=false)String authorization)throws IOException{tokens.sourceHeader(authorization,settings.getConnectorUrl().replaceAll("/$","")+"/api/onlyoffice/source?token="+token);var data=office.source(token);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(data.mediaType())).header("X-Content-Type-Options","nosniff").body(new FileSystemResource(data.path()));}
 @PostMapping("/api/onlyoffice/callback/{id}") public Map<String,Integer> callback(@PathVariable String id,@RequestHeader(value="Authorization",required=false)String authorization,@RequestBody JsonNode body)throws IOException{office.callback(id,tokens.callback(authorization,body));return Map.of("error",0);}
}
