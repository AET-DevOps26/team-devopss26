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

/**
 * REST controller exposing endpoints for calendar event CRUD operations.
 * All endpoints require a valid JWT token for authentication.
 */
@RestController
@RequiredArgsConstructor
public class CalendarEventController implements CalendarEventsApi {

	private final CalendarEventService service;
	private final HttpServletRequest servletRequest;

	/**
	 * Creates a new calendar event for the authenticated user.
	 *
	 * @param request the create request containing event details
	 * @return {@code 201 Created} with the created event data
	 */
	@RequireTokenValidation
	@Override
	public ResponseEntity<CreateCalendarEventResponse> createEvent(CreateCalendarEventRequest request) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

		CreateCalendarEventResponse response = service.createEvent(request, userId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Retrieves all calendar events of the authenticated user.
	 *
	 * @return {@code 200 OK} with the list of events
	 */
	@RequireTokenValidation
	@Override
	public ResponseEntity<ListCalendarEventResponse> getEvents() {
		long userId = JWTHelper.extractFrom(servletRequest).getUserId();

		ListCalendarEventResponse response = service.getEvents(userId);
		return ResponseEntity.ok(response);
	}

	/**
	 * Retrieves a single calendar event by its ID.
	 *
	 * @param id the ID of the calendar event
	 * @return {@code 200 OK} with the event data
	 */
	@RequireTokenValidation
	@Override
	public ResponseEntity<GetCalendarEventResponse> getEventById(Long id) {
		long userId = JWTHelper.extractFrom(servletRequest).getUserId();

		GetCalendarEventResponse response = service.getEvent(userId, id);
		return ResponseEntity.ok(response);
	}

	/**
	 * Updates an existing calendar event. Only the fields provided in the diff
	 * are applied.
	 *
	 * @param id   the ID of the calendar event to update
	 * @param diff the DTO containing the fields to update
	 * @return {@code 200 OK} with the updated event data
	 */
	@RequireTokenValidation
	@Override
	public ResponseEntity<UpdateCalendarEventResponse> updateEvent(Long id, CalendarEvent diff) {
		long userId = JWTHelper.extractFrom(servletRequest).getUserId();

		UpdateCalendarEventResponse response = service.updateEvent(userId, id, diff);
		return ResponseEntity.ok(response);
	}

	/**
	 * Deletes a calendar event by its ID.
	 *
	 * @param id the ID of the calendar event to delete
	 * @return {@code 204 No Content} on successful deletion
	 */
	@RequireTokenValidation
	@Override
	public ResponseEntity<Void> deleteEvent(Long id) {
		long userId = JWTHelper.extractFrom(servletRequest).getUserId();

		service.deleteEvent(userId, id);
		return ResponseEntity.noContent().build();
	}
}
