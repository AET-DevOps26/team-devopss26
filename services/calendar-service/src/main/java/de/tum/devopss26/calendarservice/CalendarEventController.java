package de.tum.devopss26.calendarservice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Calendar Events", description = "Operations for managing calendar events")
public class CalendarEventController {

    @GetMapping
    @Operation(summary = "Get all events for a user")
    public ResponseEntity<List<CalendarEvent>> getAllEvents(@RequestParam Long userId) {
        return ResponseEntity.ok(List.of(
                new CalendarEvent(1L, "Car Service Appointment", "Oil change and brake inspection",
                        LocalDateTime.of(2026, 6, 19, 10, 0), LocalDateTime.of(2026, 6, 19, 11, 0), "AutoShop Central"),
                new CalendarEvent(2L, "Dentist Appointment", "Routine cleaning and checkup",
                        LocalDateTime.of(2026, 6, 21, 14, 30), LocalDateTime.of(2026, 6, 21, 15, 30), "City Dental Clinic"),
                new CalendarEvent(3L, "Grocery Run", "Weekly grocery shopping",
                        LocalDateTime.of(2026, 6, 20, 9, 0), LocalDateTime.of(2026, 6, 20, 10, 0), "Supermarket")
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a calendar event by ID")
    public ResponseEntity<CalendarEvent> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(
                new CalendarEvent(id, "Sample Event", "Sample description",
                        LocalDateTime.now(), LocalDateTime.now().plusHours(1), "Munich")
        );
    }

    @PostMapping
    @Operation(summary = "Create a new calendar event")
    public ResponseEntity<CalendarEvent> createEvent(@RequestBody CalendarEvent event) {
        event.setId(1L);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a calendar event", description = "Updates title, description, startTime, endTime, and location")
    public ResponseEntity<CalendarEvent> updateEvent(@PathVariable Long id, @RequestBody CalendarEvent event) {
        event.setId(id);
        return ResponseEntity.ok(event);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a calendar event by ID")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        return ResponseEntity.noContent().build();
    }
}
