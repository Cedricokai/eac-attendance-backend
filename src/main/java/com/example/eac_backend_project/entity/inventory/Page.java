package com.example.eac_backend_project.entity.inventory;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "pages")
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String path;

    private String description;

    @Column(nullable = false)
    private Boolean isPublic = false;

    private String iconName;

    @Column(nullable = false)
    private Integer displayOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Module module = Module.GENERAL;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean requiresAuth = true;

    @ManyToMany
    @JoinTable(
            name = "page_roles",
            joinColumns = @JoinColumn(name = "page_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // NEW: Many-to-many relationship with User
    @ManyToMany
    @JoinTable(
            name = "user_pages",
            joinColumns = @JoinColumn(name = "page_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> users = new HashSet<>();

    public enum Module {
        AUTH("Authentication"),
        ATTENDANCE("Attendance"),
        LEAVE("Leave Management"),
        PAYROLL("Payroll"),
        INVENTORY("Inventory"),
        PROCUREMENT("Procurement"),
        TRANSPORT("Transport"),
        HR("Human Resources"),
        ADMIN("Administration"),
        GENERAL("General");

        private final String displayName;

        Module(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Constructors
    public Page() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public Module getModule() { return module; }
    public void setModule(Module module) { this.module = module; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getRequiresAuth() { return requiresAuth; }
    public void setRequiresAuth(Boolean requiresAuth) { this.requiresAuth = requiresAuth; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    // NEW: Getter and Setter for users
    public Set<User> getUsers() { return users; }
    public void setUsers(Set<User> users) { this.users = users; }
}