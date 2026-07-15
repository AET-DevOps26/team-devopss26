package de.tum.devopss26.calendarservice.mapper;

import de.tum.devopss26.calendarservice.entity.CalendarEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapitools.model.*;

import java.util.List;
import java.util.Objects;

/** MapStruct mapper using {@code componentModel = "spring"}. */
@Mapper(componentModel = "spring")
public interface CalendarEventMapper {

    IdentifiedCalendarEvent toIdentified(CalendarEvent event);

    @Mapping(target = "userId", source = "userId")
    CalendarEvent toCalendarEvent(CreateCalendarEventRequest request, long userId);

    CreateCalendarEventResponse toCreateResponse(CalendarEvent event);

    GetCalendarEventResponse toGetResponse(CalendarEvent event);

    /** Safely handles a {@code null} input by returning an empty list. */
    default ListCalendarEventResponse toListResponse(List<IdentifiedCalendarEvent> events) {
        return new ListCalendarEventResponse().events(Objects.requireNonNullElseGet(events, List::of));
    }

    UpdateCalendarEventResponse toUpdateResponse(CalendarEvent event);

}
