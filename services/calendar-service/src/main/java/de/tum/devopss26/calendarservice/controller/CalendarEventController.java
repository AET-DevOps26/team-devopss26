package de.tum.devopss26.calendarservice.controller;

import org.openapitools.api.CalendarEventsApi;
import org.openapitools.model.CalendarEvent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestController
public class CalendarEventController implements CalendarEventsApi {

    @Override
    public ResponseEntity<List<CalendarEvent>> getEvents(Long userId) {
        CalendarEvent event1 = new CalendarEvent()
                .id(1L)
                .title("Car Service Appointment")
                .description("Oil change and brake inspection")
                .startTime(OffsetDateTime.of(2026, 6, 19, 10, 0, 0, 0, ZoneOffset.UTC))
                .endTime(OffsetDateTime.of(2026, 6, 19, 11, 0, 0, 0, ZoneOffset.UTC))
                .location("AutoShop Central");

        CalendarEvent event2 = new CalendarEvent()
                .id(2L)
                .title("Dentist Appointment")
                .description("Routine cleaning and checkup")
                .startTime(OffsetDateTime.of(2026, 6, 21, 14, 30, 0, 0, ZoneOffset.UTC))
                .endTime(OffsetDateTime.of(2026, 6, 21, 15, 30, 0, 0, ZoneOffset.UTC))
                .location("City Dental Clinic");

        CalendarEvent event3 = new CalendarEvent()
                .id(3L)
                .title("Grocery Run")
                .description("Weekly grocery shopping")
                .startTime(OffsetDateTime.of(2026, 6, 20, 9, 0, 0, 0, ZoneOffset.UTC))
                .endTime(OffsetDateTime.of(2026, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC))
                .location("Supermarket");

        return ResponseEntity.ok(List.of(event1, event2, event3));
    }

    @Override
    public ResponseEntity<CalendarEvent> getEventById(Long id) {
        CalendarEvent event = new CalendarEvent()
                .id(id)
                .title("Sample Event")
                .description("Sample description")
                .startTime(OffsetDateTime.now())
                .endTime(OffsetDateTime.now().plusHours(1))
                .location("Munich");
        return ResponseEntity.ok(event);
    }

    @Override
    public ResponseEntity<CalendarEvent> createEvent(CalendarEvent calendarEvent) {
        calendarEvent.setId(1L);
        return ResponseEntity.status(HttpStatus.CREATED).body(calendarEvent);
    }

    @Override
    public ResponseEntity<CalendarEvent> updateEvent(Long id, CalendarEvent calendarEvent) {
        calendarEvent.setId(id);
        return ResponseEntity.ok(calendarEvent);
    }

    @Override
    public ResponseEntity<Void> deleteEvent(Long id) {
        return ResponseEntity.noContent().build();
    }
}
