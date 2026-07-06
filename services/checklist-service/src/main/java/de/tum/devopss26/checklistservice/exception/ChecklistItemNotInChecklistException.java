package de.tum.devopss26.checklistservice.exception;

public class ChecklistItemNotInChecklistException extends RuntimeException {

    public ChecklistItemNotInChecklistException(Long itemId, Long checklistId) {
        super("Checklist item " + itemId + " does not belong to checklist " + checklistId);
    }
}
