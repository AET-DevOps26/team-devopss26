package de.tum.devopss26.noteservice.service;

import org.openapitools.model.CreateNoteRequest;
import org.openapitools.model.CreateNoteResponse;
import org.openapitools.model.GetNoteResponse;
import org.openapitools.model.ListNotesResponse;
import org.openapitools.model.UpdateNoteResponse;

/**
 * Service interface defining business operations for note management.
 * <p>
 * Provides methods for creating, reading, updating, and deleting notes,
 * with user-based access control enforced at the implementation level.
 * Each operation requires a {@code userId} to ensure that users can only
 * access their own notes.
 * </p>
 */
public interface NoteService {

    /**
     * Creates a new note for the specified user.
     *
     * @param request the request containing the note title and content
     * @param userId  the ID of the user creating the note
     * @return the created note with generated ID and timestamps
     */
    CreateNoteResponse createNote(CreateNoteRequest request, long userId);

    /**
     * Retrieves all notes belonging to the specified user.
     *
     * @param userId the ID of the user whose notes to retrieve
     * @return a response containing the list of notes
     */
    ListNotesResponse getNotes(long userId);

    /**
     * Retrieves a single note by ID, verifying that it belongs to the user.
     *
     * @param userId the ID of the user requesting the note
     * @param id     the ID of the note to retrieve
     * @return the requested note
     * @throws de.tum.devopss26.noteservice.exception.NoteNotFoundException     if no note with the given ID exists
     * @throws de.tum.devopss26.noteservice.exception.IllegalNoteAccessException if the note does not belong to the user
     */
    GetNoteResponse getNote(long userId, long id);

    /**
     * Updates the title and/or content of an existing note.
     *
     * @param userId the ID of the user requesting the update
     * @param id     the ID of the note to update
     * @param diff   the note data containing fields to update (title, content, or both)
     * @return the updated note
     * @throws de.tum.devopss26.noteservice.exception.NoteNotFoundException     if no note with the given ID exists
     * @throws de.tum.devopss26.noteservice.exception.IllegalNoteAccessException if the note does not belong to the user
     */
    UpdateNoteResponse updateNote(long userId, long id, org.openapitools.model.Note diff);

    /**
     * Deletes a note by ID after verifying ownership.
     *
     * @param userId the ID of the user requesting the deletion
     * @param id     the ID of the note to delete
     * @throws de.tum.devopss26.noteservice.exception.NoteNotFoundException     if no note with the given ID exists
     * @throws de.tum.devopss26.noteservice.exception.IllegalNoteAccessException if the note does not belong to the user
     */
    void deleteNote(long userId, long id);
}
