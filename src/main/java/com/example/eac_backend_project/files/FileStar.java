package com.example.eac_backend_project.files;
import com.example.eac_backend_project.entity.inventory.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="file_stars",uniqueConstraints=@UniqueConstraint(columnNames={"item_id","user_id"})) @Getter @Setter
public class FileStar {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="item_id") private FileItem item;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") private User user;
}
