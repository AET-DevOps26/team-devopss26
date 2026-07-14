package de.tum.devopss26.checklistservice;

import de.tum.devopss26.checklistservice.entity.ChecklistEntity;
import de.tum.devopss26.checklistservice.entity.ChecklistItemEntity;
import de.tum.devopss26.checklistservice.repository.ChecklistItemRepository;
import de.tum.devopss26.checklistservice.repository.ChecklistRepository;
import de.tum.devopss26.shared.it.AbstractIntegrationTest;
import de.tum.devopss26.shared.security.TokenValidationInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
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
public class ChecklistIT extends AbstractIntegrationTest {

    private MockMvc mockMvc;
    private ChecklistRepository checklistRepository;
    private ChecklistItemRepository checklistItemRepository;

    @Autowired
    public void setMockMvc(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Autowired
    public void setChecklistRepository(ChecklistRepository checklistRepository) {
        this.checklistRepository = checklistRepository;
    }

    @Autowired
    public void setChecklistItemRepository(ChecklistItemRepository checklistItemRepository) {
        this.checklistItemRepository = checklistItemRepository;
    }

    @MockitoBean
    private TokenValidationInterceptor tokenValidationInterceptor;

    private void mockUserAuthentication(long userId) throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletRequest request = invocation.getArgument(0);
            request.setAttribute("userId", String.valueOf(userId));
            request.setAttribute("jwtClaims", null);
            return true;
        }).when(tokenValidationInterceptor).preHandle(any(), any(), any());
    }

    @BeforeEach
    void setUp() throws Exception {
        mockUserAuthentication(42L);
        checklistItemRepository.deleteAll();
        checklistRepository.deleteAll();
    }

    @Test
    void testCreateChecklist() throws Exception {
        String requestJson = """
                {
                  "userId": 42,
                  "title": "Groceries"
                }
                """;

        mockMvc.perform(post("/api/v1/checklists")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.title").value("Groceries"));

        List<ChecklistEntity> checklists = checklistRepository.findAll();
        assertThat(checklists).hasSize(1);
        assertThat(checklists.getFirst().getUserId()).isEqualTo(42L);
        assertThat(checklists.getFirst().getTitle()).isEqualTo("Groceries");
    }

    @Test
    void testGetChecklists() throws Exception {
        ChecklistEntity c1 = new ChecklistEntity();
        c1.setUserId(42L);
        c1.setTitle("Work");
        c1.setCreatedAt(LocalDateTime.now());
        checklistRepository.save(c1);

        ChecklistEntity c2 = new ChecklistEntity();
        c2.setUserId(42L);
        c2.setTitle("Shopping");
        c2.setCreatedAt(LocalDateTime.now());
        checklistRepository.save(c2);

        ChecklistEntity c3 = new ChecklistEntity();
        c3.setUserId(99L);
        c3.setTitle("Other User");
        c3.setCreatedAt(LocalDateTime.now());
        checklistRepository.save(c3);

        mockMvc.perform(get("/api/v1/checklists").param("userId", "42")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checklists", hasSize(2)))
                .andExpect(jsonPath("$.checklists[*].title").value(containsInAnyOrder("Work", "Shopping")));
    }

    @Test
    void testGetChecklistById() throws Exception {
        ChecklistEntity c = new ChecklistEntity();
        c.setUserId(42L);
        c.setTitle("Todo");
        c.setCreatedAt(LocalDateTime.now());
        c = checklistRepository.save(c);

        mockMvc.perform(get("/api/v1/checklists/" + c.getId())
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(c.getId()))
                .andExpect(jsonPath("$.title").value("Todo"));

        mockMvc.perform(get("/api/v1/checklists/99999")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateChecklist() throws Exception {
        ChecklistEntity c = new ChecklistEntity();
        c.setUserId(42L);
        c.setTitle("Old Title");
        c.setCreatedAt(LocalDateTime.now());
        c = checklistRepository.save(c);

        String updateJson = """
                {
                  "title": "New Title"
                }
                """;

        mockMvc.perform(put("/api/v1/checklists/" + c.getId())
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(c.getId()))
                .andExpect(jsonPath("$.title").value("New Title"));

        ChecklistEntity updated = checklistRepository.findById(c.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("New Title");
    }

    @Test
    void testDeleteChecklist() throws Exception {
        ChecklistEntity c = new ChecklistEntity();
        c.setUserId(42L);
        c.setTitle("ToDelete");
        c.setCreatedAt(LocalDateTime.now());
        c = checklistRepository.save(c);

        mockMvc.perform(delete("/api/v1/checklists/" + c.getId())
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNoContent());

        assertThat(checklistRepository.existsById(c.getId())).isFalse();

        mockMvc.perform(delete("/api/v1/checklists/99999")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAddChecklistItem() throws Exception {
        ChecklistEntity c = new ChecklistEntity();
        c.setUserId(42L);
        c.setTitle("My Checklist");
        c.setCreatedAt(LocalDateTime.now());
        c = checklistRepository.save(c);

        String itemJson = """
                {
                  "text": "Task 1",
                  "completed": false,
                  "position": 1
                }
                """;

        mockMvc.perform(post("/api/v1/checklists/" + c.getId() + "/items")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.text").value("Task 1"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.position").value(1));

        List<ChecklistItemEntity> items = checklistItemRepository.findAll();
        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getText()).isEqualTo("Task 1");
        assertThat(items.getFirst().getChecklist().getId()).isEqualTo(c.getId());
    }

    @Test
    void testUpdateChecklistItem() throws Exception {
        ChecklistEntity c = new ChecklistEntity();
        c.setUserId(42L);
        c.setTitle("My Checklist");
        c.setCreatedAt(LocalDateTime.now());
        c = checklistRepository.save(c);

        ChecklistItemEntity item = new ChecklistItemEntity();
        item.setChecklist(c);
        item.setText("Old Item Text");
        item.setCompleted(false);
        item.setPosition(1);
        item = checklistItemRepository.save(item);

        String updateJson = """
                {
                  "text": "New Item Text",
                  "completed": true,
                  "position": 2
                }
                """;

        mockMvc.perform(put("/api/v1/checklists/" + c.getId() + "/items/" + item.getId())
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(item.getId()))
                .andExpect(jsonPath("$.text").value("New Item Text"))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.position").value(2));

        ChecklistItemEntity updated = checklistItemRepository.findById(item.getId()).orElseThrow();
        assertThat(updated.getText()).isEqualTo("New Item Text");
        assertThat(updated.isCompleted()).isTrue();
        assertThat(updated.getPosition()).isEqualTo(2);
    }

    @Test
    void testDeleteChecklistItem() throws Exception {
        ChecklistEntity c = new ChecklistEntity();
        c.setUserId(42L);
        c.setTitle("My Checklist");
        c.setCreatedAt(LocalDateTime.now());
        c = checklistRepository.save(c);

        ChecklistItemEntity item = new ChecklistItemEntity();
        item.setChecklist(c);
        item.setText("Item to delete");
        item.setCompleted(false);
        item.setPosition(1);
        item = checklistItemRepository.save(item);

        mockMvc.perform(delete("/api/v1/checklists/" + c.getId() + "/items/" + item.getId())
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNoContent());

        assertThat(checklistItemRepository.existsById(item.getId())).isFalse();
    }
}
