package com.eschoi04.ticketing_api.event;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "events")
public class Event {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Seat> seats = new ArrayList<>();

  protected Event() {}

  public Event(String name, int seatCount) {
    this.name = name;
    for (int seatNumber = 1; seatNumber <= seatCount; seatNumber++) {
      seats.add(new Seat(this, seatNumber));
    }
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public List<Seat> getSeats() {
    return Collections.unmodifiableList(seats);
  }
}
