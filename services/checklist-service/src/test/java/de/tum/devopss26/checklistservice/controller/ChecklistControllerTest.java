package de.tum.devopss26.checklistservice.controller;

import de.tum.devopss26.checklistservice.exception.ChecklistItemNotFoundException;
import de.tum.devopss26.checklistservice.exception.ChecklistNotFoundException;
import de.tum.devopss26.checklistservice.service.ChecklistService;
import de.tum.devopss26.shared.security.SecurityAutoConfiguration;
import de.tum.devopss26.shared.security.TokenValidationInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.model.Checklist;
import org.openapitools.model.ChecklistItem;
import org.openapitools.model.IdentifiedChecklist;
import org.openapitools.model.IdentifiedChecklistItem;
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
@Import(SecurityAutoConfiguration.class)
class ChecklistControllerTest {

    private MockMvc mockMvc;

    @MockitoBean
    private ChecklistService checklistService;

    /**
     * The TokenValidationInterceptor makes an external HTTP call to fetch the
     * public key. We mock it so that in tests it simply sets the userId attribute
     * and passes through, without any real network traffic.
     */
    @MockitoBean
    private TokenValidationInterceptor tokenValidationInterceptor;

    private static final String VALID_AUTH_HEADER = "Bearer test-token";
    private static final long USER_ID = 1L;

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

    @Test
    void getChecklists_OK() throws Exception {
        IdentifiedChecklist checklist = new IdentifiedChecklist();
        checklist.setId(1L);
        checklist.setUserId(USER_ID);
        checklist.setTitle("Groceries");
        checklist.setCreatedAt(OffsetDateTime.now());
        checklist.setItems(List.of());

        when(checklistService.getChecklists(USER_ID)).thenReturn(List.of(checklist));

        mockMvc.perform(get("/api/v1/checklists")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void getChecklists_UNAUTHORIZED_invalidToken() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or invalid Authorization header");
            return false;
        }).when(tokenValidationInterceptor).preHandle(any(), any(), any());

