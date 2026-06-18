package main.hub.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A friendly name for a module. {@code userId = 0} → default/public alias (seeded from the repo's
 * module list); {@code userId > 0} → a user's personal alias. Resolves to the repo's derived moduleId.
 */
@Entity
@Table(name = "module_alias",
       uniqueConstraints = @UniqueConstraint(columnNames = {"alias", "user_id"}))
public class ModuleAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String alias;

    @Column(name = "module_id", nullable = false)
    private String moduleId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ModuleAlias() {}

    public ModuleAlias(String alias, String moduleId, Long userId) {
        this.alias = alias;
        this.moduleId = moduleId;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public String getModuleId() { return moduleId; }
    public void setModuleId(String moduleId) { this.moduleId = moduleId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
