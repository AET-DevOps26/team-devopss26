package de.tum.devopss26.calendarservice.service;

import org.openapitools.model.*;

/**
 * Every mutating or read operation is scoped to a specific user: callers must supply
 * a {@code userId} which is verified against the event's owner before any action is taken.
 */
public interface CalendarEventService {

    CreateCalendarEventResponse createEvent(CreateCalendarEventRequest request, long userId);

    ListCalendarEventResponse getEvents(long userId);

    GetCalendarEventResponse getEvent(long userId, long eventId);

    /**
     * Only non-null fields in the diff are applied; null fields are left unchanged.
     */
    UpdateCalendarEventResponse updateEvent(long userId, long eventId, CalendarEvent diff);

    void deleteEvent(long userId, long eventId);
}
