package de.tum.devopss26.calendarservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Each event is owned by exactly one user and enforces a non-null title with a required time range.
 *
 * <p><strong>Temporal fields:</strong> Both {@code startTime} and {@code endTime} are
 * stored as {@code TIMESTAMP WITH TIME ZONE} columns, using Java's
 * {@link OffsetDateTime} to preserve timezone information.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "calendar_event")
public class CalendarEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** The ID of the user who owns this event. Non-null. */
	@Column(name = "user_id", nullable = false)
	private Long userId;

	/** Event title. Required, max 255 characters. */
	@NotBlank
	@Size(max = 255)
	@Column(nullable = false)
	private String title;

	/** Optional detailed description. Stored as TEXT for unlimited length. */
	@Column(columnDefinition = "TEXT")
	private String description;

	/** Event start time. Required, stored with timezone. */
	@Column(name = "start_time", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private OffsetDateTime startTime;

	/** Event end time. Required, stored with timezone. */
	@Column(name = "end_time", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private OffsetDateTime endTime;

	/** Optional physical location (venue name, address, etc.). */
	private String location;

}
