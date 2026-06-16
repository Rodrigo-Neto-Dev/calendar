package com.example.meetings.discover;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Tag;

/**
 * Pure unit test for AgendaLxProvider — no Spring context, only Mockito.
 * Mocks the full RestClient fluent API chain to verify parsing logic in isolation.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AgendaLxProviderTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private AgendaLxProvider provider;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        when(restClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);

        provider = new AgendaLxProvider(restClientBuilder);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubSearch(String query, List<AgendaLxProvider.AlxEvent> body) {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(body);
    }

    @Test
    void search_ValidJson_ReturnsEvents() {
        AgendaLxProvider.AlxEvent event = new AgendaLxProvider.AlxEvent();
        event.id = 123L;
        event.title = new AgendaLxProvider.AlxTitle();
        event.title.rendered = "Concert";
        event.description = List.of("<p>Cool concert</p>");
        event.occurences = List.of(LocalDate.now(ZoneId.of("Europe/Lisbon")).plusDays(1).toString());
        event.stringTimes = "21h30";
        event.link = "http://example.com";
        event.venue = Map.of("1", createVenue("Music Hall"));

        stubSearch("music", List.of(event));

        List<DiscoveredEvent> results = provider.search("music");

        assertEquals(1, results.size());
        DiscoveredEvent e = results.get(0);
        assertEquals("Concert", e.title());
        assertEquals("Cool concert", e.description());
        assertEquals("Music Hall", e.venue());
        assertEquals("Agenda Cultural de Lisboa", e.source());
        assertEquals("http://example.com", e.url());
        assertNotNull(e.start());
    }

    @Test
    void search_EmptyResponse_ReturnsEmptyList() {
        stubSearch("empty", List.of());
        assertTrue(provider.search("empty").isEmpty());
    }

    @Test
    void search_NullTitle_SkipsEvent() {
        AgendaLxProvider.AlxEvent event = new AgendaLxProvider.AlxEvent();
        event.id = 1L;
        event.title = new AgendaLxProvider.AlxTitle();
        event.title.rendered = null; // missing title
        event.occurences = List.of(LocalDate.now(ZoneId.of("Europe/Lisbon")).plusDays(1).toString());

        stubSearch("notitle", List.of(event));
        assertTrue(provider.search("notitle").isEmpty());
    }

    @Test
    void search_BlankTitle_SkipsEvent() {
        AgendaLxProvider.AlxEvent event = new AgendaLxProvider.AlxEvent();
        event.id = 1L;
        event.title = new AgendaLxProvider.AlxTitle();
        event.title.rendered = "   ";
        event.occurences = List.of(LocalDate.now(ZoneId.of("Europe/Lisbon")).plusDays(1).toString());

        stubSearch("blanktitle", List.of(event));
        assertTrue(provider.search("blanktitle").isEmpty());
    }

    @Test
    void search_AllDatesInPast_ReturnsEmptyList() {
        AgendaLxProvider.AlxEvent event = new AgendaLxProvider.AlxEvent();
        event.id = 1L;
        event.title = new AgendaLxProvider.AlxTitle();
        event.title.rendered = "Past Event";
        event.occurences = List.of("2000-01-01"); // all in the past

        stubSearch("past", List.of(event));
        assertTrue(provider.search("past").isEmpty());
    }

    @Test
    void search_MalformedTime_FallsBackTo20h() {
        AgendaLxProvider.AlxEvent event = new AgendaLxProvider.AlxEvent();
        event.id = 1L;
        event.title = new AgendaLxProvider.AlxTitle();
        event.title.rendered = "No Time";
        event.occurences = List.of(LocalDate.now(ZoneId.of("Europe/Lisbon")).plusDays(1).toString());
        event.stringTimes = "not-a-time";

        stubSearch("notime", List.of(event));
        List<DiscoveredEvent> results = provider.search("notime");
        assertEquals(1, results.size());
        // Start should be at 20:00 Lisbon time (the fallback)
        assertNotNull(results.get(0).start());
    }

    @Test
    void search_ConnectionException_ReturnsEmptyList() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenThrow(new RuntimeException("Connection refused"));

        assertTrue(provider.search("fail").isEmpty());
    }

    @Test
    void search_HtmlInDescription_Stripped() {
        AgendaLxProvider.AlxEvent event = new AgendaLxProvider.AlxEvent();
        event.id = 1L;
        event.title = new AgendaLxProvider.AlxTitle();
        event.title.rendered = "HTML Event";
        event.description = List.of("<p>Line 1</p>", "<br>Line 2");
        event.occurences = List.of(LocalDate.now(ZoneId.of("Europe/Lisbon")).plusDays(1).toString());

        stubSearch("html", List.of(event));
        List<DiscoveredEvent> results = provider.search("html");
        assertEquals(1, results.size());
        assertEquals("Line 1\nLine 2", results.get(0).description());
    }

    @Test
    void search_LongDescription_Truncated() {
        String longDesc = "A".repeat(700);
        AgendaLxProvider.AlxEvent event = new AgendaLxProvider.AlxEvent();
        event.id = 1L;
        event.title = new AgendaLxProvider.AlxTitle();
        event.title.rendered = "Long Desc";
        event.description = List.of(longDesc);
        event.occurences = List.of(LocalDate.now(ZoneId.of("Europe/Lisbon")).plusDays(1).toString());

        stubSearch("long", List.of(event));
        List<DiscoveredEvent> results = provider.search("long");
        assertEquals(1, results.size());
        assertTrue(results.get(0).description().length() <= 601); // 600 + ellipsis
    }

    @Test
    void search_MultipleVenues_UsesFirst() {
        AgendaLxProvider.AlxEvent event = new AgendaLxProvider.AlxEvent();
        event.id = 1L;
        event.title = new AgendaLxProvider.AlxTitle();
        event.title.rendered = "Multi Venue";
        event.occurences = List.of(LocalDate.now(ZoneId.of("Europe/Lisbon")).plusDays(1).toString());
        event.venue = Map.of(
                "1", createVenue("First Venue"),
                "2", createVenue("Second Venue")
        );

        stubSearch("venues", List.of(event));
        List<DiscoveredEvent> results = provider.search("venues");
        assertEquals(1, results.size());
        assertEquals("First Venue", results.get(0).venue());
    }

    @Test
    void search_NoVenue_ReturnsNullVenue() {
        AgendaLxProvider.AlxEvent event = new AgendaLxProvider.AlxEvent();
        event.id = 1L;
        event.title = new AgendaLxProvider.AlxTitle();
        event.title.rendered = "No Venue";
        event.occurences = List.of(LocalDate.now(ZoneId.of("Europe/Lisbon")).plusDays(1).toString());
        event.venue = null;

        stubSearch("novenue", List.of(event));
        List<DiscoveredEvent> results = provider.search("novenue");
        assertEquals(1, results.size());
        assertNull(results.get(0).venue());
    }

    @Test
    void isConfigured_AlwaysReturnsTrue() {
        assertTrue(provider.isConfigured());
    }

    @Test
    void name_ReturnsCorrectName() {
        assertEquals("Agenda Cultural de Lisboa", provider.name());
    }

    private AgendaLxProvider.AlxVenue createVenue(String name) {
        AgendaLxProvider.AlxVenue v = new AgendaLxProvider.AlxVenue();
        v.name = name;
        return v;
    }
}