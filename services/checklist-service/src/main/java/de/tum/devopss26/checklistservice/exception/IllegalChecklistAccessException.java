package de.tum.devopss26.checklistservice.exception;

import de.tum.devopss26.shared.exception.ForbiddenException;

/**
 * Thrown when a user attempts to access a checklist they do not own.
 */
public class IllegalChecklistAccessException extends ForbiddenException {

    /**
     * Constructs a new exception describing the unauthorized access attempt.
     *
     * @param accessorId  the ID of the user who tried to access the checklist
     * @param ownerId    the ID of the user who owns the checklist
     * @param checklistId the ID of the checklist
     */
    public IllegalChecklistAccessException(long accessorId, long ownerId, long checklistId) {
        super(accessorId + " tried to illegally access checklist " + checklistId + " of owner " + ownerId);
    }
}
