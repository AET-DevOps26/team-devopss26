package de.tum.devopss26.checklistservice.exception;

import de.tum.devopss26.shared.exception.NotFoundException;

public class ChecklistNotFoundException extends NotFoundException {

    public ChecklistNotFoundException(Long id) {
        super("Checklist not found: " + id);
    }
}
