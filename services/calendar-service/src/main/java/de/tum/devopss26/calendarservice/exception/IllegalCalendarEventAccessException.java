package de.tum.devopss26.calendarservice.exception;

import de.tum.devopss26.shared.exception.ForbiddenException;
import org.jspecify.annotations.NonNull;

public class IllegalCalendarEventAccessException extends ForbiddenException {

    public IllegalCalendarEventAccessException(long accessorId, IllegalAccessPair pair) {
        super(accessorId + " tried to illegally access calendar event " + pair.eventId + " of owner " + pair.ownerId);
    }

    public record IllegalAccessPair(long ownerId, long eventId) {

        @Override
        public @NonNull String toString() {
            return "(" + ownerId + ", " + eventId + ")";
        }
    }

}
