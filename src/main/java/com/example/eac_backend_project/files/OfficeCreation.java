package com.example.eac_backend_project.files;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service @Transactional(rollbackFor=Exception.class)
public class OfficeCreation {
 private final FileSuiteService files;private final OfficeSettings settings;
 public OfficeCreation(FileSuiteService files,OfficeSettings settings){this.files=files;this.settings=settings;}
 public Object templates(int page){files.currentUser();if(settings.getTemplateFolderId()==null)return java.util.Map.of("configured",false,"content",java.util.List.of(),"last",true);return files.list("files",settings.getTemplateFolderId(),"",page,30);}
 public FileSuiteService.ItemView fromTemplate(Long id,Long parent,String name)throws IOException{var source=files.get(id);if(settings.getTemplateFolderId()==null||!settings.getTemplateFolderId().equals(source.parentId())||source.folder()||!source.name().toLowerCase(java.util.Locale.ROOT).matches(".*\\.(docx|xlsx|pptx)$"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose an Office document from the configured template folder");String extension=source.name().substring(source.name().lastIndexOf('.'));if(name==null||!name.toLowerCase(java.util.Locale.ROOT).endsWith(extension.toLowerCase(java.util.Locale.ROOT)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The new file must retain its template format");var copy=files.copy(id,parent);return files.rename(copy.id(),name,copy.revision());}
 public FileSuiteService.ItemView create(String name,Long parent,String type)throws IOException{if(name==null||!(type.equals("docx")||type.equals("xlsx"))||!name.toLowerCase(java.util.Locale.ROOT).endsWith("."+type))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use the matching .docx or .xlsx filename");var out=new ByteArrayOutputStream();String media;if(type.equals("xlsx")){try(var workbook=new XSSFWorkbook()){workbook.createSheet("Sheet1");workbook.write(out);}media="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";}else{try(var document=new XWPFDocument()){document.createParagraph();document.write(out);}media="application/vnd.openxmlformats-officedocument.wordprocessingml.document";}byte[] bytes=out.toByteArray();MultipartFile part=new MultipartFile(){public String getName(){return "file";}public String getOriginalFilename(){return name;}public String getContentType(){return media;}public boolean isEmpty(){return bytes.length==0;}public long getSize(){return bytes.length;}public byte[] getBytes(){return bytes;}public InputStream getInputStream(){return new ByteArrayInputStream(bytes);}public void transferTo(java.io.File dest)throws IOException{java.nio.file.Files.write(dest.toPath(),bytes);}};return files.upload(part,parent);}
}
