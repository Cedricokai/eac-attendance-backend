package com.example.eac_backend_project.files;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.zip.*;

/** Versioned, checksum-verified snapshots. Restore always makes new private copies. */
@Service @Transactional(isolation=Isolation.READ_COMMITTED,rollbackFor=Exception.class)
public class FileBackupService {
 private final FileSuiteService files;private final FileItemRepository items;private final FileVersionRepository versions;
 private final FileBackupRepository backups;private final FileBackupChunkRepository chunks;private final FileBackupPolicyRepository policies;
 private final FileSettingsRepository settings;private final FileContentStore store;private final ObjectMapper json;private final Path root;
 public FileBackupService(FileSuiteService files,FileItemRepository items,FileVersionRepository versions,FileBackupRepository backups,FileBackupChunkRepository chunks,FileBackupPolicyRepository policies,FileSettingsRepository settings,FileContentStore store,ObjectMapper json,@Value("${app.files.backup-root:./backups/file-suite}")String root){this.files=files;this.items=items;this.versions=versions;this.backups=backups;this.chunks=chunks;this.policies=policies;this.settings=settings;this.store=store;this.json=json;this.root=Path.of(root).toAbsolutePath().normalize();}
 public record Item(Long id,Long parentId,Long ownerId,String owner,String name,boolean folder,String mediaType,long currentVersion,Instant createdAt,Instant updatedAt,Instant deletedAt){}
 public record Version(Long itemId,long number,String key,String checksum,long size,String mediaType,String editor,Instant createdAt,String reason){}
 public record Manifest(String format,int schemaVersion,Instant createdAt,List<Item> items,List<Version> versions){}
 private void admin(){if(!FileSuiteService.admin(files.currentUser()))throw fail(403,"Administrator required");}
 private static ResponseStatusException fail(int code,String message){return new ResponseStatusException(HttpStatus.valueOf(code),message);}
 private FileBackupPolicy config(){return policies.findById(1L).orElseGet(()->policies.saveAndFlush(new FileBackupPolicy()));}
 private FileBackupPolicy lock(){config();return policies.lock().orElseThrow();}
 private Path path(String id){if(id==null||!id.matches("[a-f0-9-]{36}"))throw fail(400,"Invalid backup ID");return root.resolve(id+".zip");}
 public List<FileBackup> list(){admin();return backups.findAllByOrderByCreatedAtDesc();}
 public FileBackupPolicy policy(){admin();return config();}
 public FileBackupPolicy policy(FileBackupPolicy input){admin();var cfg=lock();if(input.getRevision()!=cfg.getRevision())throw fail(409,"Backup settings changed. Reload.");if(!List.of("DAILY","WEEKLY","MONTHLY").contains(input.getFrequency())||!List.of("FILESYSTEM","DATABASE").contains(input.getDestination())||input.getRetainCount()<1||input.getRetainCount()>365||input.getMaxBackupBytes()<1048576||input.getMaxBackupBytes()>10737418240L)throw fail(400,"Invalid backup settings");cfg.setEnabled(input.isEnabled());cfg.setFrequency(input.getFrequency());cfg.setDestination(input.getDestination());cfg.setRetainCount(input.getRetainCount());cfg.setMaxBackupBytes(input.getMaxBackupBytes());cfg.setNextRun(input.isEnabled()?Instant.now().plusSeconds(60):null);return policies.saveAndFlush(cfg);}
 public FileBackup create(String destination)throws IOException{admin();return createInternal(destination,files.currentUser().getUserName());}
 public FileBackup createScheduled()throws IOException{var cfg=lock();if(!cfg.isEnabled()||cfg.getNextRun()==null||cfg.getNextRun().isAfter(Instant.now()))return null;var result=createInternal(cfg.getDestination(),"scheduled-backup");prune(cfg.getRetainCount());return result;}
 private void prune(int retain){var all=backups.findAllByOrderByCreatedAtDesc().stream().filter(b->"scheduled-backup".equals(b.getCreatedBy())&&"COMPLETE".equals(b.getStatus())).toList();for(var old:all.subList(Math.min(retain,all.size()),all.size())){chunks.deleteByBackup_Id(old.getId());backups.delete(old);if("FILESYSTEM".equals(old.getDestination()))TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCommit(){try{Files.deleteIfExists(path(old.getId()));}catch(IOException ex){System.err.println("Old backup cleanup failed: "+old.getId());}}});}}
 private FileBackup createInternal(String destination,String actor)throws IOException{
  if(!List.of("FILESYSTEM","DATABASE").contains(destination))throw fail(400,"Choose filesystem or database storage");
  var cfg=lock();settings.lock().orElseThrow(); // same global lock used by version writes/purge
  var allItems=items.findAll();var allVersions=versions.findAll();if(allItems.size()>50000||allVersions.size()>250000)throw fail(413,"This backup exceeds the synchronous export limit. Use an administrator database/storage snapshot.");
  long total=0;for(var v:allVersions){total=Math.addExact(total,v.getSizeBytes());if(total>cfg.getMaxBackupBytes())throw fail(413,"Content exceeds the configured backup limit");}
  var manifest=new Manifest("eac-file-suite-backup",1,Instant.now(),allItems.stream().map(f->new Item(f.getId(),f.getParent()==null?null:f.getParent().getId(),f.getOwner().getId(),f.getOwner().getUserName(),f.getName(),f.isFolder(),f.getMediaType(),f.getCurrentVersion(),f.getCreatedAt(),f.getUpdatedAt(),f.getDeletedAt())).toList(),allVersions.stream().map(v->new Version(v.getItem().getId(),v.getNumber(),v.getStorageKey(),v.getChecksum(),v.getSizeBytes(),v.getMediaType(),v.getEditor().getUserName(),v.getCreatedAt(),v.getReason())).toList());
  Files.createDirectories(root);var backup=new FileBackup();backup.setId(UUID.randomUUID().toString());backup.setDestination(destination);backup.setCreatedBy(actor);backup.setItemCount(allItems.size());backup.setVersionCount(allVersions.size());backups.saveAndFlush(backup);
  Path target=path(backup.getId()),temp=root.resolve(backup.getId()+".partial");
  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCompletion(int status){if(status!=STATUS_COMMITTED)try{Files.deleteIfExists(temp);Files.deleteIfExists(target);}catch(IOException ignored){}}});
  try{
   try(var out=new ZipOutputStream(Files.newOutputStream(temp,StandardOpenOption.CREATE_NEW))){
    byte[] metadata=json.writeValueAsBytes(manifest);if(metadata.length>10485760)throw new IOException("Backup manifest exceeds 10 MB");out.putNextEntry(new ZipEntry("manifest.json"));out.write(metadata);out.closeEntry();
    Set<String> seen=new HashSet<>();for(var version:manifest.versions()){if(!seen.add(version.key()))continue;out.putNextEntry(new ZipEntry("content/"+version.key()));try(var in=Files.newInputStream(store.path(version.key()))){var result=copy(in,out,cfg.getMaxBackupBytes());if(result.size()!=version.size()||!result.checksum().equals(version.checksum()))throw new IOException("Source checksum mismatch");}out.closeEntry();}
   }
   if(Files.size(temp)>cfg.getMaxBackupBytes()+10485760L)throw fail(413,"Backup archive exceeds limit");
   Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);
   backup.setSizeBytes(Files.size(target));try(var in=Files.newInputStream(target)){backup.setChecksum(copy(in,OutputStream.nullOutputStream(),cfg.getMaxBackupBytes()+10485760L).checksum());}
   if(destination.equals("DATABASE")){if(backup.getSizeBytes()>268435456L)throw fail(413,"Database backups are limited to 256 MB. Use filesystem storage for larger backups.");try(var in=Files.newInputStream(target)){int number=0;byte[] data;while((data=in.readNBytes(1048576)).length>0){var chunk=new FileBackupChunk();chunk.setBackup(backup);chunk.setNumber(number++);chunk.setData(data);chunks.save(chunk);}}TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCommit(){try{Files.deleteIfExists(target);}catch(IOException ignored){}}});}
   backup.setStatus("COMPLETE");backup.setVerifiedAt(Instant.now());backup.setMessage("Includes file tree and all immutable versions. Recovery creates private copies; account permissions and guest sessions are not replayed.");backups.saveAndFlush(backup);cfg.setLastRun(Instant.now());cfg.setLastStatus("SUCCESS");cfg.setNextRun(next(cfg));policies.saveAndFlush(cfg);return backup;
  }catch(IOException|RuntimeException ex){Files.deleteIfExists(temp);throw ex;}
 }
 private Instant next(FileBackupPolicy cfg){return cfg.isEnabled()?java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).plusDays(cfg.getFrequency().equals("DAILY")?1:cfg.getFrequency().equals("WEEKLY")?7:30).toInstant():null;}
 static class NonClosingOutputStream extends FilterOutputStream{NonClosingOutputStream(OutputStream out){super(out);}public void close()throws IOException{flush();}}
 record Digest(long size,String checksum){}
 private Digest copy(InputStream in,OutputStream out,long max)throws IOException{try{var hash=MessageDigest.getInstance("SHA-256");long size=0;byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1){size+=n;if(size>max)throw new IOException("Backup data exceeds limit");hash.update(buffer,0,n);out.write(buffer,0,n);}return new Digest(size,HexFormat.of().formatHex(hash.digest()));}catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}}
 private FileBackup backup(String id){return backups.findById(id).filter(b->"COMPLETE".equals(b.getStatus())).orElseThrow(()->fail(404,"Completed backup not found"));}
 private InputStream source(FileBackup backup)throws IOException{
  if("FILESYSTEM".equals(backup.getDestination()))return Files.newInputStream(path(backup.getId()));
  var parts=chunks.findByBackup_IdOrderByNumberAsc(backup.getId());return new SequenceInputStream(Collections.enumeration(parts.stream().map(c->(InputStream)new ByteArrayInputStream(c.getData())).toList()));
 }
 private Path verifiedArchive(FileBackup backup)throws IOException{
  Files.createDirectories(root);Path temp=Files.createTempFile(root,"verify-",".zip");try(var in=source(backup);var out=Files.newOutputStream(temp)){var actual=copy(in,out,Math.max(backup.getSizeBytes(),1));if(actual.size()!=backup.getSizeBytes()||!actual.checksum().equals(backup.getChecksum()))throw new IOException("Backup archive checksum mismatch");}catch(IOException|RuntimeException ex){Files.deleteIfExists(temp);throw ex;}return temp;
 }
 private Manifest verifyContents(Path path)throws IOException{
  try(var archive=new ZipFile(path.toFile())){var manifestEntry=archive.getEntry("manifest.json");if(manifestEntry==null||manifestEntry.getSize()>10485760)throw new IOException("Invalid backup manifest");Manifest manifest;try(var in=archive.getInputStream(manifestEntry)){byte[] bytes=in.readNBytes(10485761);if(bytes.length>10485760)throw new IOException("Manifest too large");manifest=json.readValue(bytes,Manifest.class);}if(!"eac-file-suite-backup".equals(manifest.format())||manifest.schemaVersion()!=1||manifest.items()==null||manifest.versions()==null)throw new IOException("Unsupported backup format");for(var v:manifest.versions()){var entry=archive.getEntry("content/"+v.key());if(entry==null)throw new IOException("Missing backup content");try(var in=archive.getInputStream(entry)){var actual=copy(in,OutputStream.nullOutputStream(),v.size());if(actual.size()!=v.size()||!actual.checksum().equals(v.checksum()))throw new IOException("Content checksum mismatch");}}return manifest;}
 }
 public FileBackup verify(String id)throws IOException{admin();lock();var backup=backup(id);Path temp=verifiedArchive(backup);try{verifyContents(temp);backup.setVerifiedAt(Instant.now());return backups.saveAndFlush(backup);}finally{Files.deleteIfExists(temp);}}
 public FileSuiteService.ItemView restore(String id)throws IOException{
  admin();lock();var backup=backup(id);Path temp=verifiedArchive(backup);try{
   var manifest=verifyContents(temp);if(manifest.items().size()>5000)throw fail(413,"Recovery is limited to 5,000 items per operation");
   var destination=files.createFolder("Recovered backup "+backup.getCreatedAt().toString().replace(':','-'),null);
   Map<Long,Long> recovered=new HashMap<>();var remaining=new ArrayList<>(manifest.items());
   try(var archive=new ZipFile(temp.toFile())){
    while(!remaining.isEmpty()){boolean progress=false;for(var it=remaining.iterator();it.hasNext();){var old=it.next();if(old.parentId()!=null&&!recovered.containsKey(old.parentId()))continue;Long parent=old.parentId()==null?destination.id():recovered.get(old.parentId());
      if(old.folder()){var folder=files.createFolder(old.name(),parent);recovered.put(old.id(),folder.id());}
      else {var history=manifest.versions().stream().filter(v->v.itemId().equals(old.id())).sorted(Comparator.comparingLong(Version::number)).toList();if(history.isEmpty())throw new IOException("File has no saved versions");FileSuiteService.ItemView restored=null;for(var version:history){try(var in=archive.getInputStream(archive.getEntry("content/"+version.key()))){restored=files.recoverContent(restored==null?null:restored.id(),old.name(),parent,version.mediaType(),in,"Recovered from backup "+backup.getId());}}recovered.put(old.id(),restored.id());}
      it.remove();progress=true;
    }if(!progress)throw new IOException("Invalid or cyclic backup hierarchy");}
   }
   return destination;
  }finally{Files.deleteIfExists(temp);}
 }
 public Path download(String id)throws IOException{admin();lock();return verifiedArchive(backup(id));}
 @Transactional(propagation=Propagation.REQUIRES_NEW) public void recordFailure(){var cfg=lock();cfg.setLastRun(Instant.now());cfg.setLastStatus("FAILED: inspect server log and verify storage availability");cfg.setNextRun(Instant.now().plusSeconds(3600));policies.saveAndFlush(cfg);}
}
