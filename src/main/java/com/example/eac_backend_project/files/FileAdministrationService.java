package com.example.eac_backend_project.files;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
@Service @Transactional(readOnly=true)
public class FileAdministrationService {
 private final FileSuiteService files;private final FileItemRepository items;private final FileVersionRepository versions;private final FileEventRepository events;
 public FileAdministrationService(FileSuiteService files,FileItemRepository items,FileVersionRepository versions,FileEventRepository events){this.files=files;this.items=items;this.versions=versions;this.events=events;}
 private void admin(){if(!FileSuiteService.admin(files.currentUser()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Administrator required");}
 public Map<String,Object> analytics(){
  var user=files.currentUser();boolean admin=FileSuiteService.admin(user);var all=items.findAll().stream().filter(f->admin||f.getOwner().getId().equals(user.getId())).toList();Set<Long> ids=all.stream().map(FileItem::getId).collect(Collectors.toSet());var history=versions.findAll().stream().filter(v->ids.contains(v.getItem().getId())).toList();
  Map<String,Long> types=new TreeMap<>();Map<String,Long> owners=new TreeMap<>();long recycled=0,current=0;
  for(var version:history){types.merge(version.getMediaType(),version.getSizeBytes(),Long::sum);owners.merge(version.getItem().getOwner().getUserName(),version.getSizeBytes(),Long::sum);if(version.getItem().getDeletedAt()!=null)recycled+=version.getSizeBytes();else if(version.getItem().getCurrentVersion()==version.getNumber())current+=version.getSizeBytes();}
  long total=history.stream().mapToLong(FileVersion::getSizeBytes).sum();return Map.of("organization",admin,"totalBytes",total,"currentBytes",current,"recycledBytes",recycled,"historyBytes",total-current-recycled,"versionCount",history.size(),"files",all.stream().filter(f->!f.isFolder()).count(),"byMediaType",types,"byOwner",owners);
 }
 private Specification<FileEvent> filter(String query,String action,String status,Instant from,Instant to){files.checkOperation("FILE_VIEW_AUDIT");
  var user=files.currentUser();String q=query==null?"":query.toLowerCase(Locale.ROOT).substring(0,Math.min(180,query.length()));
  return (root,criteria,builder)->{List<jakarta.persistence.criteria.Predicate> p=new ArrayList<>();if(!FileSuiteService.admin(user))p.add(builder.equal(root.get("ownerId"),user.getId()));if(!q.isBlank())p.add(builder.or(builder.like(builder.lower(root.get("filename")),"%"+q+"%"),builder.like(builder.lower(root.get("username")),"%"+q+"%")));if(action!=null&&!action.isBlank())p.add(builder.equal(root.get("action"),action));if(status!=null&&!status.isBlank())p.add(builder.equal(root.get("status"),status));if(from!=null)p.add(builder.greaterThanOrEqualTo(root.get("createdAt"),from));if(to!=null)p.add(builder.lessThanOrEqualTo(root.get("createdAt"),to));return builder.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));};
 }
 public Page<FileEvent> audit(String q,String action,String status,Instant from,Instant to,int page){return events.findAll(filter(q,action,status,from,to),PageRequest.of(Math.max(0,page),50,Sort.by(Sort.Direction.DESC,"createdAt")));}
 private String cell(Object value){String text=Objects.toString(value,"");int index=0;while(index<text.length()&&(Character.isWhitespace(text.charAt(index))||Character.isISOControl(text.charAt(index))||Character.getType(text.charAt(index))==Character.FORMAT))index++;if(index<text.length()&&"=+-@".indexOf(text.charAt(index))>=0)text="'"+text;char quote=34;return quote+text.replace(String.valueOf(quote),String.valueOf(quote)+quote)+quote;}
 public String export(String q,String action,String status,Instant from,Instant to){var rows=events.findAll(filter(q,action,status,from,to),PageRequest.of(0,10000,Sort.by(Sort.Direction.DESC,"createdAt")));StringBuilder csv=new StringBuilder("Created,User,Action,File,Status,IP\r\n");for(var event:rows)csv.append(String.join(",",cell(event.getCreatedAt()),cell(event.getUsername()),cell(event.getAction()),cell(event.getFilename()),cell(event.getStatus()),cell(event.getIpAddress()))).append("\r\n");return csv.toString();}
 public Page<Map<String,Object>> companyFiles(String query,int page){admin();String q=Objects.toString(query,"").toLowerCase(Locale.ROOT);var list=items.findAll().stream().filter(f->f.getName().toLowerCase(Locale.ROOT).contains(q)).sorted(Comparator.comparing(FileItem::getUpdatedAt).reversed()).map(f->{Map<String,Object> row=new LinkedHashMap<>();row.put("id",f.getId());row.put("name",f.getName());row.put("owner",f.getOwner().getUserName());row.put("folder",f.isFolder());row.put("sizeBytes",f.getSizeBytes());row.put("deletedAt",f.getDeletedAt());return row;}).toList();var paging=PageRequest.of(Math.max(0,page),50);int start=(int)Math.min(list.size(),paging.getOffset());return new PageImpl<>(list.subList(start,Math.min(list.size(),start+50)),paging,list.size());}
}
