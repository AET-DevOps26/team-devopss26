package de.tum.devopss26.calendarservice.repository;

import de.tum.devopss26.calendarservice.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    List<CalendarEvent> findAllByUserId(long userId);

}
