package com.eschoi04.ticketing_api.event;

import com.eschoi04.ticketing_api.common.error.DomainException;
import org.springframework.http.HttpStatus;

public class SeatAlreadyReservedException extends DomainException {

  public SeatAlreadyReservedException(Long seatId) {
    super(HttpStatus.CONFLICT, "SEAT_ALREADY_RESERVED", "이미 예약된 좌석입니다: " + seatId);
  }
}
