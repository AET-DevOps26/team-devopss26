package de.tum.devopss26.noteservice;

import de.tum.devopss26.noteservice.entity.Note;
import de.tum.devopss26.noteservice.repository.NoteRepository;
import de.tum.devopss26.shared.it.AbstractIntegrationTest;
import de.tum.devopss26.shared.security.TokenValidationInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class NoteIT extends AbstractIntegrationTest {

    private MockMvc mockMvc;
    private NoteRepository repository;

    @Autowired
    public void setMockMvc(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Autowired
    public void setRepository(NoteRepository repository) {
        this.repository = repository;
    }

    @MockitoBean
    private TokenValidationInterceptor tokenValidationInterceptor;

    private static final long USER_A = 100L;
    private static final long USER_B = 200L;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private void mockUserAuthentication(long userId) throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletRequest request = invocation.getArgument(0);
            request.setAttribute("userId", String.valueOf(userId));
            request.setAttribute("jwtClaims", null);
            return true;
        }).when(tokenValidationInterceptor).preHandle(any(), any(), any());
    }

    @Test
    void testCreateNote() throws Exception {
        mockUserAuthentication(USER_A);

        String requestJson = """
                {
                  "title": "Groceries List",
                  "content": "Milk, Butter, Eggs"
                }
                """;

        mockMvc.perform(post("/api/v1/notes")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Groceries List"))
                .andExpect(jsonPath("$.content").value("Milk, Butter, Eggs"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.lastUpdatedAt").exists());

        List<Note> notes = repository.findAll();
        assertThat(notes).hasSize(1);
        assertThat(notes.getFirst().getUserId()).isEqualTo(USER_A);
    }

    @Test
    void testGetNotes() throws Exception {
        Note noteA1 = new Note();
        noteA1.setUserId(USER_A);
        noteA1.setTitle("Note A1");
        noteA1.setContent("Content A1");
        noteA1.setCreatedAt(OffsetDateTime.now());
        noteA1.setLastUpdatedAt(OffsetDateTime.now());
        repository.save(noteA1);

        Note noteA2 = new Note();
        noteA2.setUserId(USER_A);
        noteA2.setTitle("Note A2");
        noteA2.setContent("Content A2");
        noteA2.setCreatedAt(OffsetDateTime.now());
        noteA2.setLastUpdatedAt(OffsetDateTime.now());
        repository.save(noteA2);

        Note noteB = new Note();
        noteB.setUserId(USER_B);
        noteB.setTitle("Note B");
        noteB.setContent("Content B");
        noteB.setCreatedAt(OffsetDateTime.now());
        noteB.setLastUpdatedAt(OffsetDateTime.now());
        repository.save(noteB);

        mockUserAuthentication(USER_A);
        mockMvc.perform(get("/api/v1/notes")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes", hasSize(2)))
                .andExpect(jsonPath("$.notes[*].title").value(containsInAnyOrder("Note A1", "Note A2")));
    }

    @Test
    void testGetNoteById() throws Exception {
        Note note = new Note();
        note.setUserId(USER_A);
        note.setTitle("Secret Key");
        note.setContent("12345");
        note.setCreatedAt(OffsetDateTime.now());
        note.setLastUpdatedAt(OffsetDateTime.now());
        note = repository.save(note);

        mockUserAuthentication(USER_A);
        mockMvc.perform(get("/api/v1/notes/" + note.getId())
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(note.getId()))
                .andExpect(jsonPath("$.title").value("Secret Key"))
                .andExpect(jsonPath("$.content").value("12345"));

        mockUserAuthentication(USER_B);
        mockMvc.perform(get("/api/v1/notes/" + note.getId())
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/notes/99999")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateNote() throws Exception {
        Note note = new Note();
        note.setUserId(USER_A);
        note.setTitle("Old Title");
        note.setContent("Old Content");
        note.setCreatedAt(OffsetDateTime.now());
        note.setLastUpdatedAt(OffsetDateTime.now());
        note = repository.save(note);

        mockUserAuthentication(USER_A);

        String updateJson = """
                {
                  "title": "New Title",
                  "content": "New Content"
                }
                """;

        mockMvc.perform(put("/api/v1/notes/" + note.getId())
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(note.getId()))
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.content").value("New Content"));

        Note updated = repository.findById(note.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("New Title");
        assertThat(updated.getContent()).isEqualTo("New Content");

        mockUserAuthentication(USER_B);
        mockMvc.perform(put("/api/v1/notes/" + note.getId())
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteNote() throws Exception {
        Note note = new Note();
        note.setUserId(USER_A);
        note.setTitle("To Delete");
        note.setContent("Content");
        note.setCreatedAt(OffsetDateTime.now());
        note.setLastUpdatedAt(OffsetDateTime.now());
        note = repository.save(note);

        mockUserAuthentication(USER_B);
        mockMvc.perform(delete("/api/v1/notes/" + note.getId())
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());

        mockUserAuthentication(USER_A);
        mockMvc.perform(delete("/api/v1/notes/" + note.getId())
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNoContent());

        assertThat(repository.existsById(note.getId())).isFalse();
    }
}
