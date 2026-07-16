package de.tum.devopss26.calendarservice.exception;

import de.tum.devopss26.shared.exception.ForbiddenException;

/**
 * Thrown when a user attempts to access a calendar event they do not own.
 */
public class IllegalCalendarEventAccessException extends ForbiddenException {

	/**
	 * Creates a new exception describing the unauthorized access attempt.
	 *
	 * @param accessorId the ID of the user who tried to access the event
	 * @param pair       a record containing the event's actual owner ID and the event ID
	 */
	public IllegalCalendarEventAccessException(long accessorId, IllegalAccessPair pair) {
		super(accessorId + " tried to illegally access calendar event " + pair.eventId + " of owner " + pair.ownerId);
	}

	/**
	 * A record pairing the owner of an event with the event's ID.
	 *
	 * @param ownerId the ID of the user who owns the event
	 * @param eventId the ID of the event
	 */
	public record IllegalAccessPair(long ownerId, long eventId) {
	}

}
