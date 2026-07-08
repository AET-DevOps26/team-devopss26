package de.tum.devopss26.noteservice.controller;

import de.tum.devopss26.noteservice.exception.IllegalNoteAccessException;
import de.tum.devopss26.noteservice.exception.NoteNotFoundException;
import de.tum.devopss26.noteservice.service.NoteService;
import de.tum.devopss26.shared.exception.GlobalExceptionHandler;
import de.tum.devopss26.shared.security.SecurityAutoConfiguration;
import de.tum.devopss26.shared.security.TokenValidationInterceptor;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
@WebMvcTest(NoteController.class)
@Import({SecurityAutoConfiguration.class, GlobalExceptionHandler.class})
class NoteControllerTest {

    private MockMvc mockMvc;

    @MockitoBean
    private NoteService noteService;

    @MockitoBean
    private TokenValidationInterceptor tokenValidationInterceptor;

    private static final String VALID_AUTH_HEADER = "Bearer test-token";
    private static final long USER_ID = 42L;

    @BeforeEach
    void setUpInterceptor() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletRequest request = invocation.getArgument(0);
            request.setAttribute("userId", String.valueOf(USER_ID));
            request.setAttribute("jwtClaims", null);
            return true;
        }).when(tokenValidationInterceptor).preHandle(any(), any(), any());
    }

    @Autowired
    public void setMockMvc(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/notes  –  createNote
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void createNote_CREATED() throws Exception {
        CreateNoteResponse response = new CreateNoteResponse();
        response.setId(1L);
        response.setTitle("Shopping List");
        response.setContent("Buy milk, bread, eggs");
        response.setCreatedAt(OffsetDateTime.parse("2026-07-09T00:00:00Z"));
        response.setLastUpdatedAt(OffsetDateTime.parse("2026-07-09T00:00:00Z"));

        when(noteService.createNote(any(CreateNoteRequest.class), eq(USER_ID)))
                .thenReturn(response);

        String body = """
                {
                  "title": "Shopping List",
                  "content": "Buy milk, bread, eggs"
                }
                """;

        mockMvc.perform(post("/api/v1/notes")
                        .header("Authorization", VALID_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(openApi().isValid("note-service.yaml"));
    }

    @Test
    void createNote_UNAUTHORIZED_invalidToken() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or invalid Authorization header");
            return false;
        }).when(tokenValidationInterceptor).preHandle(any(), any(), any());

        String body = """
                {
                  "title": "Shopping List",
                  "content": "Buy milk, bread, eggs"
                }
                """;

        mockMvc.perform(post("/api/v1/notes")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid("note-service.yaml"));
    }

    @Test
    void createNote_INTERNAL_SERVER_ERROR() throws Exception {
        when(noteService.createNote(any(CreateNoteRequest.class), eq(USER_ID)))
                .thenThrow(new RuntimeException("forced error"));

        String body = """
                {
                  "title": "Shopping List",
                  "content": "Buy milk, bread, eggs"
                }
                """;

        mockMvc.perform(post("/api/v1/notes")
                        .header("Authorization", VALID_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(openApi().isValid("note-service.yaml"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/notes  –  getNotes
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getNotes_OK() throws Exception {
        IdentifiedTimestampedNote note = new IdentifiedTimestampedNote();
        note.setId(1L);
        note.setTitle("Shopping List");
        note.setContent("Buy milk, bread, eggs");
        note.setCreatedAt(OffsetDateTime.parse("2026-07-09T00:00:00Z"));
        note.setLastUpdatedAt(OffsetDateTime.parse("2026-07-09T00:00:00Z"));

        ListNotesResponse response = new ListNotesResponse();
        response.setNotes(List.of(note));

        when(noteService.getNotes(USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/notes")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("note-service.yaml"));
    }

    @Test
    void getNotes_UNAUTHORIZED_invalidToken() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or invalid Authorization header");
            return false;
        }).when(tokenValidationInterceptor).preHandle(any(), any(), any());

        mockMvc.perform(get("/api/v1/notes")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid("note-service.yaml"));
    }

    @Test
    void getNotes_INTERNAL_SERVER_ERROR() throws Exception {
        when(noteService.getNotes(USER_ID))
                .thenThrow(new RuntimeException("forced error"));

        mockMvc.perform(get("/api/v1/notes")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isInternalServerError())
                .andExpect(openApi().isValid("note-service.yaml"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/notes/{id}  –  getNoteById
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getNoteById_OK() throws Exception {
        GetNoteResponse response = new GetNoteResponse();
        response.setId(1L);
        response.setTitle("Shopping List");
        response.setContent("Buy milk, bread, eggs");
        response.setCreatedAt(OffsetDateTime.parse("2026-07-09T00:00:00Z"));
        response.setLastUpdatedAt(OffsetDateTime.parse("2026-07-09T00:00:00Z"));

        when(noteService.getNote(USER_ID, 1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/notes/1")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("note-service.yaml"));
    }

    @Test
    void getNoteById_NOT_FOUND() throws Exception {
        when(noteService.getNote(USER_ID, 99L))
                .thenThrow(new NoteNotFoundException(99L));

        mockMvc.perform(get("/api/v1/notes/99")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid("note-service.yaml"));
    }

    @Test
    void getNoteById_FORBIDDEN() throws Exception {
        when(noteService.getNote(USER_ID, 1L))
                .thenThrow(new IllegalNoteAccessException(USER_ID,
                        new IllegalNoteAccessException.IllegalAccessPair(99L, 1L)));

        mockMvc.perform(get("/api/v1/notes/1")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isForbidden())
                .andExpect(openApi().isValid("note-service.yaml"));
    }

    @Test
    void getNoteById_UNAUTHORIZED_invalidToken() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or invalid Authorization header");
            return false;
        }).when(tokenValidationInterceptor).preHandle(any(), any(), any());

        mockMvc.perform(get("/api/v1/notes/1")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid("note-service.yaml"));
    }

    @Test
    void getNoteById_INTERNAL_SERVER_ERROR() throws Exception {
        when(noteService.getNote(USER_ID, 1L))
                .thenThrow(new RuntimeException("forced error"));

        mockMvc.perform(get("/api/v1/notes/1")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isInternalServerError())
                .andExpect(openApi().isValid("note-service.yaml"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/notes/{id}  –  updateNote
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void updateNote_OK() throws Exception {
        UpdateNoteResponse response = new UpdateNoteResponse();
        response.setId(1L);
        response.setTitle("Updated Shopping List");
        response.setContent("Buy milk, bread, eggs");
        response.setCreatedAt(OffsetDateTime.parse("2026-07-09T00:00:00Z"));
        response.setLastUpdatedAt(OffsetDateTime.parse("2026-07-09T01:00:00Z"));

        when(noteService.updateNote(eq(USER_ID), eq(1L), any(org.openapitools.model.Note.class)))
                .thenReturn(response);

        String body = """
                {
                  "title": "Updated Shopping List"
                }
                """;

        mockMvc.perform(put("/api/v1/notes/1")
                        .header("Authorization", VALID_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("note-service.yaml"));
    }

    @Test
    void updateNote_NOT_FOUND() throws Exception {
        when(noteService.updateNote(eq(USER_ID), eq(99L), any(org.openapitools.model.Note.class)))
                .thenThrow(new NoteNotFoundException(99L));

        String body = """
                {
                  "title": "Updated Shopping List"
                }
                """;

        mockMvc.perform(put("/api/v1/notes/99")
                        .header("Authorization", VALID_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid("note-service.yaml"));
    }

    @Test
    void updateNote_FORBIDDEN() throws Exception {
        when(noteService.updateNote(eq(USER_ID), eq(1L), any(org.openapitools.model.Note.class)))
                .thenThrow(new IllegalNoteAccessException(USER_ID,
                        new IllegalNoteAccessException.IllegalAccessPair(99L, 1L)));

        String body = """
                {
                  "title": "Updated Shopping List"
                }
                """;

        mockMvc.perform(put("/api/v1/notes/1")
                        .header("Authorization", VALID_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(openApi().isValid("note-service.yaml"));
    }

    @Test
    void updateNote_UNAUTHORIZED_invalidToken() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or invalid Authorization header");
            return false;
        }).when(tokenValidationInterceptor).preHandle(any(), any(), any());

        String body = """
                {
                  "title": "Updated Shopping List"
                }
                """;

        mockMvc.perform(put("/api/v1/notes/1")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid("note-service.yaml"));
    }

    @Test
    void updateNote_INTERNAL_SERVER_ERROR() throws Exception {
        when(noteService.updateNote(eq(USER_ID), eq(1L), any(org.openapitools.model.Note.class)))
                .thenThrow(new RuntimeException("forced error"));

        String body = """
                {
                  "title": "Updated Shopping List"
                }
                """;

        mockMvc.perform(put("/api/v1/notes/1")
                        .header("Authorization", VALID_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(openApi().isValid("note-service.yaml"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/v1/notes/{id}  –  deleteNote
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void deleteNote_NO_CONTENT() throws Exception {
        doNothing().when(noteService).deleteNote(USER_ID, 1L);

        mockMvc.perform(delete("/api/v1/notes/1")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isNoContent())
                .andExpect(openApi().isValid("note-service.yaml"));
    }

    @Test
    void deleteNote_NOT_FOUND() throws Exception {
        doThrow(new NoteNotFoundException(99L))
                .when(noteService).deleteNote(USER_ID, 99L);

        mockMvc.perform(delete("/api/v1/notes/99")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid("note-service.yaml"));
    }

    @Test
    void deleteNote_FORBIDDEN() throws Exception {
        doThrow(new IllegalNoteAccessException(USER_ID,
                new IllegalNoteAccessException.IllegalAccessPair(99L, 1L)))
                .when(noteService).deleteNote(USER_ID, 1L);

        mockMvc.perform(delete("/api/v1/notes/1")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isForbidden())
                .andExpect(openApi().isValid("note-service.yaml"));
    }

    @Test
    void deleteNote_UNAUTHORIZED_invalidToken() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or invalid Authorization header");
            return false;
        }).when(tokenValidationInterceptor).preHandle(any(), any(), any());

        mockMvc.perform(delete("/api/v1/notes/1")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid("note-service.yaml"));
    }

    @Test
    void deleteNote_INTERNAL_SERVER_ERROR() throws Exception {
        doThrow(new RuntimeException("forced error"))
                .when(noteService).deleteNote(USER_ID, 1L);

        mockMvc.perform(delete("/api/v1/notes/1")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isInternalServerError())
                .andExpect(openApi().isValid("note-service.yaml"));
    }
}
