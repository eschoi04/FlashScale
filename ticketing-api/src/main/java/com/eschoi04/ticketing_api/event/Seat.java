package com.eschoi04.ticketing_api.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "seats",
    uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "seat_number"}))
public class Seat {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id", nullable = false)
  private Event event;

  @Column(name = "seat_number", nullable = false)
  private int seatNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SeatStatus status;

  @OneToOne(mappedBy = "seat", fetch = FetchType.LAZY)
  private Reservation reservation;

  protected Seat() {}

  Seat(Event event, int seatNumber) {
    this.event = event;
    this.seatNumber = seatNumber;
    this.status = SeatStatus.AVAILABLE;
  }

  public void reserve(Reservation reservation) {
    if (status == SeatStatus.RESERVED) {
      throw new SeatAlreadyReservedException(id);
    }
    this.reservation = reservation;
    this.status = SeatStatus.RESERVED;
  }

  public Long getId() {
    return id;
  }

  public Long getEventId() {
    return event.getId();
  }

  public int getSeatNumber() {
    return seatNumber;
  }

  public SeatStatus getStatus() {
    return status;
  }
}
