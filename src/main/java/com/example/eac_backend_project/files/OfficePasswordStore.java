package com.example.eac_backend_project.files;
import jakarta.persistence.*;import lombok.Getter;import lombok.Setter;import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.time.Instant;
@Entity @Table(name="file_office_passwords") @Getter @Setter
class OfficeRememberedPassword {
 @Id private Long fileId;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @MapsId @JoinColumn(name="file_id") @org.hibernate.annotations.OnDelete(action=org.hibernate.annotations.OnDeleteAction.CASCADE) private FileItem file;
 @Lob @Column(name="secret_box",columnDefinition="TEXT",nullable=false) private String secretBox;
 @Column(nullable=false) private Instant updatedAt=Instant.now();
 @Column(length=180) private String updatedBy;
 @Version private long revision;
}
interface OfficeRememberedPasswordRepository extends JpaRepository<OfficeRememberedPassword,Long>{}
@Service @Transactional
public class OfficePasswordStore {
 private final OfficeRememberedPasswordRepository records;private final FileItemRepository files;private final OfficePasswordCipher cipher;
 public OfficePasswordStore(OfficeRememberedPasswordRepository records,FileItemRepository files,OfficePasswordCipher cipher){this.records=records;this.files=files;this.cipher=cipher;}
 @Transactional(readOnly=true) public char[] load(Long id){return records.findById(id).map(row->cipher.open(id,row.getSecretBox())).orElse(null);}
 public void save(Long id,char[] password){var row=records.findById(id).orElseGet(OfficeRememberedPassword::new);row.setFile(files.findById(id).orElseThrow());row.setSecretBox(cipher.seal(id,password));row.setUpdatedAt(Instant.now());var auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();row.setUpdatedBy(auth==null?"system":auth.getName());records.saveAndFlush(row);}
}
