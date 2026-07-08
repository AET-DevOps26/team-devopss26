package de.tum.devopss26.calendarservice.controller;

import de.tum.devopss26.calendarservice.exception.CalendarEventNotFoundException;
import de.tum.devopss26.calendarservice.service.CalendarEventService;
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
@WebMvcTest(CalendarEventController.class)
@Import({SecurityAutoConfiguration.class, GlobalExceptionHandler.class})
class CalendarEventControllerTest {

    private MockMvc mockMvc;

    @MockitoBean
    private CalendarEventService calendarEventService;

    /**
     * The TokenValidationInterceptor makes an external HTTP call to fetch the
     * public key. We mock it so that in tests it simply sets the userId attribute
     * and passes through, without any real network traffic.
     */
    @MockitoBean
    private TokenValidationInterceptor tokenValidationInterceptor;

    /**
     * Convenience JWT bearer token used in all requests that require authentication.
     */
    private static final String VALID_AUTH_HEADER = "Bearer test-token";
    private static final long USER_ID = 42L;

    @BeforeEach
    void setUpInterceptor() throws Exception {
        // Simulate a valid token: set the userId attribute and return true so the
        // request is forwarded to the controller.
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
    // POST /api/v1/events  –  createEvent
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void createEvent_CREATED() throws Exception {
        CreateCalendarEventResponse response = new CreateCalendarEventResponse();
        response.setId(1L);
        response.setTitle("Team Meeting");
        response.setDescription("Weekly sync");
        response.setStartTime(OffsetDateTime.parse("2025-08-01T10:00:00Z"));
        response.setEndTime(OffsetDateTime.parse("2025-08-01T11:00:00Z"));
        response.setLocation("Room 42");
        when(calendarEventService.createEvent(any(CreateCalendarEventRequest.class), eq(USER_ID)))
                .thenReturn(response);

        String body = """
                {
                  "title": "Team Meeting",
                  "description": "Weekly sync",
                  "startTime": "2025-08-01T10:00:00Z",
                  "endTime": "2025-08-01T11:00:00Z",
                  "location": "Room 42"
                }
                """;

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", VALID_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(openApi().isValid("calendar-service.yaml"));
    }

    @Test
    void createEvent_UNAUTHORIZED_invalidToken() throws Exception {
        // Interceptor rejects the (present but invalid) token and returns 401.
        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or invalid Authorization header");
            return false;
        }).when(tokenValidationInterceptor).preHandle(any(), any(), any());

        String body = """
                {
                  "title": "Team Meeting",
                  "description": "Weekly sync",
                  "startTime": "2025-08-01T10:00:00Z",
                  "endTime": "2025-08-01T11:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid("calendar-service.yaml"));
    }

    @Test
    void createEvent_INTERNAL_SERVER_ERROR() throws Exception {
        when(calendarEventService.createEvent(any(CreateCalendarEventRequest.class), eq(USER_ID)))
                .thenThrow(new RuntimeException("forced error"));

        String body = """
                {
                  "title": "Team Meeting",
                  "description": "Weekly sync",
                  "startTime": "2025-08-01T10:00:00Z",
                  "endTime": "2025-08-01T11:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", VALID_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(openApi().isValid("calendar-service.yaml"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/events  –  getEvents
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getEvents_OK() throws Exception {
        IdentifiedCalendarEvent event = new IdentifiedCalendarEvent();
        event.setId(1L);
        event.setTitle("Team Meeting");
        event.setDescription("Weekly sync");
        event.setStartTime(OffsetDateTime.parse("2025-08-01T10:00:00Z"));
        event.setEndTime(OffsetDateTime.parse("2025-08-01T11:00:00Z"));
        event.setLocation("Room 42");

        ListCalendarEventResponse response = new ListCalendarEventResponse();
        response.setEvents(List.of(event));
        when(calendarEventService.getEvents(USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("calendar-service.yaml"));
    }

    @Test
    void getEvents_UNAUTHORIZED_invalidToken() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or invalid Authorization header");
            return false;
        }).when(tokenValidationInterceptor).preHandle(any(), any(), any());

        mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid("calendar-service.yaml"));
    }

