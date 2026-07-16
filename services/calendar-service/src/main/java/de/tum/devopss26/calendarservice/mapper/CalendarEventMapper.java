package de.tum.devopss26.calendarservice.mapper;

import de.tum.devopss26.calendarservice.entity.CalendarEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapitools.model.*;

import java.util.List;
import java.util.Objects;

/**
 * MapStruct mapper for converting between {@link CalendarEvent} entities
 * and various API DTOs.
 */
@Mapper(componentModel = "spring")
public interface CalendarEventMapper {

    /**
     * Maps a {@link CalendarEvent} entity to an {@link IdentifiedCalendarEvent} DTO.
     *
     * @param event the calendar event entity
     * @return the identified calendar event DTO
     */
    IdentifiedCalendarEvent toIdentified(CalendarEvent event);

    /**
     * Maps a create request and a user ID to a {@link CalendarEvent} entity.
     *
     * @param request the create request DTO
     * @param userId  the ID of the owning user
     * @return the calendar event entity
     */
    @Mapping(target = "userId", source = "userId")
    CalendarEvent toCalendarEvent(CreateCalendarEventRequest request, long userId);

    /**
     * Maps a {@link CalendarEvent} entity to a {@link CreateCalendarEventResponse} DTO.
     *
     * @param event the calendar event entity
     * @return the create response DTO
     */
    CreateCalendarEventResponse toCreateResponse(CalendarEvent event);

    /**
     * Maps a {@link CalendarEvent} entity to a {@link GetCalendarEventResponse} DTO.
     *
     * @param event the calendar event entity
     * @return the get response DTO
     */
    GetCalendarEventResponse toGetResponse(CalendarEvent event);

    /**
     * Maps a list of {@link IdentifiedCalendarEvent} DTOs to a {@link ListCalendarEventResponse} DTO.
     *
     * @param events the list of identified calendar events
     * @return the list response DTO
     */
    default ListCalendarEventResponse toListResponse(List<IdentifiedCalendarEvent> events) {
        return new ListCalendarEventResponse().events(Objects.requireNonNullElseGet(events, List::of));
    }

    /**
     * Maps a {@link CalendarEvent} entity to an {@link UpdateCalendarEventResponse} DTO.
     *
     * @param event the calendar event entity
     * @return the update response DTO
     */
    UpdateCalendarEventResponse toUpdateResponse(CalendarEvent event);

}
