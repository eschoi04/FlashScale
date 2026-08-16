package com.eschoi04.ticketing_api.event;

import com.eschoi04.ticketing_api.event.EventDtos.CreateEventRequest;
import com.eschoi04.ticketing_api.event.EventDtos.CreateReservationRequest;
import com.eschoi04.ticketing_api.event.EventDtos.EventResponse;
import com.eschoi04.ticketing_api.event.EventDtos.ReservationResponse;
import com.eschoi04.ticketing_api.event.EventDtos.SeatListResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

  private final EventService eventService;

  public EventController(EventService eventService) {
    this.eventService = eventService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EventResponse createEvent(@Valid @RequestBody CreateEventRequest request) {
    return eventService.createEvent(request);
  }

  @GetMapping("/{eventId}")
  public EventResponse getEvent(@PathVariable Long eventId) {
    return eventService.getEvent(eventId);
  }

  @GetMapping("/{eventId}/seats")
  public SeatListResponse getSeats(@PathVariable Long eventId) {
    return eventService.getSeats(eventId);
  }

  @PostMapping("/{eventId}/seats/{seatId}/reservations")
  @ResponseStatus(HttpStatus.CREATED)
  public ReservationResponse reserveSeat(
      @PathVariable Long eventId,
      @PathVariable Long seatId,
      @Valid @RequestBody CreateReservationRequest request) {
    return eventService.reserveSeat(eventId, seatId, request);
  }
}
