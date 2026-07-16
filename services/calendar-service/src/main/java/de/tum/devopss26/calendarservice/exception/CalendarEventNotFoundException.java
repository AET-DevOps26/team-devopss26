package de.tum.devopss26.calendarservice.exception;

import de.tum.devopss26.shared.exception.NotFoundException;

/**
 * Thrown when a requested calendar event does not exist.
 */
public class CalendarEventNotFoundException extends NotFoundException {

    /**
     * Creates a new exception for the given event ID.
     *
     * @param eventId the ID of the event that was not found
     */
    public CalendarEventNotFoundException(long eventId) {
        super("Calendar event with id " + eventId + " not found");
    }

}
