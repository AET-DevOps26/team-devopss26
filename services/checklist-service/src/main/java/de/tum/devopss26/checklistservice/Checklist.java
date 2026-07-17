package de.tum.devopss26.checklistservice;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data class representing a checklist DTO used in sample/test data.
 */
@Getter
@Setter
public class Checklist {

    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private List<ChecklistItem> items;

    public Checklist() {}

    public Checklist(Long id, String title) {
        this.id = id;
        this.title = title;
        this.createdAt = LocalDateTime.now();
        this.items = List.of(new ChecklistItem(1L, "Sample item", false, 1));
    }
}
