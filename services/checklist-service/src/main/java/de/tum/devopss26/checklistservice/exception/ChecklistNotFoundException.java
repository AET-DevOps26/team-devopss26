package de.tum.devopss26.checklistservice.exception;

import de.tum.devopss26.shared.exception.NotFoundException;

/**
 * Exception thrown when a requested checklist cannot be found in the database.
 * This typically occurs when an operation references a checklist ID that does not exist.
 */
public class ChecklistNotFoundException extends NotFoundException {

    /**
     * Creates a new exception with a descriptive message.
     *
     * @param id the ID of the checklist that was not found
     */
    public ChecklistNotFoundException(Long id) {
        super("Checklist not found: " + id);
    }
}
