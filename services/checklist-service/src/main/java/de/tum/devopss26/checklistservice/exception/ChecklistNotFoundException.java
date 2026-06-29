package de.tum.devopss26.checklistservice.exception;

public class ChecklistNotFoundException extends RuntimeException {

    public ChecklistNotFoundException(Long id) {
        super("Checklist not found: " + id);
    }
}
