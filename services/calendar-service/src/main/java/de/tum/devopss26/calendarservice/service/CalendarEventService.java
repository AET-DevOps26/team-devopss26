package de.tum.devopss26.calendarservice.service;

import org.openapitools.model.*;

/**
 * Every mutating or read operation is scoped to a specific user: callers must supply
 * a {@code userId} which is verified against the event's owner before any action is taken.
 */
public interface CalendarEventService {

    /**
     * Creates a new calendar event for the given user.
     *
     * @param request the event creation payload
     * @param userId  the ID of the user who will own the event
     * @return the created event response
     */
    CreateCalendarEventResponse createEvent(CreateCalendarEventRequest request, long userId);

    /**
     * Retrieves all events belonging to the given user.
     *
     * @param userId the ID of the user whose events to retrieve
     * @return a list response containing all events owned by the user
     */
    ListCalendarEventResponse getEvents(long userId);

    /**
     * Retrieves a single event by ID after verifying the user owns it.
     *
     * @param userId  the ID of the authenticated user
     * @param eventId the ID of the event to retrieve
     * @return the event response
     * @throws de.tum.devopss26.calendarservice.exception.CalendarEventNotFoundException      if no event exists with the given ID
     * @throws de.tum.devopss26.calendarservice.exception.IllegalCalendarEventAccessException if the event belongs to a different user
     */
    GetCalendarEventResponse getEvent(long userId, long eventId);

    /**
     * Only non-null fields in the diff are applied; null fields are left unchanged.
     *
     * @param userId  the ID of the authenticated user
     * @param eventId the ID of the event to update
     * @param diff    the patch containing only the fields to change (null fields are ignored)
     * @return the updated event response
     * @throws de.tum.devopss26.calendarservice.exception.CalendarEventNotFoundException      if no event exists with the given ID
     * @throws de.tum.devopss26.calendarservice.exception.IllegalCalendarEventAccessException if the event belongs to a different user
     */
    UpdateCalendarEventResponse updateEvent(long userId, long eventId, CalendarEvent diff);

    /**
     * Deletes an event by ID after verifying the user owns it.
     *
     * @param userId  the ID of the authenticated user
     * @param eventId the ID of the event to delete
     * @throws de.tum.devopss26.calendarservice.exception.CalendarEventNotFoundException      if no event exists with the given ID
     * @throws de.tum.devopss26.calendarservice.exception.IllegalCalendarEventAccessException if the event belongs to a different user
     */
    void deleteEvent(long userId, long eventId);
}
