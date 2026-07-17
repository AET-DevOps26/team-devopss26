package de.tum.devopss26.checklistservice.exception;

import de.tum.devopss26.shared.exception.NotFoundException;

/**
 * Thrown when a requested checklist cannot be found.
 */
public class ChecklistNotFoundException extends NotFoundException {

    /**
     * Constructs a new exception for the given checklist ID.
     *
     * @param id the ID of the checklist that was not found
     */
    public ChecklistNotFoundException(Long id) {
        super("Checklist not found: " + id);
    }
}
