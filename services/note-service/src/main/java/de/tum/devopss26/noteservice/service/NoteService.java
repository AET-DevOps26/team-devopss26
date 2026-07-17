package de.tum.devopss26.noteservice.service;

import org.openapitools.model.CreateNoteRequest;
import org.openapitools.model.CreateNoteResponse;
import org.openapitools.model.GetNoteResponse;
import org.openapitools.model.ListNotesResponse;
import org.openapitools.model.UpdateNoteResponse;

/**
 * Every operation is scoped to a {@code userId} to ensure
 * that users can only access their own notes.
 */
public interface NoteService {

    /**
     * Creates a new note for the given user, assigning current timestamps.
     *
     * @param request the request containing title and content
     * @param userId  the ID of the authenticated user who owns the note
     * @return the created note with assigned id and timestamps
     */
    CreateNoteResponse createNote(CreateNoteRequest request, long userId);

    /**
     * Retrieves all notes belonging to the given user.
     *
     * @param userId the ID of the authenticated user
     * @return a list of all notes owned by the user
     */
    ListNotesResponse getNotes(long userId);

    /**
     * Retrieves a single note by its ID after verifying ownership.
     *
     * @param userId the ID of the authenticated user
     * @param id     the ID of the note to retrieve
     * @return the note with the given ID
     * @throws de.tum.devopss26.noteservice.exception.NoteNotFoundException     if no note with the given ID exists
     * @throws de.tum.devopss26.noteservice.exception.IllegalNoteAccessException if the note does not belong to the user
     */
    GetNoteResponse getNote(long userId, long id);

    /**
     * Only non-null fields in {@code diff} are applied. Ownership is verified before the update.
     *
     * @param userId the ID of the authenticated user
     * @param id     the ID of the note to update
     * @param diff   the note containing only the fields to update (null fields are ignored)
     * @return the updated note with the new {@code lastUpdatedAt} timestamp
     * @throws de.tum.devopss26.noteservice.exception.NoteNotFoundException     if no note with the given ID exists
     * @throws de.tum.devopss26.noteservice.exception.IllegalNoteAccessException if the note does not belong to the user
     */
    UpdateNoteResponse updateNote(long userId, long id, org.openapitools.model.Note diff);

    /**
     * Deletes a note after verifying ownership.
     *
     * @param userId the ID of the authenticated user
     * @param id     the ID of the note to delete
     * @throws de.tum.devopss26.noteservice.exception.NoteNotFoundException     if no note with the given ID exists
     * @throws de.tum.devopss26.noteservice.exception.IllegalNoteAccessException if the note does not belong to the user
     */
    void deleteNote(long userId, long id);
}
