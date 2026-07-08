package de.tum.devopss26.calendarservice.exception;

import de.tum.devopss26.shared.exception.NotFoundException;

public class CalendarEventNotFoundException extends NotFoundException {

    public CalendarEventNotFoundException(long eventId) {
        super("Calendar event with id " + eventId + " not found");
    }

}
