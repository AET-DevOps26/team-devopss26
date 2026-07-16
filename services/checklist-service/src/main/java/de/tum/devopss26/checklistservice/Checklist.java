package de.tum.devopss26.checklistservice;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class Checklist {

    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private List<ChecklistItem> items;

    public Checklist() {}

    /**
     * Creates a checklist with the given ID and title, initializing
     * {@code createdAt} to the current time and adding a single sample item.
     *
     * @param id    the checklist ID
     * @param title the checklist title
     */
    public Checklist(Long id, String title) {
        this.id = id;
        this.title = title;
        this.createdAt = LocalDateTime.now();
        this.items = List.of(new ChecklistItem(1L, "Sample item", false, 1));
    }
}
