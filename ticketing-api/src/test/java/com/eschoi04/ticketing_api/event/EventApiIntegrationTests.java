package com.eschoi04.ticketing_api.event;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest
class EventApiIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void creatingEventCreatesRequestedSeatsAndSupportsQueries() throws Exception {
    JsonNode event = createEvent("FlashScale Concert", 3);
    long eventId = event.get("id").asLong();

    mockMvc
        .perform(get("/api/events/{eventId}", eventId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(eventId))
        .andExpect(jsonPath("$.name").value("FlashScale Concert"))
        .andExpect(jsonPath("$.seatCount").value(3));

    mockMvc
        .perform(get("/api/events/{eventId}/seats", eventId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventId").value(eventId))
        .andExpect(jsonPath("$.seats", hasSize(3)))
        .andExpect(jsonPath("$.seats[0].seatNumber").value(1))
        .andExpect(jsonPath("$.seats[0].status").value("AVAILABLE"))
        .andExpect(jsonPath("$.seats[2].seatNumber").value(3));
  }

  @Test
  void reservingSeatChangesStatusAndSequentialRetryReturnsConflict() throws Exception {
    JsonNode event = createEvent("FlashScale Concert", 1);
    long eventId = event.get("id").asLong();
    long seatId = getFirstSeatId(eventId);

    mockMvc
        .perform(
            post("/api/events/{eventId}/seats/{seatId}/reservations", eventId, seatId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"load-test-user-1\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.seatId").value(seatId))
        .andExpect(jsonPath("$.customerId").value("load-test-user-1"))
        .andExpect(jsonPath("$.status").value("RESERVED"));

    mockMvc
        .perform(get("/api/events/{eventId}/seats", eventId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.seats[0].status").value("RESERVED"));

    mockMvc
        .perform(
            post("/api/events/{eventId}/seats/{seatId}/reservations", eventId, seatId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"load-test-user-2\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.code").value("SEAT_ALREADY_RESERVED"))
        .andExpect(jsonPath("$.path").value(reservationPath(eventId, seatId)));
  }

  @Test
  void gettingUnknownEventReturnsNotFound() throws Exception {
    mockMvc
        .perform(get("/api/events/{eventId}", 999999))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"))
        .andExpect(jsonPath("$.fieldErrors").isMap());
  }

  @Test
  void seatCannotBeReservedThroughAnotherEvent() throws Exception {
    long firstEventId = createEvent("First Event", 1).get("id").asLong();
    long secondEventId = createEvent("Second Event", 1).get("id").asLong();
    long firstEventSeatId = getFirstSeatId(firstEventId);

    mockMvc
        .perform(
            post(
                    "/api/events/{eventId}/seats/{seatId}/reservations",
                    secondEventId,
                    firstEventSeatId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"load-test-user-1\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SEAT_NOT_FOUND"));
  }

  @Test
  void invalidEventRequestsReturnBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"   \",\"seatCount\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.fieldErrors.name").value("이벤트 이름은 필수입니다"))
        .andExpect(jsonPath("$.fieldErrors.seatCount").value("좌석 수는 1 이상이어야 합니다"));
  }

  @Test
  void blankCustomerIdReturnsBadRequest() throws Exception {
    long eventId = createEvent("FlashScale Concert", 1).get("id").asLong();
    long seatId = getFirstSeatId(eventId);

    mockMvc
        .perform(
            post("/api/events/{eventId}/seats/{seatId}/reservations", eventId, seatId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.fieldErrors.customerId").value("고객 식별자는 필수입니다"));
  }

  private JsonNode createEvent(String name, int seatCount) throws Exception {
    String requestBody =
        objectMapper.createObjectNode().put("name", name).put("seatCount", seatCount).toString();
    MvcResult result =
        mockMvc
            .perform(
                post("/api/events").contentType(MediaType.APPLICATION_JSON).content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.seatCount").value(seatCount))
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private long getFirstSeatId(long eventId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/api/events/{eventId}/seats", eventId))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper
        .readTree(result.getResponse().getContentAsString())
        .get("seats")
        .get(0)
        .get("id")
        .asLong();
  }

  private String reservationPath(long eventId, long seatId) {
    return "/api/events/" + eventId + "/seats/" + seatId + "/reservations";
  }
}
