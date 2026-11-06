package com.example.eac_backend_project.files;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.*;
import javax.xml.stream.*;

/** Reads metadata only. Never rewrites, reconstructs or exports a workbook. */
final class OfficeWorkbookInspection {
 record Rule(String name,boolean restricted,Map<String,String> members){}
 record Sheet(String name,String relation){}
 private static XMLInputFactory factory(){var f=XMLInputFactory.newFactory();f.setProperty(XMLInputFactory.SUPPORT_DTD,false);f.setProperty("javax.xml.stream.isSupportingExternalEntities",false);return f;}
 private static String attr(XMLStreamReader r,String name){for(int i=0;i<r.getAttributeCount();i++)if(name.equals(r.getAttributeLocalName(i)))return r.getAttributeValue(i);return "";}
 static List<Rule> read(Path path)throws IOException{
  try(var zip=new ZipFile(path.toFile())){
   Map<String,byte[]> metadata=new HashMap<>();Map<String,Rule> rules=new HashMap<>();
   var entries=zip.entries();while(entries.hasMoreElements()){var entry=entries.nextElement();if(entry.getName().equals("xl/workbook.xml")||entry.getName().equals("xl/_rels/workbook.xml.rels")){try(var in=zip.getInputStream(entry)){metadata.put(entry.getName(),limited(in));}}else if(entry.getName().matches("xl/worksheets/[^/]+\\.xml")){try(var in=zip.getInputStream(entry)){rules.put(entry.getName(),rule(in));}}}
   return combine(metadata,rules);
  }
 }
 static List<Rule> read(byte[] bytes)throws IOException{
  Map<String,byte[]> metadata=new HashMap<>();Map<String,Rule> rules=new HashMap<>();
  try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry entry;while((entry=zip.getNextEntry())!=null){String name=entry.getName();if(name.equals("xl/workbook.xml")||name.equals("xl/_rels/workbook.xml.rels"))metadata.put(name,limited(zip));else if(name.matches("xl/worksheets/[^/]+\\.xml"))rules.put(name,rule(new FilterInputStream(zip){@Override public void close(){}}));}}
  return combine(metadata,rules);
 }
 private static byte[] limited(InputStream in)throws IOException{byte[] data=in.readNBytes(2*1024*1024+1);if(data.length>2*1024*1024)throw new IOException("Workbook metadata is too large");return data;}
 private static Rule rule(InputStream in)throws IOException{
  Map<String,String> members=new LinkedHashMap<>();boolean selected=false,restricted=false;
  try{var r=factory().createXMLStreamReader(in);while(r.hasNext()){int type=r.next();if(type==XMLStreamConstants.START_ELEMENT){if(r.getLocalName().equals("userProtectedRange")){selected="EAC_SHEET_ACCESS".equals(attr(r,"name"));if(selected){restricted="notView".equalsIgnoreCase(attr(r,"type"));String area=attr(r,"sqref").replace("$","");if(restricted&&!Set.of("1:1048576","A1:XFD1048576","A:XFD").contains(area))throw new IOException("EAC sheet rule no longer covers the entire sheet");}}else if(selected&&r.getLocalName().equals("user")){String access=switch(attr(r,"type")){case "edit"->"CanEdit";case "view"->"CanView";default->"NotView";};members.put(attr(r,"id"),access);}}else if(type==XMLStreamConstants.END_ELEMENT&&r.getLocalName().equals("userProtectedRange"))selected=false;}r.close();return new Rule("",restricted,members);}catch(XMLStreamException e){throw new IOException("Invalid workbook XML",e);}
 }
 private static List<Rule> combine(Map<String,byte[]> metadata,Map<String,Rule> rules)throws IOException{
  if(!metadata.containsKey("xl/workbook.xml")||!metadata.containsKey("xl/_rels/workbook.xml.rels"))throw new IOException("Save this as a valid XLSX workbook before setting sheet access");
  try{Map<String,String> paths=new HashMap<>();var r=factory().createXMLStreamReader(new ByteArrayInputStream(metadata.get("xl/_rels/workbook.xml.rels")));while(r.hasNext()){if(r.next()==XMLStreamConstants.START_ELEMENT&&r.getLocalName().equals("Relationship")){String target=attr(r,"Target");String path=target.startsWith("/")?target.substring(1):"xl/"+target;paths.put(attr(r,"Id"),path);}}r.close();
   List<Rule> out=new ArrayList<>();r=factory().createXMLStreamReader(new ByteArrayInputStream(metadata.get("xl/workbook.xml")));while(r.hasNext()){if(r.next()==XMLStreamConstants.START_ELEMENT&&r.getLocalName().equals("sheet")){String name=attr(r,"name");var policy=rules.getOrDefault(paths.get(attr(r,"id")),new Rule("",false,Map.of()));out.add(new Rule(name,policy.restricted(),policy.members()));if(out.size()>1000)throw new IOException("Workbook has more than 1000 sheets");}}r.close();if(out.isEmpty())throw new IOException("Workbook has no worksheets");return out;
  }catch(XMLStreamException e){throw new IOException("Invalid workbook metadata",e);}
 }
}
