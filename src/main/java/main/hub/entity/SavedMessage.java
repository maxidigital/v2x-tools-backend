package main.hub.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A user-defined "message": a named handle referenced by name (messageRef). Identity lives in columns;
 * everything that defines the message — {@code moduleAlias}, {@code rootType}, {@code fixups},
 * {@code description} — lives in the opaque {@code data} JSON blob, so the shape can grow (tags, batch,
 * geo, …) without schema migrations. The hub parses {@code data} when resolving the ref.
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

    /** JSON blob: {moduleAlias, rootType, description, fixups:[{path,value}]}. */
    @Column(name = "data", columnDefinition = "TEXT")
    private String data;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public SavedMessage() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
