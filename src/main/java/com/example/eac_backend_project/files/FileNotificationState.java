package com.example.eac_backend_project.files;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import com.example.eac_backend_project.entity.inventory.User;
@Entity @Table(name="file_notification_state") @Getter @Setter
public class FileNotificationState {
 @Id @Column(name="user_id")private Long userId;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @MapsId @JoinColumn(name="user_id") @org.hibernate.annotations.OnDelete(action=org.hibernate.annotations.OnDeleteAction.CASCADE) private User user;
 private Instant lastStorageNotice;
}
