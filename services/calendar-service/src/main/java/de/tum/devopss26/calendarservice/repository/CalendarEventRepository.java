package de.tum.devopss26.calendarservice.repository;

import de.tum.devopss26.calendarservice.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for managing {@link CalendarEvent} entities.
 */
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    /**
     * Finds all calendar events owned by the specified user.
     *
     * @param userId the ID of the user whose events to find
     * @return a list of events belonging to the user, or an empty list if none exist
     */
    List<CalendarEvent> findAllByUserId(long userId);

}
