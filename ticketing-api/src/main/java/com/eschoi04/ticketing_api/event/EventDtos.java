package com.eschoi04.ticketing_api.event;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class EventDtos {

  private EventDtos() {}

  public record CreateEventRequest(
      @NotBlank(message = "이벤트 이름은 필수입니다") String name,
      @NotNull(message = "좌석 수는 필수입니다") @Min(value = 1, message = "좌석 수는 1 이상이어야 합니다")
          Integer seatCount) {}

  public record EventResponse(Long id, String name, int seatCount) {
    static EventResponse from(Event event) {
      return new EventResponse(event.getId(), event.getName(), event.getSeats().size());
    }
  }

  public record SeatResponse(Long id, int seatNumber, SeatStatus status) {
    static SeatResponse from(Seat seat) {
      return new SeatResponse(seat.getId(), seat.getSeatNumber(), seat.getStatus());
    }
  }

  public record SeatListResponse(Long eventId, List<SeatResponse> seats) {}

  public record CreateReservationRequest(@NotBlank(message = "고객 식별자는 필수입니다") String customerId) {}

  public record ReservationResponse(Long id, Long seatId, String customerId, SeatStatus status) {
    static ReservationResponse from(Reservation reservation, Seat seat) {
      return new ReservationResponse(
          reservation.getId(), seat.getId(), reservation.getCustomerId(), seat.getStatus());
    }
  }
}
