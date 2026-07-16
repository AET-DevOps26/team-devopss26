package de.tum.devopss26.checklistservice.exception;

import de.tum.devopss26.shared.exception.NotFoundException;

/**
 * Exception thrown when a requested checklist item cannot be found in the database.
 * This typically occurs when an operation references an item ID that does not exist.
 */
public class ChecklistItemNotFoundException extends NotFoundException {

    /**
     * Creates a new exception with a descriptive message.
     *
     * @param itemId the ID of the checklist item that was not found
     */
    public ChecklistItemNotFoundException(Long itemId) {
        super("Checklist item not found: " + itemId);
    }
}
