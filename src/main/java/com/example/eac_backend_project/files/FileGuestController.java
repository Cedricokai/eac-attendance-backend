package com.example.eac_backend_project.files;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
@RestController @RequestMapping("/api/file-guests")
public class FileGuestController {
    private final FileGuestService guests;
    public FileGuestController(FileGuestService guests){this.guests=guests;}
    public record VerificationCode(String code){}
    @GetMapping("/info") public Object info(@RequestHeader("X-EAC-Guest-Invite")String token){return guests.info(token);}
    @PostMapping("/code") public void code(@RequestHeader("X-EAC-Guest-Invite")String token){guests.requestCode(token);}
    @PostMapping("/verify") public ResponseEntity<?> verify(@RequestHeader("X-EAC-Guest-Invite")String token,@RequestBody VerificationCode code){var result=guests.verify(token,code.code());return ResponseEntity.status(result.verified()?200:400).body(result);}
    @GetMapping("/state") public Object state(@RequestHeader("X-EAC-Guest-Invite")String token,@RequestHeader("X-EAC-Guest-Session")String session){return guests.stateResponse(token,session);}
    @PostMapping(value="/upload",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Object upload(@RequestHeader("X-EAC-Guest-Invite")String token,@RequestHeader("X-EAC-Guest-Session")String session,@RequestParam MultipartFile file)throws IOException{return guests.upload(token,session,file);}
    @GetMapping("/content") public ResponseEntity<FileSystemResource> content(@RequestHeader("X-EAC-Guest-Invite")String token,@RequestHeader("X-EAC-Guest-Session")String session){var file=guests.download(token,session);return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.name(),StandardCharsets.UTF_8).build().toString()).header("X-Content-Type-Options","nosniff").header(HttpHeaders.CACHE_CONTROL,"no-store").contentType(MediaType.parseMediaType(file.mediaType())).body(new FileSystemResource(file.path()));}
    @PostMapping(value="/save",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Object save(@RequestHeader("X-EAC-Guest-Invite")String token,@RequestHeader("X-EAC-Guest-Session")String session,@RequestParam MultipartFile file,@RequestParam Long revision)throws IOException{return guests.save(token,session,file,revision);}
}
