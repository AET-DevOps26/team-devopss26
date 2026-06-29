package de.tum.devopss26.checklistservice.exception;

public class ChecklistItemNotFoundException extends RuntimeException {

    public ChecklistItemNotFoundException(Long itemId) {
        super("Checklist item not found: " + itemId);
    }
}
