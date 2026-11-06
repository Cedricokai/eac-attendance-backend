package com.example.eac_backend_project.files;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="file_backup_chunks",uniqueConstraints=@UniqueConstraint(columnNames={"backup_id","chunk_number"})) @Getter @Setter
public class FileBackupChunk {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="backup_id") private FileBackup backup;
 @Column(name="chunk_number") private int number;
 @Lob @Column(columnDefinition="LONGBLOB") private byte[] data;
}
