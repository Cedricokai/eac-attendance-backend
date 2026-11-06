package com.example.eac_backend_project.files;
import com.example.eac_backend_project.entity.inventory.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
@Entity @Table(name="file_external_invitations",indexes=@Index(name="idx_file_guest_token",columnList="tokenHash",unique=true)) @Getter @Setter
public class FileGuestInvitation {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private FileItem item;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private User grantedBy;
    @Column(nullable=false,length=160) private String name;
    @Column(nullable=false,length=254) private String email;
    @Column(nullable=false,length=20) private String access;
    @Column(nullable=false,length=64,unique=true) private String tokenHash;
    private Instant expiresAt;
    private Instant revokedAt;
    private Instant createdAt=Instant.now();
    private String codeHash;
    private Instant codeExpiresAt;
    private Instant codeSentAt;
    private int failedAttempts;
    private int codeRequests;
    private Instant codeWindowAt;
    private String sessionHash;
    private Instant sessionExpiresAt;
    private boolean uploadOnly;
    private String requestTitle;
    @Column(length=2000) private String requestDescription;
    private long maxUploadBytes=52428800L;
    private int maxUploads=20;
    private int uploadCount;
}
