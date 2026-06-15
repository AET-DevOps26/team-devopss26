package de.tum.devopss26.checklistservice;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChecklistItem {

    private Long id;
    private String text;
    private boolean completed;
    private Integer position;

    public ChecklistItem() {}

    public ChecklistItem(Long id, String text, boolean completed, Integer position) {
        this.id = id;
        this.text = text;
        this.completed = completed;
        this.position = position;
    }
}
