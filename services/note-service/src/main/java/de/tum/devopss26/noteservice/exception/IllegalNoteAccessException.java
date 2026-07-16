package de.tum.devopss26.noteservice.exception;

import de.tum.devopss26.shared.exception.ForbiddenException;

/**
 * Exception thrown when a user attempts to access a note they do not own.
 * <p>
 * Extends {@link ForbiddenException}, which results in an HTTP {@code 403 Forbidden}
 * response when handled by the global exception handler.
 * </p>
 */
public class IllegalNoteAccessException extends ForbiddenException {

	/**
	 * Constructs a new exception for the illegal access attempt.
	 *
	 * @param accessorId the ID of the user who attempted the access
	 * @param pair       a record containing the owner ID and the note ID involved
	 */
	public IllegalNoteAccessException(long accessorId, IllegalAccessPair pair) {
		super(accessorId + " tried to illegally access note " + pair.noteId + " of owner " + pair.ownerId);
	}

	/**
	 * Record holding the owner ID and note ID involved in an illegal access attempt.
	 *
	 * @param ownerId the ID of the note owner
	 * @param noteId  the ID of the note that was accessed illegally
	 */
	public record IllegalAccessPair(long ownerId, long noteId) {
	}

}
