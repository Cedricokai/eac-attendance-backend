package com.example.eac_backend_project.transport;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import static com.example.eac_backend_project.transport.TransportAccess.fail;

public final class TransportFileValidation {
 private TransportFileValidation(){}
 public record Validated(String name,String mediaType,byte[] bytes){}
 public static Validated validate(MultipartFile part,boolean imageOnly,boolean allowOffice)throws IOException{
  if(part==null||part.isEmpty()||part.getSize()>10L*1024*1024)throw fail(413,"Choose a non-empty file up to 10 MB");
  String name=part.getOriginalFilename();if(name==null||name.isBlank()||name.length()>180||name.indexOf('/')>=0||name.indexOf((char)92)>=0||name.chars().anyMatch(Character::isISOControl))throw fail(400,"Choose a safe filename without directory paths");String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT):"";byte[] bytes=part.getBytes();if(bytes.length>10*1024*1024)throw fail(413,"File exceeds 10 MB");String mime=null;
  if(Set.of("jpg","jpeg").contains(ext)&&bytes.length>3&&(bytes[0]&255)==255&&(bytes[1]&255)==216&&(bytes[2]&255)==255)mime="image/jpeg";
  else if("png".equals(ext)&&bytes.length>=8&&Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))mime="image/png";
  else if("webp".equals(ext)&&bytes.length>=16&&new String(bytes,0,4,StandardCharsets.US_ASCII).equals("RIFF")&&new String(bytes,8,4,StandardCharsets.US_ASCII).equals("WEBP"))mime="image/webp";
  else if(!imageOnly&&"pdf".equals(ext)&&bytes.length>=5&&new String(bytes,0,5,StandardCharsets.US_ASCII).equals("%PDF-"))mime="application/pdf";
  else if(!imageOnly&&allowOffice&&Set.of("docx","xlsx","pptx").contains(ext)&&bytes.length>=4&&bytes[0]=='P'&&bytes[1]=='K'){
   String expected="docx".equals(ext)?"word/document.xml":"xlsx".equals(ext)?"xl/workbook.xml":"ppt/presentation.xml";boolean contentTypes=false,document=false;long total=0;int count=0;try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))){java.util.zip.ZipEntry entry;byte[] buffer=new byte[8192];while((entry=zip.getNextEntry())!=null){if(++count>10000||entry.getName().contains("..")||entry.getName().startsWith("/")||entry.getName().toLowerCase(Locale.ROOT).contains("vbaproject"))throw fail(400,"Office attachment contains unsafe or excessive content");contentTypes|="[Content_Types].xml".equals(entry.getName());document|=expected.equals(entry.getName());int n;while((n=zip.read(buffer))!=-1){total+=n;if(total>50L*1024*1024)throw fail(413,"Office attachment expands beyond the safe processing limit");}}}catch(java.util.zip.ZipException e){throw fail(400,"Office attachment is not a valid document");}if(contentTypes&&document)mime="docx".equals(ext)?"application/vnd.openxmlformats-officedocument.wordprocessingml.document":"xlsx".equals(ext)?"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":"application/vnd.openxmlformats-officedocument.presentationml.presentation";
  }else if(!imageOnly&&allowOffice&&Set.of("txt","csv").contains(ext)&&new String(bytes,StandardCharsets.UTF_8).indexOf('\0')<0)mime="csv".equals(ext)?"text/csv":"text/plain";
  if(mime==null)throw fail(415,imageOnly?"Choose a valid JPG, PNG or WebP image":"Choose a valid PDF, JPG, PNG or WebP file"+(allowOffice?", plain text or Office document":""));
  if(Set.of("image/jpeg","image/png").contains(mime)){try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw fail(400,"Image cannot be decoded");var reader=readers.next();try{reader.setInput(input);long width=reader.getWidth(0),height=reader.getHeight(0);if(width<=0||height<=0||width*height>40000000L)throw fail(413,"Image dimensions exceed 40 million pixels");if(reader.read(0)==null)throw fail(400,"Image cannot be decoded");}finally{reader.dispose();}}}
  return new Validated(name,mime,bytes);
 }
 public static MultipartFile part(Validated data){return new MultipartFile(){public String getName(){return "file";}public String getOriginalFilename(){return data.name();}public String getContentType(){return data.mediaType();}public boolean isEmpty(){return data.bytes().length==0;}public long getSize(){return data.bytes().length;}public byte[] getBytes(){return data.bytes().clone();}public InputStream getInputStream(){return new ByteArrayInputStream(data.bytes());}public void transferTo(File file)throws IOException{java.nio.file.Files.write(file.toPath(),data.bytes());}};}
}
