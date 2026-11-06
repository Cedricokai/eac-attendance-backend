package com.example.eac_backend_project.files;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.repository.JpaRepository;

@Entity @Table(name="file_office_sessions",indexes=@Index(name="idx_office_file_state",columnList="file_id,state")) @Getter @Setter
class OfficeSession {
 @Id @Column(length=36) private String id;
 @ManyToOne(optional=false) @JoinColumn(name="file_id") @org.hibernate.annotations.OnDelete(action=org.hibernate.annotations.OnDeleteAction.CASCADE) private FileItem file;
 @Column(nullable=false,length=100,unique=true) private String documentKey;
 @Column(nullable=false) private long savedVersion;
 @Column(nullable=false) private boolean writable;
 @Column(nullable=false,length=20) private String state="OPEN";
 @Column(nullable=false) private Instant expiresAt;
 @Column(nullable=false) private Instant createdAt=Instant.now();
 @Lob @Column(columnDefinition="TEXT") private String participants="{}";
 @Column(length=64) private String lastChecksum;
 @Version private long revision;
}
interface OfficeSessionRepository extends JpaRepository<OfficeSession,String> {
 java.util.List<OfficeSession> findByFile_IdAndStateOrderByCreatedAtDesc(Long id,String state);
}
