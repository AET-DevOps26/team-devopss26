package de.tum.devopss26.checklistservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A single item within a {@link Checklist}. Carries a text description, a completion flag, and
 * an ordinal position used for display ordering ({@code ORDER BY completed ASC, position ASC}).
 * The owning-side foreign key ({@code checklist_id}) establishes the many-to-one relationship;
 * lifecycle is managed by the parent's cascade configuration.
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
