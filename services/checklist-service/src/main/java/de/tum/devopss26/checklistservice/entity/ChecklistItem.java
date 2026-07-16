package de.tum.devopss26.checklistservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity representing a single item within a checklist.
 * Each item belongs to exactly one checklist and has a text description,
 * a completion status, and a display position within the checklist.
 */
@Entity
@Getter
@Setter
@Table(name = "checklist_items")
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checklist_id", nullable = false)
    private Checklist checklist;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private Integer position;
}
