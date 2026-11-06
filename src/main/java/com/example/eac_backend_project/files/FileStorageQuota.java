package com.example.eac_backend_project.files;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="file_storage_quotas",uniqueConstraints=@UniqueConstraint(columnNames={"target_type","target_id"})) @Getter @Setter
public class FileStorageQuota {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)private Long id;
 @Column(name="target_type",nullable=false)private String targetType;
 @Column(name="target_id",nullable=false)private Long targetId;
 private long limitBytes;
}
