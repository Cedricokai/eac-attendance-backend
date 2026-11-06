package com.example.eac_backend_project.files;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;

interface FileItemRepository extends JpaRepository<FileItem,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select f from FileItem f where f.id=:id") Optional<FileItem> lock(@Param("id") Long id);
    @Query("select f from FileItem f where f.deletedAt is not null and (f.deletionGroup=f.id or f.deletionGroup is null) and lower(f.name) like lower(concat('%',:q,'%'))")
    Page<FileItem> organizationTrash(@Param("q") String q,Pageable page);
    List<FileItem> findByParent_Id(Long parentId);
    boolean existsByParent_Id(Long parentId);
    boolean existsByParent_IdAndDeletedAtIsNull(Long parentId);
    List<FileItem> findByDeletionGroup(Long group);
    @Query("select f from FileItem f where f.owner.id=:owner and ((:trash=true and f.deletedAt is not null and (f.deletionGroup=f.id or f.deletionGroup is null)) or (:trash=false and f.deletedAt is null)) and lower(f.name) like lower(concat('%',:q,'%'))")
    Page<FileItem> owned(@Param("owner") Long owner,@Param("trash") boolean trash,@Param("q") String q,Pageable page);
    @Query("select f from FileItem f where f.deletedAt is null and ((:parent is null and f.parent is null and f.owner.id=:owner) or f.parent.id=:parent) and lower(f.name) like lower(concat('%',:q,'%'))")
    Page<FileItem> children(@Param("owner") Long owner,@Param("parent") Long parent,@Param("q") String q,Pageable page);
    @Query("select coalesce(sum(v.sizeBytes),0) from FileVersion v where v.item.owner.id=:owner") long used(@Param("owner") Long owner);
    @Query("select coalesce(sum(v.sizeBytes),0) from FileVersion v") long totalUsed();
    long countByOwner_IdAndFolderAndDeletedAtIsNull(Long owner,boolean folder);
}
interface FileVersionRepository extends JpaRepository<FileVersion,Long> {
    List<FileVersion> findByItem_IdOrderByNumberDesc(Long id);
    Optional<FileVersion> findByItem_IdAndNumber(Long id,long number);
}
interface FilePermissionRepository extends JpaRepository<FilePermission,Long> {
    @Query("select p from FilePermission p where p.item.owner.id=:owner and p.item.deletedAt is null order by p.grantedAt desc") Page<FilePermission> sentForReview(@Param("owner") Long owner,Pageable page);
    Optional<FilePermission> findByItem_IdAndRecipient_Id(Long item,Long user);
    List<FilePermission> findByItem_Id(Long item);
    @Query("select p from FilePermission p where p.recipient.id=:user and (p.expiresAt is null or p.expiresAt>CURRENT_TIMESTAMP) and p.item.deletedAt is null") List<FilePermission> received(@Param("user") Long user);
    List<FilePermission> findByItem_Owner_Id(Long user);
    void deleteByItem_Id(Long id);
}
interface FileStarRepository extends JpaRepository<FileStar,Long> {
    boolean existsByItem_IdAndUser_Id(Long item,Long user);
    void deleteByItem_IdAndUser_Id(Long item,Long user);
    void deleteByItem_Id(Long item);
    @Query("select s.item from FileStar s where s.user.id=:user and s.item.deletedAt is null") List<FileItem> starred(@Param("user") Long user);
}
interface FileSettingsRepository extends JpaRepository<FileSettings,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from FileSettings s where s.id=1") Optional<FileSettings> lock();
}
interface FileEventRepository extends JpaRepository<FileEvent,Long>,org.springframework.data.jpa.repository.JpaSpecificationExecutor<FileEvent> {
    Page<FileEvent> findByOwnerIdOrderByCreatedAtDesc(Long owner,Pageable page);
    Page<FileEvent> findByItemIdOrderByCreatedAtDesc(Long item,Pageable page);
}
interface FileCommentRepository extends JpaRepository<FileComment,Long> {
    List<FileComment> findByItem_IdOrderByCreatedAtAsc(Long id);
    void deleteByItem_Id(Long id);
}
interface FileGuestRepository extends JpaRepository<FileGuestInvitation,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select g from FileGuestInvitation g where g.tokenHash=:hash") Optional<FileGuestInvitation> lockByToken(@Param("hash") String hash);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select g from FileGuestInvitation g where g.id=:id") Optional<FileGuestInvitation> lockById(@Param("id") Long id);
    List<FileGuestInvitation> findByItem_Id(Long id);
    List<FileGuestInvitation> findByGrantedBy_IdAndUploadOnlyTrue(Long user);
    List<FileGuestInvitation> findByUploadOnlyTrue();
    void deleteByItem_Id(Long id);
}
interface FileCollaborationRepository extends JpaRepository<FileCollaborationSnapshot,Long>{}
interface FileBackupRepository extends JpaRepository<FileBackup,String>{List<FileBackup> findAllByOrderByCreatedAtDesc();}
interface FileBackupPolicyRepository extends JpaRepository<FileBackupPolicy,Long>{
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from FileBackupPolicy p where p.id=1") Optional<FileBackupPolicy> lock();
}
interface FileBackupChunkRepository extends JpaRepository<FileBackupChunk,Long>{
 List<FileBackupChunk> findByBackup_IdOrderByNumberAsc(String backup);
 void deleteByBackup_Id(String backup);
}
interface FileStorageQuotaRepository extends JpaRepository<FileStorageQuota,Long>{Optional<FileStorageQuota> findByTargetTypeAndTargetId(String type,Long id);}
interface FileNotificationStateRepository extends JpaRepository<FileNotificationState,Long>{}
