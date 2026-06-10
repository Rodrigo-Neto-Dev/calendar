package com.example.meetings.discover;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Tag;

@Tag("provider")
@RestClientTest(SeatGeekProvider.class)
@TestPropertySource(properties = "app.discover.seatgeek.client-id=test-client")
class SeatGeekProviderIntegrationTest {

    @Autowired
    private SeatGeekProvider provider;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void search_ParsesCorrectly() {
        String json = """
            {
              "events": [
                {
                  "id": 456,
                  "title": "Jazz Night",
                  "datetime_utc": "2099-01-01T21:00:00",
                  "url": "http://sg.com",
                  "venue": { "name": "Jazz Club" }
                }
              ]
            }
            """;

        this.server.expect(requestTo(containsString("/events?q=jazz")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<DiscoveredEvent> events = provider.search("jazz");

        assertEquals(1, events.size());
        assertEquals("Jazz Night", events.get(0).title());
        assertEquals("Jazz Club", events.get(0).venue());
    }

    @Test
    void search_HandlesErrorResponse() {
        this.server.expect(requestTo(containsString("error")))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());

        List<DiscoveredEvent> events = provider.search("error");

        assertTrue(events.isEmpty());
    }

    @Test
    void search_HandlesMalformedJson() {
        this.server.expect(requestTo(containsString("malformed")))
                .andRespond(withSuccess("{ invalid }", MediaType.APPLICATION_JSON));

        List<DiscoveredEvent> events = provider.search("malformed");

        assertTrue(events.isEmpty());
    }
}
