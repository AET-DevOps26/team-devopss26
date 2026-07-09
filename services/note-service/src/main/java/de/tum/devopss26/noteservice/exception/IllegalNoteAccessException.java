package de.tum.devopss26.noteservice.exception;

import de.tum.devopss26.shared.exception.ForbiddenException;

public class IllegalNoteAccessException extends ForbiddenException {

	public IllegalNoteAccessException(long accessorId, IllegalAccessPair pair) {
		super(accessorId + " tried to illegally access note " + pair.noteId + " of owner " + pair.ownerId);
	}

	public record IllegalAccessPair(long ownerId, long noteId) {
	}

}
