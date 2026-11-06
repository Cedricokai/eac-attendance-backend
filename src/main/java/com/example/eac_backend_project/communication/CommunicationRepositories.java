package com.example.eac_backend_project.communication;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;
import java.util.*;
interface CommunicationConversationRepository extends JpaRepository<CommunicationConversation,Long> {
 Optional<CommunicationConversation> findByDirectKey(String key);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from CommunicationConversation c where c.id=:id") Optional<CommunicationConversation> lock(@Param("id")Long id);
}
interface CommunicationParticipantRepository extends JpaRepository<CommunicationParticipant,Long> {
 List<CommunicationParticipant> findByUser_IdAndLeftAtIsNull(Long userId);
 List<CommunicationParticipant> findByConversation_Id(Long id);
 Optional<CommunicationParticipant> findByConversation_IdAndUser_Id(Long conversation,Long user);
}
interface CommunicationMessageRepository extends JpaRepository<CommunicationMessage,Long> {
 Optional<CommunicationMessage> findByConversation_IdAndSender_IdAndClientId(Long conversation,Long sender,String clientId);
 List<CommunicationMessage> findByConversation_IdAndSequenceLessThanAndSequenceGreaterThanEqualOrderBySequenceDesc(Long id,long before,long joined,Pageable page);
 long countByConversation_IdAndSequenceGreaterThanAndSender_IdNotAndDeletedAtIsNull(Long id,long lastRead,Long userId);
 List<CommunicationMessage> findByConversation_IdAndSequenceGreaterThanAndSender_IdNotAndDeletedAtIsNullOrderBySequenceDesc(Long id,long lastRead,Long userId,Pageable page);
 List<CommunicationMessage> findByDeletedAtIsNotNull(Pageable page);
}
interface CommunicationPolicyRepository extends JpaRepository<CommunicationPolicy,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from CommunicationPolicy p where p.id=1") Optional<CommunicationPolicy> lock();
}
interface CommunicationAuditRepository extends JpaRepository<CommunicationAudit,Long> {}
interface CommunicationUploadRepository extends JpaRepository<CommunicationUpload,Long> { Optional<CommunicationUpload> findByFileId(Long id); }
interface CommunicationBackupRepository extends JpaRepository<CommunicationBackup,String> {}
