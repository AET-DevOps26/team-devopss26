package de.tum.devopss26.noteservice.exception;

import de.tum.devopss26.shared.exception.NotFoundException;

public class NoteNotFoundException extends NotFoundException {

    public NoteNotFoundException(long noteId) {
        super("Note with id " + noteId + " not found");
    }

}
