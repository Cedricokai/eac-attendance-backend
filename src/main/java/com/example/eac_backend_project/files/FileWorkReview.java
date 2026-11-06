package com.example.eac_backend_project.files;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;

/** A receipt for a saved version, never a second copy of the document. */
@Entity @Table(name="file_work_returns",uniqueConstraints=@UniqueConstraint(columnNames={"file_id","employee_id","version_number"})) @Getter @Setter
class FileWorkReturn {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="file_id") @org.hibernate.annotations.OnDelete(action=org.hibernate.annotations.OnDeleteAction.CASCADE) private FileItem file;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="employee_id") private com.example.eac_backend_project.entity.inventory.User employee;
 @Column(name="version_number",nullable=false) private long versionNumber;
 @Column(nullable=false,length=64) private String checksum;
 @Column(nullable=false,length=2000) private String note="";
 @Column(nullable=false) private Instant returnedAt=Instant.now();
 private Instant reviewedAt;
}
interface FileWorkReturnRepository extends JpaRepository<FileWorkReturn,Long>{
 Page<FileWorkReturn> findByFile_Owner_IdOrderByReturnedAtDesc(Long owner,Pageable page);
 Optional<FileWorkReturn> findByFile_IdAndEmployee_IdAndVersionNumber(Long file,Long employee,long version);
 void deleteByFile_Id(Long file);
}
