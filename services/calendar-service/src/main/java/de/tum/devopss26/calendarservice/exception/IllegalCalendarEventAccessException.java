package de.tum.devopss26.calendarservice.exception;

import org.jspecify.annotations.NonNull;

import java.util.List;

public class IllegalCalendarEventAccessException extends RuntimeException {

    public IllegalCalendarEventAccessException(long accessorId, IllegalAccessPair pair) {
        super(accessorId + " tried to illegally access calendar event " + pair.eventId + " of owner " + pair.ownerId);
    }

    public IllegalCalendarEventAccessException(long accessorId, List<IllegalAccessPair> pairs) {
        super(accessorId + " tried to illegally access calendar event pairs [(ownerId, eventId), ...]=["
                + String.join(", ", pairs.stream().map(IllegalAccessPair::toString).toList()) + "]");
    }

    public record IllegalAccessPair(long ownerId, long eventId) {

        @Override
        public @NonNull String toString() {
            return "(" + ownerId + ", " + eventId + ")";
        }
    }

}
