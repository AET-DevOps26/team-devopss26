package de.tum.devopss26.noteservice.mapper;

import de.tum.devopss26.noteservice.entity.Note;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapitools.model.*;

import java.util.List;
import java.util.Objects;

/**
 * Conversion is name-based by default; the {@code toNote} mapping explicitly
 * copies {@code userId} from the method parameter into the entity.
 */
@Mapper(componentModel = "spring")
public interface NoteMapper {

    /**
     * Maps a {@link Note} entity to an {@link IdentifiedTimestampedNote} DTO.
     *
     * @param note the entity to map
     * @return the identified timestamped note DTO
     */
    IdentifiedTimestampedNote toIdentifiedTimestamped(Note note);

    /**
     * Maps a creation request to a {@link Note} entity, assigning ownership to the given user.
     *
     * @param request the creation payload
     * @param userId  the ID of the user who will own the note
     * @return the mapped entity ready for persistence
     */
    @Mapping(target = "userId", source = "userId")
    Note toNote(CreateNoteRequest request, long userId);

    /**
     * Maps a persisted {@link Note} to its creation response DTO.
     *
     * @param note the persisted entity
     * @return the creation response DTO
     */
    CreateNoteResponse toCreateResponse(Note note);

    /**
     * Maps a {@link Note} entity to its full-detail response DTO.
     *
     * @param note the entity to map
     * @return the full-detail response DTO
     */
    GetNoteResponse toGetResponse(Note note);

    /**
     * Returns an empty list when the input is {@code null}.
     *
     * @param notes the list of notes, may be {@code null}
     * @return a list response containing the notes, or an empty list if input was {@code null}
     */
    default ListNotesResponse toListResponse(List<IdentifiedTimestampedNote> notes) {
        return new ListNotesResponse().notes(Objects.requireNonNullElseGet(notes, List::of));
    }

    /**
     * Maps an updated {@link Note} entity to its update response DTO.
     *
     * @param note the updated entity
     * @return the update response DTO
     */
    UpdateNoteResponse toUpdateResponse(Note note);

}
