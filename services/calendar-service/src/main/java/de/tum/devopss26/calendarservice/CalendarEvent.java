package de.tum.devopss26.calendarservice;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CalendarEvent {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;

    public CalendarEvent() {}

    public CalendarEvent(Long id, String title, String description, LocalDateTime startTime, LocalDateTime endTime, String location) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
    }
}
