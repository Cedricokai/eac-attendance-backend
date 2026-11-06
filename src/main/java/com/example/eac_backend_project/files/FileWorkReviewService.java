package com.example.eac_backend_project.files;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import static org.springframework.http.HttpStatus.*;

@Service @Transactional(rollbackFor=Exception.class)
public class FileWorkReviewService {
 private final FileSuiteService files;private final FileWorkReturnRepository returns;private final FileVersionRepository versions;private final FilePermissionRepository permissions;private final OfficeSheetPolicyRepository policies;private final OfficePasswordVault passwords;private final FileNotificationService notifications;
 public FileWorkReviewService(FileSuiteService files,FileWorkReturnRepository returns,FileVersionRepository versions,FilePermissionRepository permissions,OfficeSheetPolicyRepository policies,OfficePasswordVault passwords,FileNotificationService notifications){this.files=files;this.returns=returns;this.versions=versions;this.permissions=permissions;this.policies=policies;this.passwords=passwords;this.notifications=notifications;}
 public record Receipt(Long id,Long fileId,String fileName,Long employeeId,String employee,long version,String savedBy,String note,Instant returnedAt,Instant reviewedAt,boolean deleted){}
 public record Sent(Long fileId,String fileName,boolean folder,String employee,String access,Instant sentAt,Instant expiresAt,long currentVersion){}
 private Receipt view(FileWorkReturn row){var file=row.getFile();var saved=versions.findByItem_IdAndNumber(file.getId(),row.getVersionNumber());return new Receipt(row.getId(),file.getId(),file.getName(),row.getEmployee().getId(),row.getEmployee().getName(),row.getVersionNumber(),saved.map(v->v.getEditor().getName()).orElse("Version no longer retained"),row.getNote(),row.getReturnedAt(),row.getReviewedAt(),file.getDeletedAt()!=null);}
 public Receipt sendBack(Long id,long expectedVersion,String note){
  var user=files.currentUser();var file=files.officeLock(id);var access=files.officeView(file);
  if(file.isFolder()||file.getOwner().getId().equals(user.getId())||!access.canEdit()||user.getEmployee()==null)throw new ResponseStatusException(FORBIDDEN,"Only an employee with current editing access can send a shared document back to its owner");
  if(expectedVersion!=file.getCurrentVersion())throw new ResponseStatusException(CONFLICT,"The saved version changed. Refresh the file before sending it back");
  if(passwords.needsPassword(id)||policies.findById(id).filter(OfficeSheetPolicy::isPending).isPresent())throw new ResponseStatusException(CONFLICT,"Verify the opening password and finish saving sheet restrictions before sending this file back");
  if(note!=null&&note.length()>2000)throw new ResponseStatusException(BAD_REQUEST,"Describe your work in at most 2000 characters");
  var existing=returns.findByFile_IdAndEmployee_IdAndVersionNumber(id,user.getId(),expectedVersion);if(existing.isPresent())return view(existing.get());
  var version=versions.findByItem_IdAndNumber(id,expectedVersion).orElseThrow(()->new ResponseStatusException(CONFLICT,"Save your changes before sending this file back"));
  var row=new FileWorkReturn();row.setFile(file);row.setEmployee(user);row.setVersionNumber(expectedVersion);row.setChecksum(version.getChecksum());row.setNote(note==null?"":note.trim());returns.saveAndFlush(row);
  files.officeEvent(file,"WORK_RETURNED_V"+expectedVersion);notifications.notice(file.getOwner(),"Work returned for review",user.getName()+" returned "+file.getName()+" (saved version "+expectedVersion+") for your review.",id);return view(row);
 }
 public Page<Receipt> inbox(int page){var user=files.currentUser();return returns.findByFile_Owner_IdOrderByReturnedAtDesc(user.getId(),PageRequest.of(Math.max(0,page),25)).map(this::view);}
 public Page<Sent> sent(int page){var user=files.currentUser();return permissions.sentForReview(user.getId(),PageRequest.of(Math.max(0,page),25)).map(p->new Sent(p.getItem().getId(),p.getItem().getName(),p.getItem().isFolder(),p.getRecipient().getName(),p.getAccess().name(),p.getGrantedAt(),p.getExpiresAt(),p.getItem().getCurrentVersion()));}
 public Receipt reviewed(Long id){var row=returns.findById(id).orElseThrow(()->new ResponseStatusException(NOT_FOUND,"Returned work not found"));var user=files.currentUser();if(!row.getFile().getOwner().getId().equals(user.getId()))throw new ResponseStatusException(FORBIDDEN,"Only the file owner can review this return");files.officeLock(row.getFile().getId());if(row.getReviewedAt()==null){row.setReviewedAt(Instant.now());files.officeEvent(row.getFile(),"WORK_REVIEWED_V"+row.getVersionNumber());}return view(row);}
}
