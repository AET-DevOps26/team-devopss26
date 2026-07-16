package de.tum.devopss26.calendarservice.exception;

import de.tum.devopss26.shared.exception.NotFoundException;

/**
 * Exception thrown when a calendar event with a given ID does not exist.
 */
public class CalendarEventNotFoundException extends NotFoundException {

    /**
     * Constructs a new exception for the missing event.
     *
     * @param eventId the ID of the calendar event that was not found
     */
    public CalendarEventNotFoundException(long eventId) {
        super("Calendar event with id " + eventId + " not found");
    }

}
