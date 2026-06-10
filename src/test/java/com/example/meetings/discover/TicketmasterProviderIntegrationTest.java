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
@RestClientTest(TicketmasterProvider.class)
@TestPropertySource(properties = "app.discover.ticketmaster.api-key=test-key")
class TicketmasterProviderIntegrationTest {

    @Autowired
    private TicketmasterProvider provider;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void search_ParsesCorrectly() {
        String json = """
            {
              "_embedded": {
                "events": [
                  {
                    "id": "tm123",
                    "name": "Rock Show",
                    "url": "http://tm.com",
                    "info": "Rocking",
                    "dates": {
                      "start": { "dateTime": "2099-01-01T20:00:00Z" }
                    },
                    "_embedded": {
                      "venues": [{ "name": "Arena" }]
                    }
                  }
                ]
              }
            }
            """;

        this.server.expect(requestTo(containsString("/events.json?keyword=rock")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<DiscoveredEvent> events = provider.search("rock");

        assertEquals(1, events.size());
        assertEquals("Rock Show", events.get(0).title());
        assertEquals("Arena", events.get(0).venue());
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