        mockMvc.perform(get("/api/v1/checklists")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void getChecklistById_OK() throws Exception {
        IdentifiedChecklist checklist = new IdentifiedChecklist();
        checklist.setId(1L);
        checklist.setUserId(USER_ID);
        checklist.setTitle("Groceries");
        checklist.setCreatedAt(OffsetDateTime.now());
        checklist.setItems(List.of());

        when(checklistService.getChecklistById(USER_ID, 1L)).thenReturn(checklist);

        mockMvc.perform(get("/api/v1/checklists/1")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void getChecklistById_NOT_FOUND() throws Exception {
        when(checklistService.getChecklistById(USER_ID, 99L)).thenThrow(new ChecklistNotFoundException(99L));

        mockMvc.perform(get("/api/v1/checklists/99")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void createChecklist_CREATED() throws Exception {
        IdentifiedChecklist created = new IdentifiedChecklist();
        created.setId(1L);
        created.setUserId(USER_ID);
        created.setTitle("Groceries");
        created.setCreatedAt(OffsetDateTime.now());
        created.setItems(List.of());

        when(checklistService.createChecklist(eq(USER_ID), any(Checklist.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/checklists")
                        .header("Authorization", VALID_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Groceries"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void createChecklist_UNAUTHORIZED_invalidToken() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or invalid Authorization header");
            return false;
        }).when(tokenValidationInterceptor).preHandle(any(), any(), any());

        mockMvc.perform(post("/api/v1/checklists")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Groceries"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void updateChecklist_OK() throws Exception {
        IdentifiedChecklist updated = new IdentifiedChecklist();
        updated.setId(1L);
        updated.setUserId(USER_ID);
        updated.setTitle("Updated Title");
        updated.setCreatedAt(OffsetDateTime.now());
        updated.setItems(List.of());

        when(checklistService.updateChecklist(eq(USER_ID), eq(1L), any(Checklist.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/checklists/1")
                        .header("Authorization", VALID_AUTH_HEADER)
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
        when(checklistService.updateChecklist(eq(USER_ID), eq(99L), any(Checklist.class)))
                .thenThrow(new ChecklistNotFoundException(99L));

        mockMvc.perform(put("/api/v1/checklists/99")
                        .header("Authorization", VALID_AUTH_HEADER)
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
        doNothing().when(checklistService).deleteChecklist(USER_ID, 1L);

        mockMvc.perform(delete("/api/v1/checklists/1")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isNoContent())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void deleteChecklist_NOT_FOUND() throws Exception {
        doThrow(new ChecklistNotFoundException(99L)).when(checklistService).deleteChecklist(USER_ID, 99L);

        mockMvc.perform(delete("/api/v1/checklists/99")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void deleteChecklist_UNAUTHORIZED_invalidToken() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or invalid Authorization header");
            return false;
        }).when(tokenValidationInterceptor).preHandle(any(), any(), any());

        mockMvc.perform(delete("/api/v1/checklists/1")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void addChecklistItem_CREATED() throws Exception {
        IdentifiedChecklistItem item = new IdentifiedChecklistItem();
        item.setId(1L);
        item.setText("Milk");
        item.setCompleted(false);
        item.setPosition(1);

        when(checklistService.addChecklistItem(eq(USER_ID), eq(1L), any(ChecklistItem.class))).thenReturn(item);

        mockMvc.perform(post("/api/v1/checklists/1/items")
                        .header("Authorization", VALID_AUTH_HEADER)
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
        when(checklistService.addChecklistItem(eq(USER_ID), eq(99L), any(ChecklistItem.class)))
                .thenThrow(new ChecklistNotFoundException(99L));

        mockMvc.perform(post("/api/v1/checklists/99/items")
                        .header("Authorization", VALID_AUTH_HEADER)
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
        IdentifiedChecklistItem item = new IdentifiedChecklistItem();
        item.setId(1L);
        item.setText("Oat Milk");
        item.setCompleted(true);
        item.setPosition(1);

        when(checklistService.updateChecklistItem(eq(USER_ID), eq(1L), eq(1L), any(ChecklistItem.class))).thenReturn(item);

        mockMvc.perform(put("/api/v1/checklists/1/items/1")
                        .header("Authorization", VALID_AUTH_HEADER)
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
        when(checklistService.updateChecklistItem(eq(USER_ID), eq(1L), eq(99L), any(ChecklistItem.class)))
                .thenThrow(new ChecklistItemNotFoundException(99L));

        mockMvc.perform(put("/api/v1/checklists/1/items/99")
                        .header("Authorization", VALID_AUTH_HEADER)
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
        doNothing().when(checklistService).deleteChecklistItem(USER_ID, 1L, 1L);

        mockMvc.perform(delete("/api/v1/checklists/1/items/1")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isNoContent())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void deleteChecklistItem_NOT_FOUND() throws Exception {
        doThrow(new ChecklistItemNotFoundException(99L)).when(checklistService).deleteChecklistItem(USER_ID, 1L, 99L);

        mockMvc.perform(delete("/api/v1/checklists/1/items/99")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid("checklist-service.yaml"));
    }

    @Test
    void createChecklist_BAD_REQUEST_malformedBody() throws Exception {
        mockMvc.perform(post("/api/v1/checklists")
                        .header("Authorization", VALID_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getChecklistById_BAD_REQUEST_nonNumericId() throws Exception {
        mockMvc.perform(get("/api/v1/checklists/not-a-number")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateChecklist_BAD_REQUEST_nonNumericId() throws Exception {
        mockMvc.perform(put("/api/v1/checklists/not-a-number")
                        .header("Authorization", VALID_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated Title"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateChecklist_BAD_REQUEST_malformedBody() throws Exception {
        mockMvc.perform(put("/api/v1/checklists/1")
                        .header("Authorization", VALID_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteChecklist_BAD_REQUEST_nonNumericId() throws Exception {
        mockMvc.perform(delete("/api/v1/checklists/not-a-number")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addChecklistItem_BAD_REQUEST_nonNumericId() throws Exception {
        mockMvc.perform(post("/api/v1/checklists/not-a-number/items")
                        .header("Authorization", VALID_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Milk",
                                  "completed": false,
                                  "position": 1
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addChecklistItem_BAD_REQUEST_malformedBody() throws Exception {
        mockMvc.perform(post("/api/v1/checklists/1/items")
                        .header("Authorization", VALID_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateChecklistItem_BAD_REQUEST_nonNumericItemId() throws Exception {
        mockMvc.perform(put("/api/v1/checklists/1/items/not-a-number")
                        .header("Authorization", VALID_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Oat Milk",
                                  "completed": true,
                                  "position": 1
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateChecklistItem_BAD_REQUEST_malformedBody() throws Exception {
        mockMvc.perform(put("/api/v1/checklists/1/items/1")
                        .header("Authorization", VALID_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteChecklistItem_BAD_REQUEST_nonNumericItemId() throws Exception {
        mockMvc.perform(delete("/api/v1/checklists/1/items/not-a-number")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isBadRequest());
    }
}
