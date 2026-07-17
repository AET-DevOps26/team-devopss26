package de.tum.devopss26.noteservice.exception;

import de.tum.devopss26.shared.exception.ForbiddenException;

/**
 * Thrown when a user attempts to access a note they do not own.
 */
public class IllegalNoteAccessException extends ForbiddenException {

	/**
	 * Constructs a new exception describing the unauthorized access attempt.
	 *
	 * @param accessorId the ID of the user who tried to access the note
	 * @param pair       a record containing the note's owner ID and the note ID
	 */
	public IllegalNoteAccessException(long accessorId, IllegalAccessPair pair) {
		super(accessorId + " tried to illegally access note " + pair.noteId + " of owner " + pair.ownerId);
	}

	/**
	 * A record pairing the owner of a note with the note's ID.
	 *
	 * @param ownerId the ID of the user who owns the note
	 * @param noteId  the ID of the note
	 */
	public record IllegalAccessPair(long ownerId, long noteId) {
	}

}
