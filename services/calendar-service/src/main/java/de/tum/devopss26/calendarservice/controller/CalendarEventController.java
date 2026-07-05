package de.tum.devopss26.calendarservice.controller;

import de.tum.devopss26.calendarservice.service.CalendarEventService;
import de.tum.devopss26.shared.security.JWTHelper;
import de.tum.devopss26.shared.security.RequireTokenValidation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.CalendarEventsApi;
import org.openapitools.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CalendarEventController implements CalendarEventsApi {

	private final CalendarEventService service;
	private final HttpServletRequest servletRequest;

	@RequireTokenValidation
	@Override
	public ResponseEntity<CreateCalendarEventResponse> createEvent(CreateCalendarEventRequest request) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

		CreateCalendarEventResponse response = service.createEvent(request, userId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@RequireTokenValidation
	@Override
	public ResponseEntity<ListCalendarEventResponse> getEvents() {
		long userId = JWTHelper.extractFrom(servletRequest).getUserId();

		ListCalendarEventResponse response = service.getEvents(userId);
		return ResponseEntity.ok(response);
	}

	@RequireTokenValidation
	@Override
	public ResponseEntity<GetCalendarEventResponse> getEventById(Long id) {
		long userId = JWTHelper.extractFrom(servletRequest).getUserId();

		GetCalendarEventResponse response = service.getEvent(userId, id);
		return ResponseEntity.ok(response);
	}

	@RequireTokenValidation
	@Override
	public ResponseEntity<UpdateCalendarEventResponse> updateEvent(Long id, CalendarEvent diff) {
		long userId = JWTHelper.extractFrom(servletRequest).getUserId();

		UpdateCalendarEventResponse response = service.updateEvent(userId, id, diff);
		return ResponseEntity.ok(response);
	}

	@RequireTokenValidation
	@Override
	public ResponseEntity<Void> deleteEvent(Long id) {
		long userId = JWTHelper.extractFrom(servletRequest).getUserId();

		service.deleteEvent(userId, id);
		return ResponseEntity.noContent().build();
	}
}
