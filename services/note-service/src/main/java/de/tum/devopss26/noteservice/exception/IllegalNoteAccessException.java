package de.tum.devopss26.noteservice.exception;

import de.tum.devopss26.shared.exception.ForbiddenException;
import org.jspecify.annotations.NonNull;

public class IllegalNoteAccessException extends ForbiddenException {

    public IllegalNoteAccessException(long accessorId, IllegalAccessPair pair) {
        super(accessorId + " tried to illegally access note " + pair.noteId + " of owner " + pair.ownerId);
    }

    public record IllegalAccessPair(long ownerId, long noteId) {

        @Override
        public @NonNull String toString() {
            return "(" + ownerId + ", " + noteId + ")";
        }
    }

}
