package de.tum.devopss26.checklistservice.exception;

import de.tum.devopss26.shared.exception.NotFoundException;

public class ChecklistItemNotFoundException extends NotFoundException {

    public ChecklistItemNotFoundException(Long itemId) {
        super("Checklist item not found: " + itemId);
    }
}
