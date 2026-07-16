package de.tum.devopss26.checklistservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing a checklist owned by a user.
 * Each checklist has a title, a creation timestamp, and an ordered list
 * of {@link ChecklistItem} entities. Items are sorted by completion status
 * and position.
 */
@Entity
@Getter
@Setter
@Table(name = "checklists")
public class Checklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "checklist", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("completed ASC, position ASC")
    private List<ChecklistItem> items = new ArrayList<>();
}
