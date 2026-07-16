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
 * Implementation of {@link NoteService} providing note CRUD operations.
 * <p>
 * Handles persistence via {@link NoteRepository}, maps between entities and DTOs
 * using {@link NoteMapper}, and enforces user-based access control on all operations.
 * All public methods are transactional and verify that the requesting user owns the
 * targeted note before performing any operation.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final NoteRepository repository;
    private final NoteMapper mapper;

    /**
     * Creates a new note with the current timestamp and persists it.
     *
     * @param request the request containing the note title and content
     * @param userId  the ID of the note owner
     * @return the created note response with generated ID and timestamps
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
     * Retrieves all notes owned by the specified user.
     *
     * @param userId the ID of the user whose notes to retrieve
     * @return a response containing the list of notes
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
     * Retrieves a single note by ID after verifying that it belongs to the requesting user.
     *
     * @param userId the ID of the requesting user
     * @param id     the ID of the note to retrieve
     * @return the requested note
     * @throws NoteNotFoundException     if no note exists with the given ID
     * @throws IllegalNoteAccessException if the note does not belong to the user
     */
    @Transactional(readOnly = true)
    @Override
    public GetNoteResponse getNote(long userId, long id) {
        Note note = getNoteEntity(userId, id);

        return mapper.toGetResponse(note);
    }

    /**
     * Updates the title and/or content of an existing note.
     * <p>
     * Only the fields provided in the diff (non-null) are updated.
     * The {@code lastUpdatedAt} timestamp is refreshed on every update.
     * </p>
     *
     * @param userId the ID of the requesting user
     * @param id     the ID of the note to update
     * @param diff   the note data containing the fields to update (title, content, or both)
     * @return the updated note
     * @throws NoteNotFoundException     if no note exists with the given ID
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
        note = repository.save(note);

        return mapper.toUpdateResponse(note);
    }

    /**
     * Deletes a note by ID after verifying ownership.
     *
     * @param userId the ID of the requesting user
     * @param id     the ID of the note to delete
     * @throws NoteNotFoundException     if no note exists with the given ID
     * @throws IllegalNoteAccessException if the note does not belong to the user
     */
    @Transactional
    @Override
    public void deleteNote(long userId, long id) {
        Note note = getNoteEntity(userId, id);

        repository.delete(note);
    }
}
