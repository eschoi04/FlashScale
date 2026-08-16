package com.eschoi04.ticketing_api.event;

import com.eschoi04.ticketing_api.event.EventDtos.CreateEventRequest;
import com.eschoi04.ticketing_api.event.EventDtos.CreateReservationRequest;
import com.eschoi04.ticketing_api.event.EventDtos.EventResponse;
import com.eschoi04.ticketing_api.event.EventDtos.ReservationResponse;
import com.eschoi04.ticketing_api.event.EventDtos.SeatListResponse;
import com.eschoi04.ticketing_api.event.EventDtos.SeatResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

  private final EventRepository eventRepository;

  private final SeatRepository seatRepository;

  private final ReservationRepository reservationRepository;

  public EventService(
      EventRepository eventRepository,
      SeatRepository seatRepository,
      ReservationRepository reservationRepository) {
    this.eventRepository = eventRepository;
    this.seatRepository = seatRepository;
    this.reservationRepository = reservationRepository;
  }

  @Transactional
  public EventResponse createEvent(CreateEventRequest request) {
    Event event = new Event(request.name().trim(), request.seatCount());
    return EventResponse.from(eventRepository.save(event));
  }

  @Transactional(readOnly = true)
  public EventResponse getEvent(Long eventId) {
    return EventResponse.from(findEvent(eventId));
  }

  @Transactional(readOnly = true)
  public SeatListResponse getSeats(Long eventId) {
    findEvent(eventId);
    List<SeatResponse> seats =
        seatRepository.findAllByEvent_IdOrderBySeatNumber(eventId).stream()
            .map(SeatResponse::from)
            .toList();
    return new SeatListResponse(eventId, seats);
  }

  @Transactional
  public ReservationResponse reserveSeat(
      Long eventId, Long seatId, CreateReservationRequest request) {
    findEvent(eventId);
    Seat seat =
        seatRepository
            .findByIdAndEvent_Id(seatId, eventId)
            .orElseThrow(() -> new SeatNotFoundException(eventId, seatId));
    Reservation reservation = new Reservation(seat, request.customerId().trim());
    seat.reserve(reservation);
    reservationRepository.save(reservation);
    return ReservationResponse.from(reservation, seat);
  }

  private Event findEvent(Long eventId) {
    return eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
  }
}
