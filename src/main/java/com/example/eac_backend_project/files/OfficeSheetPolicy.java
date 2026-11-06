package com.example.eac_backend_project.files;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

@Entity @Table(name="file_office_sheet_policies") @Getter @Setter
class OfficeSheetPolicy {
 @Id private Long fileId;
 @ManyToOne(optional=false) @MapsId @JoinColumn(name="file_id")
 @org.hibernate.annotations.OnDelete(action=org.hibernate.annotations.OnDeleteAction.CASCADE) private FileItem file;
 @Lob @Column(columnDefinition="LONGTEXT",nullable=false) private String rules="[]";
 @Column(nullable=false) private boolean pending=true;
 @Column(nullable=false) private long policyVersion;
 @Column(nullable=false) private Instant changedAt=Instant.now();
 private String changedBy;
 @Version private long revision;
}
interface OfficeSheetPolicyRepository extends JpaRepository<OfficeSheetPolicy,Long>{}

@Entity @Table(name="file_office_sheet_policy_audit",indexes=@Index(name="idx_sheet_policy_file",columnList="fileId")) @Getter @Setter
class OfficeSheetPolicyAudit {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long fileId;
 @Column(nullable=false) private long policyVersion;
 @Column(nullable=false) private Long actorId;
 @Column(nullable=false) private String actorName;
 @Column(nullable=false) private Instant createdAt=Instant.now();
 @Lob @Column(columnDefinition="LONGTEXT",nullable=false) private String rules;
}
interface OfficeSheetPolicyAuditRepository extends JpaRepository<OfficeSheetPolicyAudit,Long>{
 java.util.List<OfficeSheetPolicyAudit> findTop50ByFileIdOrderByIdDesc(Long fileId);
}
