package com.example.eac_backend_project.files;

import com.example.eac_backend_project.entity.inventory.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name="file_permissions",uniqueConstraints=@UniqueConstraint(columnNames={"item_id","recipient_id"}))
@Getter @Setter
public class FilePermission {
    public enum Access { VIEWER, COMMENTER, EDITOR, MANAGER }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="item_id") private FileItem item;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="recipient_id") private User recipient;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Access access;
    @Column(nullable=false) private Instant grantedAt=Instant.now();
    private Instant expiresAt;
    @Column(nullable=false,columnDefinition="boolean default false") private boolean downloadBlocked;
}
