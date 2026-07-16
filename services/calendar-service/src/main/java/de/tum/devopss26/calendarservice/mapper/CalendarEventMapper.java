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

    /**
     * Maps a {@link CalendarEvent} entity to an {@link IdentifiedCalendarEvent} DTO.
     *
     * @param event the entity to map
     * @return the identified event DTO
     */
    IdentifiedCalendarEvent toIdentified(CalendarEvent event);

    /**
     * Maps a creation request to a {@link CalendarEvent} entity, assigning ownership to the given user.
     *
     * @param request the creation payload
     * @param userId  the ID of the user who will own the event
     * @return the mapped entity ready for persistence
     */
    @Mapping(target = "userId", source = "userId")
    CalendarEvent toCalendarEvent(CreateCalendarEventRequest request, long userId);

    /**
     * Maps a persisted {@link CalendarEvent} to its creation response DTO.
     *
     * @param event the persisted entity
     * @return the creation response DTO
     */
    CreateCalendarEventResponse toCreateResponse(CalendarEvent event);

    /**
     * Maps a {@link CalendarEvent} entity to its full-detail response DTO.
     *
     * @param event the entity to map
     * @return the full-detail response DTO
     */
    GetCalendarEventResponse toGetResponse(CalendarEvent event);

    /**
     * Safely handles a {@code null} input by returning an empty list.
     *
     * @param events the list of identified events, may be {@code null}
     * @return a list response containing the events, or an empty list if input was {@code null}
     */
    default ListCalendarEventResponse toListResponse(List<IdentifiedCalendarEvent> events) {
        return new ListCalendarEventResponse().events(Objects.requireNonNullElseGet(events, List::of));
    }

    /**
     * Maps an updated {@link CalendarEvent} entity to its update response DTO.
     *
     * @param event the updated entity
     * @return the update response DTO
     */
    UpdateCalendarEventResponse toUpdateResponse(CalendarEvent event);

}
