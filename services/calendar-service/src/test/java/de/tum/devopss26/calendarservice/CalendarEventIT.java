package de.tum.devopss26.calendarservice;

import de.tum.devopss26.calendarservice.entity.CalendarEvent;
import de.tum.devopss26.calendarservice.repository.CalendarEventRepository;
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
public class CalendarEventIT extends AbstractIntegrationTest {

    private MockMvc mockMvc;
    private CalendarEventRepository repository;

    @Autowired
    public void setMockMvc(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Autowired
    public void setRepository(CalendarEventRepository repository) {
        this.repository = repository;
    }

    @MockitoBean
    private TokenValidationInterceptor tokenValidationInterceptor;

    private static final long USER_A = 1L;
    private static final long USER_B = 2L;

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
    void testCreateEvent() throws Exception {
        mockUserAuthentication(USER_A);

        String requestJson = """
                {
                  "title": "Meeting",
                  "description": "Project sync",
                  "startTime": "2026-07-20T10:00:00Z",
                  "endTime": "2026-07-20T11:00:00Z",
                  "location": "TUM"
                }
                """;

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Meeting"))
                .andExpect(jsonPath("$.description").value("Project sync"))
                .andExpect(jsonPath("$.startTime").value("2026-07-20T10:00:00Z"))
                .andExpect(jsonPath("$.endTime").value("2026-07-20T11:00:00Z"))
                .andExpect(jsonPath("$.location").value("TUM"));

        List<CalendarEvent> events = repository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getUserId()).isEqualTo(USER_A);
    }

    @Test
    void testGetEvents() throws Exception {
        CalendarEvent eventA1 = new CalendarEvent();
        eventA1.setUserId(USER_A);
        eventA1.setTitle("User A Event 1");
        eventA1.setStartTime(OffsetDateTime.parse("2026-07-20T10:00:00Z"));
        eventA1.setEndTime(OffsetDateTime.parse("2026-07-20T11:00:00Z"));
        repository.save(eventA1);

        CalendarEvent eventA2 = new CalendarEvent();
        eventA2.setUserId(USER_A);
        eventA2.setTitle("User A Event 2");
        eventA2.setStartTime(OffsetDateTime.parse("2026-07-20T12:00:00Z"));
        eventA2.setEndTime(OffsetDateTime.parse("2026-07-20T13:00:00Z"));
        repository.save(eventA2);

        CalendarEvent eventB = new CalendarEvent();
        eventB.setUserId(USER_B);
        eventB.setTitle("User B Event");
        eventB.setStartTime(OffsetDateTime.parse("2026-07-20T14:00:00Z"));
        eventB.setEndTime(OffsetDateTime.parse("2026-07-20T15:00:00Z"));
        repository.save(eventB);

        mockUserAuthentication(USER_A);
        mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events", hasSize(2)))
                .andExpect(jsonPath("$.events[*].title").value(containsInAnyOrder("User A Event 1", "User A Event 2")));
    }

    @Test
    void testGetEventById() throws Exception {
        CalendarEvent event = new CalendarEvent();
        event.setUserId(USER_A);
        event.setTitle("Important Meeting");
        event.setStartTime(OffsetDateTime.parse("2026-07-20T10:00:00Z"));
        event.setEndTime(OffsetDateTime.parse("2026-07-20T11:00:00Z"));
        event = repository.save(event);

        mockUserAuthentication(USER_A);
        mockMvc.perform(get("/api/v1/events/" + event.getId())
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(event.getId()))
                .andExpect(jsonPath("$.title").value("Important Meeting"));

        mockUserAuthentication(USER_B);
        mockMvc.perform(get("/api/v1/events/" + event.getId())
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/events/99999")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateEvent() throws Exception {
        CalendarEvent event = new CalendarEvent();
        event.setUserId(USER_A);
        event.setTitle("Old Title");
        event.setStartTime(OffsetDateTime.parse("2026-07-20T10:00:00Z"));
        event.setEndTime(OffsetDateTime.parse("2026-07-20T11:00:00Z"));
        event = repository.save(event);

        mockUserAuthentication(USER_A);

        String updateJson = """
                {
                  "title": "New Title",
                  "location": "Munich"
                }
                """;

        mockMvc.perform(put("/api/v1/events/" + event.getId())
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(event.getId()))
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.location").value("Munich"));

        CalendarEvent updated = repository.findById(event.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("New Title");
        assertThat(updated.getLocation()).isEqualTo("Munich");

        mockUserAuthentication(USER_B);
        mockMvc.perform(put("/api/v1/events/" + event.getId())
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteEvent() throws Exception {
        CalendarEvent event = new CalendarEvent();
        event.setUserId(USER_A);
        event.setTitle("To Delete");
        event.setStartTime(OffsetDateTime.parse("2026-07-20T10:00:00Z"));
        event.setEndTime(OffsetDateTime.parse("2026-07-20T11:00:00Z"));
        event = repository.save(event);

        mockUserAuthentication(USER_B);
        mockMvc.perform(delete("/api/v1/events/" + event.getId())
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());

        mockUserAuthentication(USER_A);
        mockMvc.perform(delete("/api/v1/events/" + event.getId())
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNoContent());

        assertThat(repository.existsById(event.getId())).isFalse();
    }
}
