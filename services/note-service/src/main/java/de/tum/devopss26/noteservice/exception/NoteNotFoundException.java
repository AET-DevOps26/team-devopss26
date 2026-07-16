package de.tum.devopss26.noteservice.exception;

import de.tum.devopss26.shared.exception.NotFoundException;

/**
 * Exception thrown when a requested note cannot be found in the database.
 * <p>
 * Extends {@link NotFoundException}, which results in an HTTP {@code 404 Not Found}
 * response when handled by the global exception handler.
 * </p>
 */
public class NoteNotFoundException extends NotFoundException {

    /**
     * Constructs a new exception for the missing note.
     *
     * @param noteId the ID of the note that was not found
     */
    public NoteNotFoundException(long noteId) {
        super("Note with id " + noteId + " not found");
    }

}
