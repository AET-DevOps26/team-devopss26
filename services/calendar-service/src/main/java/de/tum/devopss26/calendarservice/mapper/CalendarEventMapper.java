package de.tum.devopss26.calendarservice.mapper;

import de.tum.devopss26.calendarservice.entity.CalendarEvent;
import org.mapstruct.Mapper;
import org.openapitools.model.*;

import java.util.List;
import java.util.Objects;

@Mapper(componentModel = "spring")
public interface CalendarEventMapper {

    // General mappers

    IdentifiedCalendarEvent toIdentified(CalendarEvent event);

    // Specific mappers

    CalendarEvent toCalendarEvent(CreateCalendarEventRequest request, long userId);

    CreateCalendarEventResponse toCreateResponse(CalendarEvent event);

    GetCalendarEventResponse toGetResponse(CalendarEvent event);

    default ListCalendarEventResponse toListResponse(List<IdentifiedCalendarEvent> events) {
        return new ListCalendarEventResponse().events(Objects.requireNonNullElseGet(events, List::of));
    }

    UpdateCalendarEventResponse toUpdateResponse(CalendarEvent event);

}
