package de.tum.devopss26.calendarservice.service;

import de.tum.devopss26.calendarservice.entity.CalendarEvent;
import de.tum.devopss26.calendarservice.exception.CalendarEventNotFoundException;
import de.tum.devopss26.calendarservice.exception.IllegalCalendarEventAccessException;
import de.tum.devopss26.calendarservice.mapper.CalendarEventMapper;
import de.tum.devopss26.calendarservice.repository.CalendarEventRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.openapitools.model.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static de.tum.devopss26.calendarservice.exception.IllegalCalendarEventAccessException.IllegalAccessPair;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link CalendarEventService} providing the business logic
 * for creating, reading, updating, and deleting calendar events.
 * Enforces ownership checks on every access.
 */
@Service
@RequiredArgsConstructor
class CalendarEventServiceImpl implements CalendarEventService {

    private final CalendarEventRepository repository;
    private final CalendarEventMapper mapper;

    /**
     * Creates a new calendar event for the given user by mapping the request,
     * persisting the entity, and returning the response.
     *
     * @param request the create request containing event details
     * @param userId  the ID of the owning user
     * @return the create response with the persisted event data
     */
    @Transactional
    @Override
    public CreateCalendarEventResponse createEvent(CreateCalendarEventRequest request, long userId) {
        CalendarEvent event = mapper.toCalendarEvent(request, userId);
        event = repository.save(event);
        return mapper.toCreateResponse(event);
    }

    /**
     * Retrieves all calendar events owned by the given user.
     *
     * @param userId the ID of the user
     * @return the list response containing all events of the user
     */
    @Transactional(readOnly = true)
    @Override
    public ListCalendarEventResponse getEvents(long userId) {
        List<CalendarEvent> eventEntities = repository.findAllByUserId(userId);

        List<IdentifiedCalendarEvent> events = eventEntities
                .stream().map(mapper::toIdentified)
                .toList();

        return mapper.toListResponse(events);
    }

    /**
     * Retrieves a calendar event entity by its ID and verifies that the
     * requesting user is the owner. Used internally by {@link #getEvent},
     * {@link #updateEvent}, and {@link #deleteEvent}.
     *
     * @param userId  the ID of the requesting user
     * @param eventId the ID of the calendar event
     * @return the calendar event entity
     * @throws CalendarEventNotFoundException         if the event does not exist
     * @throws IllegalCalendarEventAccessException    if the user does not own the event
     */
    private @NonNull CalendarEvent getEventEntity(long userId, long eventId) {
        Optional<CalendarEvent> opt = repository.findById(eventId);
        if (opt.isEmpty()) {
            throw new CalendarEventNotFoundException(eventId);
        }

        CalendarEvent event = opt.get();
        if (event.getUserId() != userId) {
            throw new IllegalCalendarEventAccessException(userId,
                    new IllegalAccessPair(event.getUserId(), event.getId()));
        }
        return event;
    }

    /**
     * Retrieves a single calendar event by its ID after verifying ownership.
     *
     * @param userId  the ID of the requesting user
     * @param eventId the ID of the calendar event
     * @return the get response with the event data
     * @throws CalendarEventNotFoundException      if the event does not exist
     * @throws IllegalCalendarEventAccessException if the user does not own the event
     */
    @Transactional(readOnly = true)
    @Override
    public GetCalendarEventResponse getEvent(long userId, long eventId) {
        CalendarEvent event = getEventEntity(userId, eventId);

        return mapper.toGetResponse(event);
    }

    /**
     * Updates an existing calendar event. Only non-null fields in the diff DTO
     * are applied to the persisted entity. Ownership is verified before updating.
     *
     * @param userId  the ID of the requesting user
     * @param eventId the ID of the calendar event to update
     * @param diff    the DTO containing the fields to update
     * @return the update response with the updated event data
     * @throws CalendarEventNotFoundException      if the event does not exist
     * @throws IllegalCalendarEventAccessException if the user does not own the event
     */
    @Transactional
    @Override
    public UpdateCalendarEventResponse updateEvent(long userId, long eventId,
                                                   org.openapitools.model.CalendarEvent diff) {
        CalendarEvent event = getEventEntity(userId, eventId);

        if (diff.getTitle() == null && diff.getDescription() == null && diff.getStartTime() == null
                && diff.getEndTime() == null && diff.getLocation() == null) {
            return mapper.toUpdateResponse(event);
        }

        if (diff.getTitle() != null) {
            event.setTitle(diff.getTitle());
        }
        if (diff.getDescription() != null) {
            event.setDescription(diff.getDescription());
        }
        if (diff.getStartTime() != null) {
            event.setStartTime(diff.getStartTime());
        }
        if (diff.getEndTime() != null) {
            event.setEndTime(diff.getEndTime());
        }
        if (diff.getLocation() != null) {
            event.setLocation(diff.getLocation());
        }

        event = repository.save(event);

        return mapper.toUpdateResponse(event);
    }

    /**
     * Deletes a calendar event after verifying ownership.
     *
     * @param userId  the ID of the requesting user
     * @param eventId the ID of the calendar event to delete
     * @throws CalendarEventNotFoundException      if the event does not exist
     * @throws IllegalCalendarEventAccessException if the user does not own the event
     */
    @Transactional
    @Override
    public void deleteEvent(long userId, long eventId) {
        CalendarEvent event = getEventEntity(userId, eventId);

        repository.delete(event);
    }

}
