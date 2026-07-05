package de.tum.devopss26.calendarservice.controller;

import de.tum.devopss26.shared.security.RequireTokenValidation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.CalendarEventsApi;
import org.openapitools.model.CalendarEvent;
import org.openapitools.model.CreateCalendarEventRequest;
import org.openapitools.model.CreateCalendarEventResponse;
import org.openapitools.model.GetCalendarEventResponse;
import org.openapitools.model.ListCalendarEventResponse;
import org.openapitools.model.UpdateCalendarEventResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CalendarEventController implements CalendarEventsApi {

	private final HttpServletRequest request;

	@RequireTokenValidation
	@Override
	public ResponseEntity<ListCalendarEventResponse> getEvents() {
		return ResponseEntity.ok(new ListCalendarEventResponse());
	}

	@RequireTokenValidation
	@Override
	public ResponseEntity<GetCalendarEventResponse> getEventById(Long id) {
		return ResponseEntity.ok(new GetCalendarEventResponse());
	}

	@RequireTokenValidation
	@Override
	public ResponseEntity<CreateCalendarEventResponse> createEvent(CreateCalendarEventRequest calendarEvent) {
		return ResponseEntity.status(HttpStatus.CREATED).body(new CreateCalendarEventResponse());
	}

	@RequireTokenValidation
	@Override
	public ResponseEntity<UpdateCalendarEventResponse> updateEvent(Long id, CalendarEvent calendarEvent) {
		return ResponseEntity.ok(new UpdateCalendarEventResponse());
	}

	@RequireTokenValidation
	@Override
	public ResponseEntity<Void> deleteEvent(Long id) {
		return ResponseEntity.noContent().build();
	}
}
