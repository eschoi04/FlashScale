package com.eschoi04.ticketing_api.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reservations")
public class Reservation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "seat_id", nullable = false, unique = true)
  private Seat seat;

  @Column(name = "customer_id", nullable = false)
  private String customerId;

  protected Reservation() {}

  public Reservation(Seat seat, String customerId) {
    this.seat = seat;
    this.customerId = customerId;
  }

  public Long getId() {
    return id;
  }

  public Long getSeatId() {
    return seat.getId();
  }

  public String getCustomerId() {
    return customerId;
  }
}
