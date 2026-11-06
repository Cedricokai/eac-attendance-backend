package com.example.eac_backend_project.files;
import com.example.eac_backend_project.entity.inventory.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
@Entity @Table(name="file_comments",indexes=@Index(name="idx_file_comment_item",columnList="item_id")) @Getter @Setter
public class FileComment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="item_id") private FileItem item;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private User author;
    @Column(nullable=false,length=4000) private String body;
    private Long threadId;
    private boolean resolved;
    private Instant createdAt=Instant.now();
}
