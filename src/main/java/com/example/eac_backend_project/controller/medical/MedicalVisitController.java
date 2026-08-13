package com.example.eac_backend_project.controller.medical;
import com.example.eac_backend_project.Service.medical.MedicalVisitService;
import com.example.eac_backend_project.dto.medical.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/medical-visits") @RequiredArgsConstructor
public class MedicalVisitController {
 private final MedicalVisitService service;
 @GetMapping public List<MedicalVisitResponse> list(@RequestParam(required=false) String status,@RequestParam(required=false) String search){return service.list(status,search);}
 @GetMapping("/me") public List<MedicalVisitResponse> mine(Authentication auth){return service.mine(auth);}
 @GetMapping("/{id}") public MedicalVisitResponse get(@PathVariable Long id){return service.get(id);}
 @PutMapping("/{id}/verify") public MedicalVisitResponse verify(@PathVariable Long id,@RequestBody(required=false) MedicalVisitDecisionRequest r,Authentication auth){return service.decide(id,true,r,auth);}
 @PutMapping("/{id}/reject") public MedicalVisitResponse reject(@PathVariable Long id,@RequestBody MedicalVisitDecisionRequest r,Authentication auth){return service.decide(id,false,r,auth);}
 @GetMapping("/{id}/attachment") public ResponseEntity<Resource> attachment(@PathVariable Long id){var v=service.attachmentInfo(id);String name=v.getAttachmentName()==null?"attachment":v.getAttachmentName().replaceAll("[\\r\\n\\\"]","");return ResponseEntity.ok().contentType(MediaType.parseMediaType(v.getAttachmentContentType()==null?"application/octet-stream":v.getAttachmentContentType())).header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=\""+name+"\"").body(service.attachment(id));}
}
