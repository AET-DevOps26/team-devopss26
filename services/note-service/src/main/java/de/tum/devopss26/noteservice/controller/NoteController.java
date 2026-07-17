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
 * Every endpoint requires a valid token (enforced via {@link RequireTokenValidation})
 * and extracts the authenticated user's ID from the JWT to scope operations.
 */
@RestController
@RequiredArgsConstructor
public class NoteController implements NotesApi {

    private final HttpServletRequest servletRequest;
    private final NoteService service;

    /**
     * Retrieves all notes for the authenticated user.
     *
     * @return {@code 200 OK} with a list of the user's notes
     */
    @RequireTokenValidation
    @Override
    public ResponseEntity<ListNotesResponse> getNotes() {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        ListNotesResponse response = service.getNotes(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Scoped to the authenticated user.
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
     * @param request the request containing title and content
     * @return {@code 201 Created} with the created note
     */
    @RequireTokenValidation
    @Override
    public ResponseEntity<CreateNoteResponse> createNote(CreateNoteRequest request) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        CreateNoteResponse response = service.createNote(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Only the provided fields ({@code title}, {@code content}) are applied; omitted fields remain unchanged.
     */
    @RequireTokenValidation
    @Override
    public ResponseEntity<UpdateNoteResponse> updateNote(Long id, Note note) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        UpdateNoteResponse response = service.updateNote(userId, id, note);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a note by its ID.
     *
     * @param id the ID of the note to delete
     * @return {@code 204 No Content} on success
     */
    @RequireTokenValidation
    @Override
    public ResponseEntity<Void> deleteNote(Long id) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

        service.deleteNote(userId, id);
        return ResponseEntity.noContent().build();
    }
}
