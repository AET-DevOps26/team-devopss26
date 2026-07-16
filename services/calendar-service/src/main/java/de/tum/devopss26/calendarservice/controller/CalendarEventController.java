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
 * Every endpoint is guarded by {@link RequireTokenValidation}, which ensures a valid
 * JWT token is present in the request. The authenticated user's ID is extracted
 * from the request attributes (populated by {@code TokenValidationInterceptor})
 * via {@link JWTHelper#extractFrom}.
 */
@RestController
@RequiredArgsConstructor
public class CalendarEventController implements CalendarEventsApi {

	private final CalendarEventService service;
	private final HttpServletRequest servletRequest;

	/**
	 * Creates a new calendar event for the authenticated user.
	 *
	 * @param request the event creation payload containing title, time range, etc.
	 * @return {@code 201 Created} with the created event details
	 */
	@RequireTokenValidation
	@Override
	public ResponseEntity<CreateCalendarEventResponse> createEvent(CreateCalendarEventRequest request) {
        long userId = JWTHelper.extractFrom(servletRequest).getUserId();

		CreateCalendarEventResponse response = service.createEvent(request, userId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Retrieves all calendar events belonging to the authenticated user.
	 *
	 * @return {@code 200 OK} with a list of the user's events
	 */
	@RequireTokenValidation
	@Override
	public ResponseEntity<ListCalendarEventResponse> getEvents() {
		long userId = JWTHelper.extractFrom(servletRequest).getUserId();

		ListCalendarEventResponse response = service.getEvents(userId);
		return ResponseEntity.ok(response);
	}

	/**
	 * Retrieves a specific calendar event by its ID.
	 * <p>The event must belong to the authenticated user.</p>
	 *
	 * @param id the ID of the event to retrieve
	 * @return {@code 200 OK} with the event details
	 */
	@RequireTokenValidation
	@Override
	public ResponseEntity<GetCalendarEventResponse> getEventById(Long id) {
		long userId = JWTHelper.extractFrom(servletRequest).getUserId();

		GetCalendarEventResponse response = service.getEvent(userId, id);
		return ResponseEntity.ok(response);
	}

	/**
	 * Applies a partial update to an existing calendar event.
	 * <p>Only non-null fields in the diff are applied. The event must belong to the authenticated user.</p>
	 *
	 * @param id   the ID of the event to update
	 * @param diff the patch containing only the fields to change (null fields are ignored)
	 * @return {@code 200 OK} with the updated event details
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
	 * <p>The event must belong to the authenticated user.</p>
	 *
	 * @param id the ID of the event to delete
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
