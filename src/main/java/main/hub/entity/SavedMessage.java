package main.hub.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A user-defined "message": a named (moduleId, typeName) plus sticky fixups + description. Referenced
 * by name (messageRef). {@code fixups} is the JSON array of {path,value} (stored as text).
 */
@Entity
@Table(name = "saved_message",
       uniqueConstraints = @UniqueConstraint(columnNames = {"name", "user_id"}))
public class SavedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "module_id", nullable = false)
    private String moduleId;

    @Column(name = "type_name", nullable = false)
    private String typeName;

    /** JSON array of {path, value} — the sticky fixups. */
    @Column(name = "fixups", columnDefinition = "TEXT")
    private String fixups;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public SavedMessage() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getModuleId() { return moduleId; }
    public void setModuleId(String moduleId) { this.moduleId = moduleId; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public String getFixups() { return fixups; }
    public void setFixups(String fixups) { this.fixups = fixups; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
