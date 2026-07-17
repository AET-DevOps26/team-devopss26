package de.tum.devopss26.checklistservice.exception;

import de.tum.devopss26.shared.exception.NotFoundException;

/**
 * Thrown when a checklist item does not belong to the expected parent checklist.
 */
public class ChecklistItemNotInChecklistException extends NotFoundException {

    /**
     * Constructs a new exception describing the mismatch.
     *
     * @param itemId     the ID of the item that does not belong
     * @param checklistId the ID of the checklist it was expected to belong to
     */
    public ChecklistItemNotInChecklistException(Long itemId, Long checklistId) {
        super("Checklist item " + itemId + " does not belong to checklist " + checklistId);
    }
}
