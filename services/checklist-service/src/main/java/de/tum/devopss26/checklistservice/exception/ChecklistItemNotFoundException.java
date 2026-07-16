package de.tum.devopss26.checklistservice.exception;

import de.tum.devopss26.shared.exception.NotFoundException;

/**
 * Thrown when a requested checklist item cannot be found.
 */
public class ChecklistItemNotFoundException extends NotFoundException {

    /**
     * Constructs a new exception for the given item ID.
     *
     * @param itemId the ID of the item that was not found
     */
    public ChecklistItemNotFoundException(Long itemId) {
        super("Checklist item not found: " + itemId);
    }
}
