package de.tum.devopss26.checklistservice.exception;

import de.tum.devopss26.shared.exception.NotFoundException;

public class ChecklistItemNotInChecklistException extends NotFoundException {

    public ChecklistItemNotInChecklistException(Long itemId, Long checklistId) {
        super("Checklist item " + itemId + " does not belong to checklist " + checklistId);
    }
}
