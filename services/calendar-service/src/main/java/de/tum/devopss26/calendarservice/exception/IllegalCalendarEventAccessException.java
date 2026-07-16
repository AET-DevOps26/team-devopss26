package de.tum.devopss26.calendarservice.exception;

import de.tum.devopss26.shared.exception.ForbiddenException;

/**
 * Exception thrown when a user tries to access a calendar event they do not own.
 */
public class IllegalCalendarEventAccessException extends ForbiddenException {

	/**
	 * Constructs a new exception for the illegal access attempt.
	 *
	 * @param accessorId the ID of the user who attempted the illegal access
	 * @param pair       the pair containing the event owner's ID and the event ID
	 */
	public IllegalCalendarEventAccessException(long accessorId, IllegalAccessPair pair) {
		super(accessorId + " tried to illegally access calendar event " + pair.eventId + " of owner " + pair.ownerId);
	}

	/**
	 * Record holding the owner ID and event ID of the illegally accessed event.
	 */
	public record IllegalAccessPair(long ownerId, long eventId) {
	}

}
