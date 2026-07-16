package de.tum.devopss26.checklistservice.exception;

import de.tum.devopss26.shared.exception.NotFoundException;

/**
 * Exception thrown when a checklist item does not belong to the specified checklist.
 * This indicates an inconsistency between the item's parent checklist and the
 * checklist context in which the operation was attempted.
 */
public class ChecklistItemNotInChecklistException extends NotFoundException {

    /**
     * Creates a new exception with a descriptive message.
     *
     * @param itemId      the ID of the checklist item
     * @param checklistId the ID of the checklist it was expected to belong to
     */
    public ChecklistItemNotInChecklistException(Long itemId, Long checklistId) {
        super("Checklist item " + itemId + " does not belong to checklist " + checklistId);
    }
}
