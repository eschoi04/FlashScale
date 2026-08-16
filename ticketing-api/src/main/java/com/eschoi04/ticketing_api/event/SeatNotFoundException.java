package com.eschoi04.ticketing_api.event;

import com.eschoi04.ticketing_api.common.error.DomainException;
import org.springframework.http.HttpStatus;

public class SeatNotFoundException extends DomainException {

  public SeatNotFoundException(Long eventId, Long seatId) {
    super(
        HttpStatus.NOT_FOUND,
        "SEAT_NOT_FOUND",
        "이벤트에 속한 좌석을 찾을 수 없습니다: eventId=" + eventId + ", seatId=" + seatId);
  }
}
