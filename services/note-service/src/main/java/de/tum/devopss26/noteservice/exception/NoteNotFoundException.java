package de.tum.devopss26.noteservice.exception;

import de.tum.devopss26.shared.exception.NotFoundException;

/**
 * Thrown when a requested note does not exist.
 */
public class NoteNotFoundException extends NotFoundException {

    /**
     * Constructs a new exception for the given note ID.
     *
     * @param noteId the ID of the note that was not found
     */
    public NoteNotFoundException(long noteId) {
        super("Note with id " + noteId + " not found");
    }

}
