package com.example.eac_backend_project.files;

import org.springframework.stereotype.Component;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Component
public class OfficeDownloadClient {
 private final OfficeSettings settings;
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
 public OfficeDownloadClient(OfficeSettings settings){this.settings=settings;}
 boolean requestSave(String key)throws IOException{
  var json=new com.fasterxml.jackson.databind.ObjectMapper();var body=Map.<String,Object>of("c","forcesave","key",key);
  String token=new OfficeTokens(settings,json).sign(Map.of("payload",body,"operation","command"));
  try{var response=http.send(HttpRequest.newBuilder(safeOrigin(settings.getServerUrl()).resolve("/coauthoring/CommandService.ashx")).timeout(Duration.ofSeconds(30)).header("Authorization","Bearer "+token).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
   if(response.statusCode()!=200)throw new ResponseStatusException(BAD_GATEWAY,"The document server did not accept the save request");
   int error=json.readTree(response.body()).path("error").asInt(-1);if(error==4)return false;if(error!=0)throw new ResponseStatusException(BAD_GATEWAY,"The document server could not start saving (code "+error+")");return true;
  }catch(HttpTimeoutException e){throw new ResponseStatusException(GATEWAY_TIMEOUT,"The document server has not confirmed the save yet. Keep the editor open, wait for a saved checkpoint, then retry.");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Save request interrupted",e);}
 }
 static URI safeOrigin(String raw){try{URI u=URI.create(raw);if(!Set.of("http","https").contains(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getFragment()!=null||u.getQuery()!=null||!(u.getPath().isEmpty()||u.getPath().equals("/")))throw new IllegalArgumentException();return u;}catch(Exception e){throw new ResponseStatusException(SERVICE_UNAVAILABLE,"Configure an absolute ONLYOFFICE service origin");}}
 private static boolean sameOrigin(URI a,URI b){int ap=a.getPort()<0?("https".equals(a.getScheme())?443:80):a.getPort(),bp=b.getPort()<0?("https".equals(b.getScheme())?443:80):b.getPort();return a.getScheme().equals(b.getScheme())&&a.getHost().equalsIgnoreCase(b.getHost())&&ap==bp;}
 URI trustedDownload(String raw){try{URI allowed=safeOrigin(settings.getServerUrl()),browser=safeOrigin(settings.getBrowserUrl()),url=URI.create(raw);if(url.getHost()==null||(!sameOrigin(allowed,url)&&!sameOrigin(browser,url))||url.getUserInfo()!=null||url.getFragment()!=null||!url.getPath().startsWith("/cache/")||url.getRawPath().contains("%")||url.getPath().contains(".."))throw new IllegalArgumentException();return URI.create(allowed.toString().replaceAll("/$","")+url.getRawPath()+(url.getRawQuery()==null?"":"?"+url.getRawQuery()));}catch(Exception e){throw new ResponseStatusException(BAD_REQUEST,"Save URL is outside the configured document server cache");}}
 byte[] fetch(String raw)throws IOException{try{var response=http.send(HttpRequest.newBuilder(trustedDownload(raw)).timeout(Duration.ofSeconds(30)).GET().build(),HttpResponse.BodyHandlers.ofInputStream());try(InputStream in=response.body()){if(response.statusCode()!=200)throw new ResponseStatusException(BAD_GATEWAY,"Document Server did not return the saved document");long limit=Math.min(200*1024*1024L,settings.getMaxSaveBytes());if(limit<=0)throw new ResponseStatusException(SERVICE_UNAVAILABLE,"Invalid ONLYOFFICE save limit");var out=new ByteArrayOutputStream();byte[] buffer=new byte[65536];int count;long size=0;while((count=in.read(buffer))!=-1){size+=count;if(size>limit)throw new ResponseStatusException(PAYLOAD_TOO_LARGE,"Edited document exceeds the save limit");out.write(buffer,0,count);}return out.toByteArray();}}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Document Server request interrupted");}}
 static void validatePackage(byte[] bytes,String extension)throws IOException{Set<String> entries=new HashSet<>();long expanded=0;int count=0;try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry entry;byte[] block=new byte[65536];while((entry=zip.getNextEntry())!=null){if(++count>20000||entry.getName().contains("..")||entry.getName().startsWith("/")||!entries.add(entry.getName()))throw new IOException("Invalid Office package");int n;while((n=zip.read(block))!=-1){expanded+=n;if(expanded>512*1024*1024L)throw new IOException("Office package exceeds validation limits");}}}String main=switch(extension){case "docx"->"word/document.xml";case "xlsx"->"xl/workbook.xml";case "pptx"->"ppt/presentation.xml";default->throw new IOException("Unsupported editable format");};if(!entries.contains("[Content_Types].xml")||!entries.contains(main)||entries.stream().anyMatch(n->n.toLowerCase(Locale.ROOT).endsWith("vbaproject.bin")))throw new IOException("Saved output is not a supported macro-free Office document");}
}
