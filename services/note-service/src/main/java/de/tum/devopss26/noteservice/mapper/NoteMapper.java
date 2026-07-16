package de.tum.devopss26.noteservice.mapper;

import de.tum.devopss26.noteservice.entity.Note;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapitools.model.*;

import java.util.List;
import java.util.Objects;

/**
 * MapStruct mapper for converting between {@link Note} entities and OpenAPI DTOs.
 * <p>
 * Provides mapping methods for all note-related API responses and requests.
 * Uses MapStruct's component model to integrate with Spring's dependency injection.
 * </p>
 */
@Mapper(componentModel = "spring")
public interface NoteMapper {

    // General mappers

    /**
     * Maps a {@link Note} entity to an {@link IdentifiedTimestampedNote} DTO.
     *
     * @param note the note entity to map
     * @return the mapped DTO with ID and timestamps
     */
    IdentifiedTimestampedNote toIdentifiedTimestamped(Note note);

    // Specific mappers

    /**
     * Maps a {@link CreateNoteRequest} to a {@link Note} entity, setting the owner.
     *
     * @param request the creation request containing title and content
     * @param userId  the ID of the note owner
     * @return the mapped note entity (without generated ID or timestamps)
     */
    @Mapping(target = "userId", source = "userId")
    Note toNote(CreateNoteRequest request, long userId);

    /**
     * Maps a {@link Note} entity to a {@link CreateNoteResponse}.
     *
     * @param note the saved note entity with generated ID and timestamps
     * @return the creation response
     */
    CreateNoteResponse toCreateResponse(Note note);

    /**
     * Maps a {@link Note} entity to a {@link GetNoteResponse}.
     *
     * @param note the note entity to map
     * @return the get-note response
     */
    GetNoteResponse toGetResponse(Note note);

    /**
     * Maps a list of {@link IdentifiedTimestampedNote} DTOs to a {@link ListNotesResponse}.
     * <p>
     * If the provided list is {@code null}, an empty list is used as fallback.
     * </p>
     *
     * @param notes the list of identified notes, may be {@code null}
     * @return the list-notes response containing the notes
     */
    default ListNotesResponse toListResponse(List<IdentifiedTimestampedNote> notes) {
        return new ListNotesResponse().notes(Objects.requireNonNullElseGet(notes, List::of));
    }

    /**
     * Maps a {@link Note} entity to an {@link UpdateNoteResponse}.
     *
     * @param note the updated note entity
     * @return the update response
     */
    UpdateNoteResponse toUpdateResponse(Note note);

}
