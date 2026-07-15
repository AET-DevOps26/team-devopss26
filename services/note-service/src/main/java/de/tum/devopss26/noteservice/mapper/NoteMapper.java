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

    // General mappers

    IdentifiedTimestampedNote toIdentifiedTimestamped(Note note);

    // Specific mappers

    @Mapping(target = "userId", source = "userId")
    Note toNote(CreateNoteRequest request, long userId);

    CreateNoteResponse toCreateResponse(Note note);

    GetNoteResponse toGetResponse(Note note);

    /**
     * Returns an empty list when the input is {@code null}.
     */
    default ListNotesResponse toListResponse(List<IdentifiedTimestampedNote> notes) {
        return new ListNotesResponse().notes(Objects.requireNonNullElseGet(notes, List::of));
    }

    UpdateNoteResponse toUpdateResponse(Note note);

}
