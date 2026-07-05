package de.tum.devopss26.checklistservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "checklist_items")
public class ChecklistItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checklist_id", nullable = false)
    private ChecklistEntity checklist;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private Integer position;
}
