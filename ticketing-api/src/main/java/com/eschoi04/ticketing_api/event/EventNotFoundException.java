package com.eschoi04.ticketing_api.event;

import com.eschoi04.ticketing_api.common.error.DomainException;
import org.springframework.http.HttpStatus;

public class EventNotFoundException extends DomainException {

  public EventNotFoundException(Long eventId) {
    super(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "이벤트를 찾을 수 없습니다: " + eventId);
  }
}
