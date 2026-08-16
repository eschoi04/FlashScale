package com.eschoi04.ticketing_api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationIntegrationTests {

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withDatabaseName("flashscale")
          .withUsername("flashscale")
          .withPassword("flashscale-test");

  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Test
  void migratesEmptyDatabaseRunsTicketingApiAndDoesNotRepeatMigrationAfterRestart()
      throws Exception {
    try (ConfigurableApplicationContext firstContext = startApplication()) {
      assertSchemaHistory(firstContext);
      exerciseTicketingApi(firstContext);
    }

    try (ConfigurableApplicationContext restartedContext = startApplication()) {
      assertSchemaHistory(restartedContext);
      Integer eventCount =
          restartedContext
              .getBean(JdbcTemplate.class)
              .queryForObject("SELECT COUNT(*) FROM events", Integer.class);
      assertThat(eventCount).isEqualTo(1);
    }
  }

  private ConfigurableApplicationContext startApplication() {
    return SpringApplication.run(
        TicketingApiApplication.class,
        "--server.port=0",
        "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
        "--spring.datasource.username=" + POSTGRES.getUsername(),
        "--spring.datasource.password=" + POSTGRES.getPassword(),
        "--spring.jpa.hibernate.ddl-auto=validate",
        "--spring.flyway.enabled=true");
  }

  private void assertSchemaHistory(ConfigurableApplicationContext context) {
    JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
    Integer appliedV1Count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success",
            Integer.class);
    assertThat(appliedV1Count).isEqualTo(1);
  }

  private void exerciseTicketingApi(ConfigurableApplicationContext context) throws Exception {
    ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
    int port = context.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
    String baseUrl = "http://localhost:" + port;

    HttpResponse<String> createEventResponse =
        sendJson(
            baseUrl + "/api/events", "POST", "{\"name\":\"Migration Test Event\",\"seatCount\":2}");
    assertThat(createEventResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    JsonNode event = objectMapper.readTree(createEventResponse.body());
    long eventId = event.get("id").asLong();

    HttpResponse<String> seatsResponse =
        sendJson(baseUrl + "/api/events/" + eventId + "/seats", "GET", null);
    assertThat(seatsResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
    JsonNode seats = objectMapper.readTree(seatsResponse.body()).get("seats");
    assertThat(seats.size()).isEqualTo(2);
    assertThat(seats.get(0).get("status").asText()).isEqualTo("AVAILABLE");

    long seatId = seats.get(0).get("id").asLong();
    HttpResponse<String> reservationResponse =
        sendJson(
            baseUrl + "/api/events/" + eventId + "/seats/" + seatId + "/reservations",
            "POST",
            "{\"customerId\":\"migration-test-user\"}");
    assertThat(reservationResponse.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    JsonNode reservation = objectMapper.readTree(reservationResponse.body());
    assertThat(reservation.get("seatId").asLong()).isEqualTo(seatId);
    assertThat(reservation.get("status").asText()).isEqualTo("RESERVED");
  }

  private HttpResponse<String> sendJson(String url, String method, String body) throws Exception {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json");
    if (body == null) {
      request.method(method, HttpRequest.BodyPublishers.noBody());
    } else {
      request.method(method, HttpRequest.BodyPublishers.ofString(body));
    }
    return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }
}
