package com.example.eac_backend_project.controller.medical;
import com.example.eac_backend_project.Service.medical.MedicalVisitService;
import com.example.eac_backend_project.dto.medical.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;
@RestController @RequestMapping("/api/public/hospital-visits/{token}") @RequiredArgsConstructor
public class PublicMedicalVisitController {
 private final MedicalVisitService service;
 @GetMapping("/employee") public Map<String,Object> employee(@PathVariable String token,@RequestParam String tagNumber){return service.lookup(token,tagNumber);}
 @PostMapping public MedicalVisitResponse submit(@PathVariable String token,@Valid @RequestBody MedicalVisitRequest r){return service.submit(token,r);}
 @PostMapping("/{id}/attachment") public MedicalVisitResponse attachment(@PathVariable String token,@PathVariable Long id,@RequestPart("file") MultipartFile file) throws IOException{return service.attach(token,id,file);}
}
