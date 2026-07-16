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
 * Every operation that targets a specific event uses {@link #getEventEntity} to
 * simultaneously assert existence and ownership, keeping access control consistent
 * across all service methods.
 */
@Service
@RequiredArgsConstructor
class CalendarEventServiceImpl implements CalendarEventService {

    private final CalendarEventRepository repository;
    private final CalendarEventMapper mapper;

    /**
     * Creates a new calendar event and persists it.
     *
     * @param request the event creation payload
     * @param userId  the ID of the authenticated user who will own the event
     * @return the created event response with assigned ID and timestamps
     */
    @Transactional
    @Override
    public CreateCalendarEventResponse createEvent(CreateCalendarEventRequest request, long userId) {
        CalendarEvent event = mapper.toCalendarEvent(request, userId);
        event = repository.save(event);
        return mapper.toCreateResponse(event);
    }

    /**
     * Retrieves all events belonging to the given user.
     * <p>No ownership check is needed here because the query filters by userId at the database level.</p>
     *
     * @param userId the ID of the user whose events to retrieve
     * @return a list response containing all events owned by the user
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
     * Existence and ownership are checked together so callers never need to handle
     * these concerns separately.
     *
     * @throws CalendarEventNotFoundException      if no event exists with the given ID
     * @throws IllegalCalendarEventAccessException if the event belongs to a different user
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
     * Retrieves a single event by ID after verifying the requesting user owns it.
     *
     * @param userId  the ID of the authenticated user
     * @param eventId the ID of the event to retrieve
     * @return the event response with full details
     * @throws CalendarEventNotFoundException      if no event exists with the given ID
     * @throws IllegalCalendarEventAccessException if the event belongs to a different user
     */
    @Transactional(readOnly = true)
    @Override
    public GetCalendarEventResponse getEvent(long userId, long eventId) {
        CalendarEvent event = getEventEntity(userId, eventId);

        return mapper.toGetResponse(event);
    }

    /**
     * Only non-null fields in {@code diff} are applied to the persisted entity; null
     * fields are left unchanged. This allows clients to send only the fields they
     * want to modify without first fetching the full current state.
     *
     * <p><strong>Why partial update?</strong> A full replacement would force every
     * client to reconstruct the entire event object, including fields they may not
     * have readily available. By treating null as "leave as-is", callers can issue
     * minimal patches (e.g., a single field change) and the service remains idempotent
     * for unchanged fields.
     *
     * @param userId  the ID of the authenticated user
     * @param eventId the ID of the event to update
     * @param diff    the patch containing only the fields to change (null fields are ignored)
     * @return the updated event response
     * @throws CalendarEventNotFoundException      if no event exists with the given ID
     * @throws IllegalCalendarEventAccessException if the event belongs to a different user
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
     * Deletes an event by ID after verifying the requesting user owns it.
     *
     * @param userId  the ID of the authenticated user
     * @param eventId the ID of the event to delete
     * @throws CalendarEventNotFoundException      if no event exists with the given ID
     * @throws IllegalCalendarEventAccessException if the event belongs to a different user
     */
    @Transactional
    @Override
    public void deleteEvent(long userId, long eventId) {
        CalendarEvent event = getEventEntity(userId, eventId);

        repository.delete(event);
    }

}
