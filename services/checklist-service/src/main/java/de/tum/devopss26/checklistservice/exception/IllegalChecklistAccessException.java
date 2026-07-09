package de.tum.devopss26.checklistservice.exception;

import de.tum.devopss26.shared.exception.ForbiddenException;

public class IllegalChecklistAccessException extends ForbiddenException {

    public IllegalChecklistAccessException(long accessorId, long ownerId, long checklistId) {
        super(accessorId + " tried to illegally access checklist " + checklistId + " of owner " + ownerId);
    }
}
