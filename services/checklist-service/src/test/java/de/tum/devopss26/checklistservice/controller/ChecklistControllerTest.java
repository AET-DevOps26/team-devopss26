package de.tum.devopss26.checklistservice.controller;

import de.tum.devopss26.checklistservice.exception.ChecklistItemNotFoundException;
import de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException;
import de.tum.devopss26.checklistservice.service.ChecklistService;
import org.junit.jupiter.api.Test;
import org.openapitools.model.Checklist;
import org.openapitools.model.ChecklistItem;
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

@WebMvcTest(ChecklistController.class)
@Import(de.tum.devopss26.shared.exception.GlobalExceptionHandler.class)
class ChecklistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChecklistService checklistService;

    @Test
    void getChecklists_OK() throws Exception {
        Checklist checklist = new Checklist();
        checklist.setId(1L);
        checklist.setUserId(1L);
        checklist.setTitle("Groceries");
        checklist.setCreatedAt(OffsetDateTime.now());
        checklist.setItems(List.of());

        when(checklistService.getChecklists(1L)).thenReturn(List.of(checklist));

        mockMvc.perform(get("/api/v1/checklists").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void getChecklistById_OK() throws Exception {
        Checklist checklist = new Checklist();
        checklist.setId(1L);
        checklist.setUserId(1L);
        checklist.setTitle("Groceries");
        checklist.setCreatedAt(OffsetDateTime.now());
        checklist.setItems(List.of());

        when(checklistService.getChecklistById(1L)).thenReturn(checklist);

        mockMvc.perform(get("/api/v1/checklists/1"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void getChecklistById_NOT_FOUND() throws Exception {
        when(checklistService.getChecklistById(99L)).thenThrow(new ChecklistNotFoundException(99L));

        mockMvc.perform(get("/api/v1/checklists/99"))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void createChecklist_CREATED() throws Exception {
        Checklist created = new Checklist();
        created.setId(1L);
        created.setUserId(1L);
        created.setTitle("Groceries");
        created.setCreatedAt(OffsetDateTime.now());
        created.setItems(List.of());

        when(checklistService.createChecklist(eq(1L), any(Checklist.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/checklists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "title": "Groceries"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void updateChecklist_OK() throws Exception {
        Checklist updated = new Checklist();
        updated.setId(1L);
        updated.setUserId(1L);
        updated.setTitle("Updated Title");
        updated.setCreatedAt(OffsetDateTime.now());
        updated.setItems(List.of());

        when(checklistService.updateChecklist(eq(1L), any(Checklist.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/checklists/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void updateChecklist_NOT_FOUND() throws Exception {
        when(checklistService.updateChecklist(eq(99L), any(Checklist.class)))
                .thenThrow(new ChecklistNotFoundException(99L));

        mockMvc.perform(put("/api/v1/checklists/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Title"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void deleteChecklist_NO_CONTENT() throws Exception {
        doNothing().when(checklistService).deleteChecklist(1L);

        mockMvc.perform(delete("/api/v1/checklists/1"))
                .andExpect(status().isNoContent())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void deleteChecklist_NOT_FOUND() throws Exception {
        doThrow(new ChecklistNotFoundException(99L)).when(checklistService).deleteChecklist(99L);

        mockMvc.perform(delete("/api/v1/checklists/99"))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void addChecklistItem_CREATED() throws Exception {
        ChecklistItem item = new ChecklistItem();
        item.setId(1L);
        item.setText("Milk");
        item.setCompleted(false);
        item.setPosition(1);

        when(checklistService.addChecklistItem(eq(1L), any(ChecklistItem.class))).thenReturn(item);

        mockMvc.perform(post("/api/v1/checklists/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Milk",
                                  "completed": false,
                                  "position": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void addChecklistItem_NOT_FOUND() throws Exception {
        when(checklistService.addChecklistItem(eq(99L), any(ChecklistItem.class)))
                .thenThrow(new ChecklistNotFoundException(99L));

        mockMvc.perform(post("/api/v1/checklists/99/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Milk",
                                  "completed": false,
                                  "position": 1
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void updateChecklistItem_OK() throws Exception {
        ChecklistItem item = new ChecklistItem();
        item.setId(1L);
        item.setText("Oat Milk");
        item.setCompleted(true);
        item.setPosition(1);

        when(checklistService.updateChecklistItem(eq(1L), eq(1L), any(ChecklistItem.class))).thenReturn(item);

        mockMvc.perform(put("/api/v1/checklists/1/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Oat Milk",
                                  "completed": true,
                                  "position": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void updateChecklistItem_NOT_FOUND() throws Exception {
        when(checklistService.updateChecklistItem(eq(1L), eq(99L), any(ChecklistItem.class)))
                .thenThrow(new ChecklistItemNotFoundException(99L));

        mockMvc.perform(put("/api/v1/checklists/1/items/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Oat Milk",
                                  "completed": true,
                                  "position": 1
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void deleteChecklistItem_NO_CONTENT() throws Exception {
        doNothing().when(checklistService).deleteChecklistItem(1L, 1L);

        mockMvc.perform(delete("/api/v1/checklists/1/items/1"))
                .andExpect(status().isNoContent())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void deleteChecklistItem_NOT_FOUND() throws Exception {
        doThrow(new ChecklistItemNotFoundException(99L)).when(checklistService).deleteChecklistItem(1L, 99L);

        mockMvc.perform(delete("/api/v1/checklists/1/items/99"))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }
}
