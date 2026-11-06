package com.example.eac_backend_project.files;
import com.example.eac_backend_project.entity.inventory.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

/** Transport uses ordinary managed records, quotas and ACLs; this bridge never bypasses file access. */
@Service @Transactional
public class TransportManagedFiles {
 private final FileSuiteService files;private final FileItemRepository items;private final FileOperationService operations;
 public TransportManagedFiles(FileSuiteService files,FileItemRepository items,FileOperationService operations){this.files=files;this.items=items;this.operations=operations;}
 public FileItem upload(MultipartFile part)throws IOException{var created=files.upload(part,null);return items.getReferenceById(created.id());}
 public FileSuiteService.ItemView metadata(Long id){return files.get(id);}
 public FileSuiteService.Download download(Long id){return files.download(id,null);}
 public boolean shareNew(Long id,User user){if(!operations.allows(user,"FILE_VIEW")||!operations.allows(user,"FILE_DOWNLOAD"))return false;files.share(id,user.getId(),FilePermission.Access.VIEWER,null,false);return true;}
 public boolean readable(Long id){try{files.get(id);return true;}catch(org.springframework.web.server.ResponseStatusException e){if(e.getStatusCode().value()==403||e.getStatusCode().value()==404)return false;throw e;}}
}
