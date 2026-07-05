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

@Service
@RequiredArgsConstructor
class CalendarEventServiceImpl implements CalendarEventService {

    private final CalendarEventRepository repository;
    private final CalendarEventMapper mapper;

    @Override
    public CreateCalendarEventResponse createEvent(CreateCalendarEventRequest request, long userId) {
        CalendarEvent event = mapper.toCalendarEvent(request, userId);
        event = repository.save(event);
        return mapper.toCreateResponse(event);
    }

    @Override
    public ListCalendarEventResponse getEvents(long userId) {
        List<CalendarEvent> eventEntities = repository.findAllByUserId(userId);
        List<CalendarEvent> illegalAccesses = eventEntities.stream()
                .filter(entity -> entity.getUserId() != userId)
                .toList();
        if (!illegalAccesses.isEmpty()) {
            List<IllegalAccessPair> pairs = illegalAccesses.stream()
                    .map(entity -> new IllegalAccessPair(entity.getUserId(), entity.getId()))
                    .toList();
            throw new IllegalCalendarEventAccessException(userId, pairs);
        }

        List<IdentifiedCalendarEvent> events = eventEntities
                .stream().map(mapper::toIdentified)
                .toList();

        return mapper.toListResponse(events);
    }

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

    @Override
    public GetCalendarEventResponse getEvent(long userId, long eventId) {
        CalendarEvent event = getEventEntity(userId, eventId);

        return mapper.toGetResponse(event);
    }

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

    @Override
    public void deleteEvent(long userId, long eventId) {
        CalendarEvent event = getEventEntity(userId, eventId);

        repository.delete(event);
    }

}