    @Test
    void getEvents_INTERNAL_SERVER_ERROR() throws Exception {
        when(calendarEventService.getEvents(USER_ID))
                .thenThrow(new RuntimeException("forced error"));

        mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isInternalServerError())
                .andExpect(openApi().isValid("calendar-service.yaml"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/events/{id}  –  getEventById
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getEventById_OK() throws Exception {
        GetCalendarEventResponse response = new GetCalendarEventResponse();
        response.setId(1L);
        response.setTitle("Team Meeting");
        response.setDescription("Weekly sync");
        response.setStartTime(OffsetDateTime.parse("2025-08-01T10:00:00Z"));
        response.setEndTime(OffsetDateTime.parse("2025-08-01T11:00:00Z"));
        response.setLocation("Room 42");
        when(calendarEventService.getEvent(USER_ID, 1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/events/1")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("calendar-service.yaml"));
    }

    @Test
    void getEventById_NOT_FOUND() throws Exception {
        when(calendarEventService.getEvent(USER_ID, 99L))
                .thenThrow(new CalendarEventNotFoundException(99L));

        mockMvc.perform(get("/api/v1/events/99")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid("calendar-service.yaml"));
    }

    @Test
    void getEventById_UNAUTHORIZED_invalidToken() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or invalid Authorization header");
            return false;
        }).when(tokenValidationInterceptor).preHandle(any(), any(), any());

        mockMvc.perform(get("/api/v1/events/1")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid("calendar-service.yaml"));
    }

    @Test
    void getEventById_INTERNAL_SERVER_ERROR() throws Exception {
        when(calendarEventService.getEvent(USER_ID, 1L))
                .thenThrow(new RuntimeException("forced error"));

        mockMvc.perform(get("/api/v1/events/1")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isInternalServerError())
                .andExpect(openApi().isValid("calendar-service.yaml"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/events/{id}  –  updateEvent
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void updateEvent_OK() throws Exception {
        UpdateCalendarEventResponse response = new UpdateCalendarEventResponse();
        response.setId(1L);
        response.setTitle("Updated Meeting");
        response.setDescription("Weekly sync");
        response.setStartTime(OffsetDateTime.parse("2025-08-01T10:00:00Z"));
        response.setEndTime(OffsetDateTime.parse("2025-08-01T11:00:00Z"));
        response.setLocation("Room 42");
        when(calendarEventService.updateEvent(eq(USER_ID), eq(1L), any(CalendarEvent.class)))
                .thenReturn(response);

        String body = """
                {
                  "title": "Updated Meeting"
                }
                """;

        mockMvc.perform(put("/api/v1/events/1")
                        .header("Authorization", VALID_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("calendar-service.yaml"));
    }

    @Test
    void updateEvent_NOT_FOUND() throws Exception {
        when(calendarEventService.updateEvent(eq(USER_ID), eq(99L), any(CalendarEvent.class)))
                .thenThrow(new CalendarEventNotFoundException(99L));

        String body = """
                {
                  "title": "Updated Meeting"
                }
                """;

        mockMvc.perform(put("/api/v1/events/99")
                        .header("Authorization", VALID_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid("calendar-service.yaml"));
    }

    @Test
    void updateEvent_UNAUTHORIZED_invalidToken() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or invalid Authorization header");
            return false;
        }).when(tokenValidationInterceptor).preHandle(any(), any(), any());

        String body = """
                {
                  "title": "Updated Meeting"
                }
                """;

        mockMvc.perform(put("/api/v1/events/1")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid("calendar-service.yaml"));
    }

    @Test
    void updateEvent_INTERNAL_SERVER_ERROR() throws Exception {
        when(calendarEventService.updateEvent(eq(USER_ID), eq(1L), any(CalendarEvent.class)))
                .thenThrow(new RuntimeException("forced error"));

        String body = """
                {
                  "title": "Updated Meeting"
                }
                """;

        mockMvc.perform(put("/api/v1/events/1")
                        .header("Authorization", VALID_AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(openApi().isValid("calendar-service.yaml"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/v1/events/{id}  –  deleteEvent
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void deleteEvent_NO_CONTENT() throws Exception {
        doNothing().when(calendarEventService).deleteEvent(USER_ID, 1L);

        mockMvc.perform(delete("/api/v1/events/1")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isNoContent())
                .andExpect(openApi().isValid("calendar-service.yaml"));
    }

    @Test
    void deleteEvent_NOT_FOUND() throws Exception {
        doThrow(new CalendarEventNotFoundException(99L))
                .when(calendarEventService).deleteEvent(USER_ID, 99L);

        mockMvc.perform(delete("/api/v1/events/99")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid("calendar-service.yaml"));
    }

    @Test
    void deleteEvent_UNAUTHORIZED_invalidToken() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or invalid Authorization header");
            return false;
        }).when(tokenValidationInterceptor).preHandle(any(), any(), any());

        mockMvc.perform(delete("/api/v1/events/1")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid("calendar-service.yaml"));
    }

    @Test
    void deleteEvent_INTERNAL_SERVER_ERROR() throws Exception {
        doThrow(new RuntimeException("forced error"))
                .when(calendarEventService).deleteEvent(USER_ID, 1L);

        mockMvc.perform(delete("/api/v1/events/1")
                        .header("Authorization", VALID_AUTH_HEADER))
                .andExpect(status().isInternalServerError())
                .andExpect(openApi().isValid("calendar-service.yaml"));
    }
}
