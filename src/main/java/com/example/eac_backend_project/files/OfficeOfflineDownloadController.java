package com.example.eac_backend_project.files;

import java.io.*;
import java.nio.file.*;
import java.security.GeneralSecurityException;
import org.apache.poi.poifs.crypt.*;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class OfficeOfflineDownloadController {
 private final FileSuiteService files;
 public OfficeOfflineDownloadController(FileSuiteService files){this.files=files;}
 public record Request(String password){}
 @PostMapping("/api/files/{id}/download/password")
 public ResponseEntity<byte[]> download(@PathVariable Long id,@RequestBody Request request)throws IOException,GeneralSecurityException{
  // Reuse the exact current-version download ACL, including managed-sheet export guards.
  var source=files.download(id,null);
  if(!source.name().toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password downloads are available for XLSX workbooks");
  if(Files.size(source.path())>64L*1024*1024)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Password downloads currently support workbooks up to 64 MB");
  byte[] encrypted=encrypt(Files.readAllBytes(source.path()),request.password());
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(source.name(),java.nio.charset.StandardCharsets.UTF_8).build().toString()).contentType(MediaType.APPLICATION_OCTET_STREAM).body(encrypted);
 }
 static byte[] encrypt(byte[] original,String password)throws IOException,GeneralSecurityException{
  if(password==null||password.length()<8||password.length()>128)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use an offline password between 8 and 128 characters");
  // Wrap the saved OOXML bytes in standard Office Agile encryption; do not reconstruct the workbook.
  if(original.length<4||original[0]!='P'||original[1]!='K')throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This workbook is already encrypted or is not an XLSX package");
  try(var fs=new POIFSFileSystem();var result=new ByteArrayOutputStream()){
   var info=new EncryptionInfo(EncryptionMode.agile);var encryptor=info.getEncryptor();encryptor.confirmPassword(password);
   // POI's cipher stream may reuse its caller's buffer for a partial block.
   // Transfer through a separate buffer so the supplied original remains unchanged.
   try(var stream=encryptor.getDataStream(fs)){stream.write(original.clone());}
   fs.writeFilesystem(result);return result.toByteArray();
  }
 }
}
