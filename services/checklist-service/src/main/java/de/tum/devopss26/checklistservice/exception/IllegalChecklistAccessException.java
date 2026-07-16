package de.tum.devopss26.checklistservice.exception;

import de.tum.devopss26.shared.exception.ForbiddenException;

/**
 * Exception thrown when a user attempts to access a checklist they do not own.
 * This enforces the access control rule that users can only interact with
 * their own checklists.
 */
public class IllegalChecklistAccessException extends ForbiddenException {

    /**
     * Creates a new exception with a descriptive message.
     *
     * @param accessorId  the ID of the user who attempted the illegal access
     * @param ownerId     the ID of the user who owns the checklist
     * @param checklistId the ID of the checklist that was illegally accessed
     */
    public IllegalChecklistAccessException(long accessorId, long ownerId, long checklistId) {
        super(accessorId + " tried to illegally access checklist " + checklistId + " of owner " + ownerId);
    }
}
