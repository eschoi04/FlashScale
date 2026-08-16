package com.eschoi04.ticketing_api.event;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {

  List<Seat> findAllByEvent_IdOrderBySeatNumber(Long eventId);

  Optional<Seat> findByIdAndEvent_Id(Long id, Long eventId);
}
