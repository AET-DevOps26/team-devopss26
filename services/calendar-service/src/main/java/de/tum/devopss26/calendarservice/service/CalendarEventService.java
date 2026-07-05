package de.tum.devopss26.calendarservice.service;

import org.openapitools.model.*;

public interface CalendarEventService {

    CreateCalendarEventResponse createEvent(CreateCalendarEventRequest request, long userId);

    ListCalendarEventResponse getEvents(long userId);

    GetCalendarEventResponse getEvent(long userId, long eventId);

    UpdateCalendarEventResponse updateEvent(long userId, long eventId, CalendarEvent diff);

    void deleteEvent(long userId, long eventId);
}
