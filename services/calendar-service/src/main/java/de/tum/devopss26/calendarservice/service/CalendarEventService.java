package de.tum.devopss26.calendarservice.service;

import org.openapitools.model.*;

/**
 * Service interface defining the business logic for calendar event management.
 */
public interface CalendarEventService {

    /**
     * Creates a new calendar event for the given user.
     *
     * @param request the create request containing event details
     * @param userId  the ID of the user who owns the event
     * @return the create response containing the created event data
     */
    CreateCalendarEventResponse createEvent(CreateCalendarEventRequest request, long userId);

    /**
     * Retrieves all calendar events for the given user.
     *
     * @param userId the ID of the user
     * @return the list response containing all events of the user
     */
    ListCalendarEventResponse getEvents(long userId);

    /**
     * Retrieves a single calendar event by its ID, verifying ownership.
     *
     * @param userId  the ID of the user requesting the event
     * @param eventId the ID of the calendar event
     * @return the get response containing the event data
     * @throws de.tum.devopss26.calendarservice.exception.CalendarEventNotFoundException if the event does not exist
     * @throws de.tum.devopss26.calendarservice.exception.IllegalCalendarEventAccessException if the user does not own the event
     */
    GetCalendarEventResponse getEvent(long userId, long eventId);

    /**
     * Updates an existing calendar event after verifying ownership.
     * Only non-null fields in the diff are applied.
     *
     * @param userId  the ID of the user requesting the update
     * @param eventId the ID of the calendar event to update
     * @param diff    the DTO containing the fields to update
     * @return the update response containing the updated event data
     * @throws de.tum.devopss26.calendarservice.exception.CalendarEventNotFoundException if the event does not exist
     * @throws de.tum.devopss26.calendarservice.exception.IllegalCalendarEventAccessException if the user does not own the event
     */
    UpdateCalendarEventResponse updateEvent(long userId, long eventId, CalendarEvent diff);

    /**
     * Deletes a calendar event after verifying ownership.
     *
     * @param userId  the ID of the user requesting the deletion
     * @param eventId the ID of the calendar event to delete
     * @throws de.tum.devopss26.calendarservice.exception.CalendarEventNotFoundException if the event does not exist
     * @throws de.tum.devopss26.calendarservice.exception.IllegalCalendarEventAccessException if the user does not own the event
     */
    void deleteEvent(long userId, long eventId);
}
