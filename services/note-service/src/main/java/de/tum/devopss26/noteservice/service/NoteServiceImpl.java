package de.tum.devopss26.noteservice.service;

import de.tum.devopss26.noteservice.entity.Note;
import de.tum.devopss26.noteservice.exception.IllegalNoteAccessException;
import de.tum.devopss26.noteservice.exception.NoteNotFoundException;
import de.tum.devopss26.noteservice.mapper.NoteMapper;
import de.tum.devopss26.noteservice.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.openapitools.model.*;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static de.tum.devopss26.noteservice.exception.IllegalNoteAccessException.IllegalAccessPair;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every mutating or read operation checks that the requesting user owns the target note
 * via {@link #getNoteEntity(long, long)}. {@link Transactional @Transactional} is applied
 * to guarantee atomic writes and consistent read isolation across JPA operations.
 */
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final NoteRepository repository;
    private final NoteMapper mapper;

    /**
     * Assigns the current timestamp to both {@code createdAt} and {@code lastUpdatedAt}.
     *
     * @param request the request containing title and content for the new note
     * @param userId  the ID of the authenticated user who owns the note
     * @return the created note with assigned id and timestamps
     */
    @Transactional
    @Override
    public CreateNoteResponse createNote(CreateNoteRequest request, long userId) {
        Note note = mapper.toNote(request, userId);
        OffsetDateTime now = OffsetDateTime.now();
        note.setCreatedAt(now);
        note.setLastUpdatedAt(now);
        note = repository.save(note);
        return mapper.toCreateResponse(note);
    }

    /**
     * Retrieves all notes that belong to the given user.
     *
     * @param userId the ID of the authenticated user
     * @return a list of all notes owned by the user
     */
    @Transactional(readOnly = true)
    @Override
    public ListNotesResponse getNotes(long userId) {
        List<Note> noteEntities = repository.findAllByUserId(userId);

        List<IdentifiedTimestampedNote> notes = noteEntities
                .stream().map(mapper::toIdentifiedTimestamped)
                .toList();

        return mapper.toListResponse(notes);
    }

    private @NonNull Note getNoteEntity(long userId, long noteId) {
        Optional<Note> opt = repository.findById(noteId);
        if (opt.isEmpty()) {
            throw new NoteNotFoundException(noteId);
        }

        Note note = opt.get();
        if (note.getUserId() != userId) {
            throw new IllegalNoteAccessException(userId,
                    new IllegalAccessPair(note.getUserId(), note.getId()));
        }
        return note;
    }

    /**
     * Retrieves a single note by its ID after verifying ownership.
     *
     * @param userId the ID of the authenticated user
     * @param id     the ID of the note to retrieve
     * @return the note with the given ID
     * @throws NoteNotFoundException     if no note with the given ID exists
     * @throws IllegalNoteAccessException if the note does not belong to the user
     */
    @Transactional(readOnly = true)
    @Override
    public GetNoteResponse getNote(long userId, long id) {
        Note note = getNoteEntity(userId, id);

        return mapper.toGetResponse(note);
    }

    /**
     * Non-null fields in {@code diff} overwrite existing values; null fields are left untouched.
     * Updates {@code lastUpdatedAt} to the current time. Ownership is verified before the update.
     *
     * @param userId the ID of the authenticated user
     * @param id     the ID of the note to update
     * @param diff   the note containing only the fields to update (null fields are ignored)
     * @return the updated note with the new {@code lastUpdatedAt} timestamp
     * @throws NoteNotFoundException     if no note with the given ID exists
     * @throws IllegalNoteAccessException if the note does not belong to the user
     */
    @Transactional
    @Override
    public UpdateNoteResponse updateNote(long userId, long id, org.openapitools.model.Note diff) {
        Note note = getNoteEntity(userId, id);

        if (diff.getTitle() == null && diff.getContent() == null) {
            return mapper.toUpdateResponse(note);
        }

        if (diff.getTitle() != null) {
            note.setTitle(diff.getTitle());
        }
        if (diff.getContent() != null) {
            note.setContent(diff.getContent());
        }

        note.setLastUpdatedAt(OffsetDateTime.now());
        return mapper.toUpdateResponse(note);
    }

    /**
     * Deletes a note after verifying ownership.
     *
     * @param userId the ID of the authenticated user
     * @param id     the ID of the note to delete
     * @throws NoteNotFoundException     if no note with the given ID exists
     * @throws IllegalNoteAccessException if the note does not belong to the user
     */
    @Transactional
    @Override
    public void deleteNote(long userId, long id) {
        Note note = getNoteEntity(userId, id);

        repository.delete(note);
    }
}
