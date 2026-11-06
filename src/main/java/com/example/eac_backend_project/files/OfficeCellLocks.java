package com.example.eac_backend_project.files;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.poifs.crypt.CryptoFunctions;

/** Patches only worksheet protection/styles. Every other ZIP entry retains its exact bytes. */
@Component
public class OfficeCellLocks {
 public record Request(String sessionId,String sheet,String range,boolean locked,boolean initialize,String password,long expectedVersion){}
 private static final String NS="http://schemas.openxmlformats.org/spreadsheetml/2006/main";
 private static Document xml(byte[] bytes)throws Exception{var f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);f.setFeature("http://xml.org/sax/features/external-general-entities",false);f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);return f.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));}
 private static byte[] bytes(Document doc)throws Exception{var f=TransformerFactory.newInstance();f.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD,"");f.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET,"");var out=new ByteArrayOutputStream();f.newTransformer().transform(new DOMSource(doc),new StreamResult(out));return out.toByteArray();}
 private static List<Element> children(Element parent,String tag){List<Element> result=new ArrayList<>();for(Node n=parent.getFirstChild();n!=null;n=n.getNextSibling())if(n instanceof Element e&&tag.equals(e.getLocalName()))result.add(e);return result;}
 private static Element first(Document d,String name){return (Element)d.getElementsByTagNameNS(NS,name).item(0);}
 private static LinkedHashMap<String,byte[]> unzip(byte[] source)throws IOException{var entries=new LinkedHashMap<String,byte[]>();long total=0;try(var in=new ZipInputStream(new ByteArrayInputStream(source))){ZipEntry e;while((e=in.getNextEntry())!=null){byte[] data=in.readNBytes(64*1024*1024+1);total+=data.length;if(data.length>64*1024*1024||total>192*1024*1024||entries.size()>10000||entries.put(e.getName(),data)!=null)throw new IOException("Workbook exceeds cell-protection limits");}}return entries;}
 private static String sheetPath(byte[] source,String name)throws IOException{try(var w=new XSSFWorkbook(new ByteArrayInputStream(source))){var sheet=w.getSheet(name);if(sheet==null)throw new ResponseStatusException(NOT_FOUND,"Worksheet not found");return sheet.getPackagePart().getPartName().getName().substring(1);}}
 public Map<String,Object> status(byte[] source,String name,long version)throws IOException{try(var w=new XSSFWorkbook(new ByteArrayInputStream(source))){var sheet=w.getSheet(name);if(sheet==null)throw new ResponseStatusException(NOT_FOUND,"Worksheet not found");var p=sheet.getCTWorksheet().getSheetProtection();return Map.of("protected",sheet.getProtect(),"hasPassword",p!=null&&(p.isSetPassword()||p.isSetHashValue()),"version",version);}}
 public byte[] apply(byte[] source,Request request)throws IOException{
  if(request.password()==null||request.password().isEmpty()||request.password().length()>128)throw new ResponseStatusException(BAD_REQUEST,"Use a sheet password between 8 and 128 characters");
  if(request.range()==null||!request.range().matches("\\$?[A-Z]{1,3}\\$?[1-9][0-9]*(?::\\$?[A-Z]{1,3}\\$?[1-9][0-9]*)?"))throw new ResponseStatusException(BAD_REQUEST,"Select one rectangular cell range");
  CellRangeAddress area;try{area=CellRangeAddress.valueOf(request.range());area.validate(org.apache.poi.ss.SpreadsheetVersion.EXCEL2007);}catch(Exception e){throw new ResponseStatusException(BAD_REQUEST,"Invalid cell range");}
  if((long)(area.getLastRow()-area.getFirstRow()+1)*(area.getLastColumn()-area.getFirstColumn()+1)>10000)throw new ResponseStatusException(BAD_REQUEST,"Lock at most 10,000 cells at once");
  try(var w=new XSSFWorkbook(new ByteArrayInputStream(source))){var sheet=w.getSheet(request.sheet());if(sheet==null)throw new ResponseStatusException(NOT_FOUND,"Worksheet not found");if(!sheet.getProtect()&&request.password().length()<8)throw new ResponseStatusException(BAD_REQUEST,"Use a new sheet password of at least 8 characters");if(sheet.getProtect()&&!sheet.validateSheetPassword(request.password()))throw new ResponseStatusException(FORBIDDEN,"Incorrect sheet-protection password");}
  try{
   String path=sheetPath(source,request.sheet());var zip=unzip(source);var sheet=xml(zip.get(path));var styles=xml(zip.get("xl/styles.xml"));Element xfs=first(styles,"cellXfs");if(xfs==null)throw new IOException("Workbook styles unavailable");
   List<Element> originals=children(xfs,"xf");Map<String,Integer> mapped=new HashMap<>();
   java.util.function.BiFunction<Integer,Boolean,Integer> style=(index,locked)->{String key=index+":"+locked;return mapped.computeIfAbsent(key,k->{if(index<0||index>=originals.size())throw new ResponseStatusException(BAD_REQUEST,"Invalid cell style");Element xf=(Element)originals.get(index).cloneNode(true);for(Element p:children(xf,"protection"))xf.removeChild(p);Element protection=styles.createElementNS(NS,"protection");protection.setAttribute("locked",locked?"1":"0"); // Preserve hidden formulas from the source style.
    var old=children(originals.get(index),"protection");if(!old.isEmpty()&&old.get(0).hasAttribute("hidden"))protection.setAttribute("hidden",old.get(0).getAttribute("hidden"));xf.insertBefore(protection,children(xf,"extLst").stream().findFirst().orElse(null));xf.setAttribute("applyProtection","1");int id=children(xfs,"xf").size();xfs.appendChild(xf);return id;});};
   Element data=first(sheet,"sheetData");if(data==null)throw new IOException("Worksheet data unavailable");Element existingProtection=first(sheet,"sheetProtection");boolean protectedBefore=existingProtection!=null&&Set.of("1","true").contains(existingProtection.getAttribute("sheet"));
   if(request.initialize()&&!protectedBefore){
    for(Element row:children(data,"row")){if(row.hasAttribute("s"))row.setAttribute("s",style.apply(Integer.parseInt(row.getAttribute("s")),false).toString());for(Element cell:children(row,"c")){int index=cell.hasAttribute("s")?Integer.parseInt(cell.getAttribute("s")):0;cell.setAttribute("s",style.apply(index,false).toString());}}
    Element cols=first(sheet,"cols");if(cols==null){cols=sheet.createElementNS(NS,"cols");sheet.getDocumentElement().insertBefore(cols,data);}List<Element> oldCols=children(cols,"col");List<Element> complete=new ArrayList<>();int next=1;
    oldCols.sort(Comparator.comparingInt(e->Integer.parseInt(e.getAttribute("min"))));
    for(Element col:oldCols){int min=Integer.parseInt(col.getAttribute("min")),max=Integer.parseInt(col.getAttribute("max"));if(min<next)throw new IOException("Overlapping worksheet column styles need review");if(min>next){Element gap=sheet.createElementNS(NS,"col");gap.setAttribute("min",Integer.toString(next));gap.setAttribute("max",Integer.toString(min-1));gap.setAttribute("style",style.apply(0,false).toString());complete.add(gap);}Element copy=(Element)col.cloneNode(true);copy.setAttribute("style",style.apply(col.hasAttribute("style")?Integer.parseInt(col.getAttribute("style")):0,false).toString());complete.add(copy);next=max+1;}
    if(next<=16384){Element gap=sheet.createElementNS(NS,"col");gap.setAttribute("min",Integer.toString(next));gap.setAttribute("max","16384");gap.setAttribute("style",style.apply(0,false).toString());complete.add(gap);}for(Element col:oldCols)cols.removeChild(col);for(Element col:complete)cols.appendChild(col);
   }
   for(int r=area.getFirstRow();r<=area.getLastRow();r++){int rowNumber=r+1;Element row=children(data,"row").stream().filter(e->Integer.parseInt(e.getAttribute("r"))==rowNumber).findFirst().orElse(null);if(row==null){row=sheet.createElementNS(NS,"row");row.setAttribute("r",Integer.toString(rowNumber));Node before=null;for(Element e:children(data,"row"))if(Integer.parseInt(e.getAttribute("r"))>rowNumber){before=e;break;}data.insertBefore(row,before);}
    for(int c=area.getFirstColumn();c<=area.getLastColumn();c++){String address=org.apache.poi.ss.util.CellReference.convertNumToColString(c)+rowNumber;Element cell=children(row,"c").stream().filter(e->address.equals(e.getAttribute("r"))).findFirst().orElse(null);int index=0;if(cell==null){cell=sheet.createElementNS(NS,"c");cell.setAttribute("r",address);Node before=null;for(Element e:children(row,"c"))if(new org.apache.poi.ss.util.CellReference(e.getAttribute("r")).getCol()>c){before=e;break;}row.insertBefore(cell,before);if(row.hasAttribute("s"))index=Integer.parseInt(row.getAttribute("s"));else{Element cols=first(sheet,"cols");if(cols!=null)for(Element col:children(cols,"col"))if(c+1>=Integer.parseInt(col.getAttribute("min"))&&c+1<=Integer.parseInt(col.getAttribute("max"))&&col.hasAttribute("style")){index=Integer.parseInt(col.getAttribute("style"));break;}}}else if(cell.hasAttribute("s"))index=Integer.parseInt(cell.getAttribute("s"));
     // A first-lock initialization may have assigned one of our new unlocked styles.
     if(index>=originals.size()){Element target=children(xfs,"xf").get(index);Element xf=(Element)target.cloneNode(true);String hidden=children(xf,"protection").stream().findFirst().map(e->e.getAttribute("hidden")).orElse("");for(Element p:children(xf,"protection"))xf.removeChild(p);Element p=styles.createElementNS(NS,"protection");p.setAttribute("locked",request.locked()?"1":"0");if(!hidden.isEmpty())p.setAttribute("hidden",hidden);xf.insertBefore(p,children(xf,"extLst").stream().findFirst().orElse(null));index=children(xfs,"xf").size();xfs.appendChild(xf);cell.setAttribute("s",Integer.toString(index));}else cell.setAttribute("s",style.apply(index,request.locked()).toString());
    }
   }
   xfs.setAttribute("count",Integer.toString(children(xfs,"xf").size()));Element protection=existingProtection;
   if(protection==null){protection=sheet.createElementNS(NS,"sheetProtection");Node before=null;boolean pastData=false;for(Node n=sheet.getDocumentElement().getFirstChild();n!=null;n=n.getNextSibling()){if(n==data){pastData=true;continue;}if(pastData&&n instanceof Element e&&!"sheetCalcPr".equals(e.getLocalName())){before=n;break;}}sheet.getDocumentElement().insertBefore(protection,before);}
   protection.setAttribute("sheet","1");if(!protectedBefore||(!protection.hasAttribute("password")&&!protection.hasAttribute("hashValue"))){protection.setAttribute("password",Integer.toHexString(CryptoFunctions.createXorVerifier1(request.password())).toUpperCase(Locale.ROOT));protection.setAttribute("objects","1");protection.setAttribute("scenarios","1");}
   Element dimension=first(sheet,"dimension");if(dimension!=null&&dimension.hasAttribute("ref")){var old=CellRangeAddress.valueOf(dimension.getAttribute("ref"));dimension.setAttribute("ref",new CellRangeAddress(Math.min(old.getFirstRow(),area.getFirstRow()),Math.max(old.getLastRow(),area.getLastRow()),Math.min(old.getFirstColumn(),area.getFirstColumn()),Math.max(old.getLastColumn(),area.getLastColumn())).formatAsString());}
   zip.put(path,bytes(sheet));zip.put("xl/styles.xml",bytes(styles));var out=new ByteArrayOutputStream();try(var z=new ZipOutputStream(out)){for(var entry:zip.entrySet()){z.putNextEntry(new ZipEntry(entry.getKey()));z.write(entry.getValue());z.closeEntry();}}return out.toByteArray();
  }catch(ResponseStatusException e){throw e;}catch(Exception e){throw new IOException("Could not apply Excel cell protection; original retained",e);}
 }
}
