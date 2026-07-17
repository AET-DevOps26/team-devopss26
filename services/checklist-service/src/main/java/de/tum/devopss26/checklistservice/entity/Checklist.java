package de.tum.devopss26.checklistservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Root aggregate for a user's checklist. Contains a ordered list of {@link ChecklistItem}
 * children. Items are managed via JPA cascade — persisting, updating, or deleting a Checklist
 * automatically propagates to its items. {@code orphanRemoval} ensures items removed from the
 * collection are deleted from the database.
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
