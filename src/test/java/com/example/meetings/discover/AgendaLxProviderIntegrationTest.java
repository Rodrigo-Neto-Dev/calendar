package com.example.meetings.discover;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Tag;

@Tag("provider")
@RestClientTest(AgendaLxProvider.class)
class AgendaLxProviderIntegrationTest {

    @Autowired
    private AgendaLxProvider provider;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void search_ParsesCorrectly() {
        String json = """
            [
              {
                "id": 123,
                "title": { "rendered": "Concert" },
                "description": ["<p>Cool concert</p>"],
                "occurences": ["2099-01-01"],
                "string_times": "21h30",
                "link": "http://example.com",
                "venue": { "1": { "name": "Music Hall" } }
              }
            ]
            """;

        this.server.expect(requestTo("https://www.agendalx.pt/wp-json/agendalx/v1/events?search=music&per_page=20"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<DiscoveredEvent> events = provider.search("music");

        assertEquals(1, events.size());
        DiscoveredEvent e = events.get(0);
        assertEquals("Concert", e.title());
        assertEquals("Cool concert", e.description());
        assertEquals("Music Hall", e.venue());
        assertEquals("Agenda Cultural de Lisboa", e.source());
    }

    @Test
    void search_HandlesEmptyResponse() {
        this.server.expect(requestTo("https://www.agendalx.pt/wp-json/agendalx/v1/events?search=empty&per_page=20"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<DiscoveredEvent> events = provider.search("empty");

        assertTrue(events.isEmpty());
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
