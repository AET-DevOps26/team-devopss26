package de.tum.devopss26.calendarservice.repository;

import de.tum.devopss26.calendarservice.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link CalendarEvent} entities.
 */
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    /**
     * Finds all calendar events owned by the specified user.
     *
     * @param userId the ID of the user
     * @return list of calendar events belonging to the user
     */
    List<CalendarEvent> findAllByUserId(long userId);

}
