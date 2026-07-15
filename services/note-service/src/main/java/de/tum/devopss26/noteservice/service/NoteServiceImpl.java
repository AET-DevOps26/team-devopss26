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

    @Transactional(readOnly = true)
    @Override
    public ListNotesResponse getNotes(long userId) {
        List<Note> noteEntities = repository.findAllByUserId(userId);

        List<IdentifiedTimestampedNote> notes = noteEntities
                .stream().map(mapper::toIdentifiedTimestamped)
                .toList();

        return mapper.toListResponse(notes);
    }

    /**
     * Throws {@link NoteNotFoundException} if the note does not exist,
     * or {@link IllegalNoteAccessException} if the note's owner does not
     * match the requesting user — preventing cross-user data access.
     */
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

    @Transactional(readOnly = true)
    @Override
    public GetNoteResponse getNote(long userId, long id) {
        Note note = getNoteEntity(userId, id);

        return mapper.toGetResponse(note);
    }

    /**
     * Non-null fields in {@code diff} overwrite existing values; null fields are left untouched.
     * Updates {@code lastUpdatedAt} to the current time. Ownership is verified before the update.
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

    @Transactional
    @Override
    public void deleteNote(long userId, long id) {
        Note note = getNoteEntity(userId, id);

        repository.delete(note);
    }
}
