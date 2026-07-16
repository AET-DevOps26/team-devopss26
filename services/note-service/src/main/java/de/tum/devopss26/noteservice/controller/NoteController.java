package de.tum.devopss26.noteservice.controller;

import de.tum.devopss26.noteservice.service.NoteService;
import de.tum.devopss26.shared.security.JWTHelper;
import de.tum.devopss26.shared.security.RequireTokenValidation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.NotesApi;
import org.openapitools.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing note management endpoints.
 * <p>
 * Implements the {@link NotesApi} interface generated from the OpenAPI specification.
 * All endpoints require a valid JWT token (enforced by {@code @RequireTokenValidation})
 * and extract the authenticated user's ID from the token for authorization.
 * </p>
 */
@RestController
@RequiredArgsConstructor
public class NoteController implements NotesApi {

    private final HttpServletRequest servletRequest;
    private final NoteService service;

    /**
     * Retrieves all notes for the authenticated user.
     *
     * @return a response containing the list of notes owned by the authenticated user
     */
    @RequireTokenValidation
    @Override
    public ResponseEntity<ListNotesResponse> getNotes() {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        ListNotesResponse response = service.getNotes(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single note by its ID for the authenticated user.
     *
     * @param id the ID of the note to retrieve
     * @return the requested note if it belongs to the authenticated user
     */
    @RequireTokenValidation
    @Override
    public ResponseEntity<GetNoteResponse> getNoteById(Long id) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        GetNoteResponse response = service.getNote(userId, id);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new note for the authenticated user.
     *
     * @param request the request containing the note title and content
     * @return the created note with HTTP 201 (Created) status
     */
    @RequireTokenValidation
    @Override
    public ResponseEntity<CreateNoteResponse> createNote(CreateNoteRequest request) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        CreateNoteResponse response = service.createNote(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing note for the authenticated user.
     *
     * @param id   the ID of the note to update
     * @param note the note data containing fields to update (title, content, or both)
     * @return the updated note
     */
    @RequireTokenValidation
    @Override
    public ResponseEntity<UpdateNoteResponse> updateNote(Long id, Note note) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        UpdateNoteResponse response = service.updateNote(userId, id, note);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a note by its ID for the authenticated user.
     *
     * @param id the ID of the note to delete
     * @return HTTP 204 (No Content) on successful deletion
     */
    @RequireTokenValidation
    @Override
    public ResponseEntity<Void> deleteNote(Long id) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        service.deleteNote(userId, id);
        return ResponseEntity.noContent().build();
    }
}
