package com.example.meetings.discover;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Tag;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DiscoveryServiceTest {

    @Test
    void discoverEvents_AllProvidersReturn_AggregatesResults() {
        EventProvider p1 = mock(EventProvider.class);
        EventProvider p2 = mock(EventProvider.class);
        
        when(p1.isConfigured()).thenReturn(true);
        when(p2.isConfigured()).thenReturn(true);
        
        Instant now = Instant.now();
        DiscoveredEvent e1 = new DiscoveredEvent("P1", "1", "Early", null, now, null, "url1", null);
        DiscoveredEvent e2 = new DiscoveredEvent("P2", "2", "Late", null, now.plusSeconds(3600), null, "url2", null);
        DiscoveredEvent e3 = new DiscoveredEvent("P2", "3", "Duplicate", null, now, null, "url1", null);
        
        when(p1.search("test")).thenReturn(List.of(e1));
        when(p2.search("test")).thenReturn(List.of(e2, e3));
        
        DiscoveryService service = new DiscoveryService(List.of(p1, p2));
        List<DiscoveredEvent> results = service.search("test");
        
        assertEquals(2, results.size());
        assertEquals("Early", results.get(0).title());
        assertEquals("Late", results.get(1).title());
    }

    @Test
    void discoverEvents_OneProviderFails_OthersStillContribute() {
        EventProvider p1 = mock(EventProvider.class);
        EventProvider p2 = mock(EventProvider.class);
        
        when(p1.isConfigured()).thenReturn(true);
        when(p2.isConfigured()).thenReturn(true);
        
        Instant now = Instant.now();
        DiscoveredEvent e2 = new DiscoveredEvent("P2", "2", "Event 2", null, now, null, "url2", null);
        
        // Mock p1 failing (returning empty list because in practice providers swallow exceptions and return empty list)
        when(p1.search("test")).thenReturn(List.of());
        when(p2.search("test")).thenReturn(List.of(e2));
        
        DiscoveryService service = new DiscoveryService(List.of(p1, p2));
        List<DiscoveredEvent> results = service.search("test");
        
        assertEquals(1, results.size());
        assertEquals("Event 2", results.get(0).title());
    }

    @Test
    void discoverEvents_AllProvidersFail_ReturnsEmptyList() {
        EventProvider p1 = mock(EventProvider.class);
        EventProvider p2 = mock(EventProvider.class);
        
        when(p1.isConfigured()).thenReturn(true);
        when(p2.isConfigured()).thenReturn(true);
        
        when(p1.search("test")).thenReturn(List.of());
        when(p2.search("test")).thenReturn(List.of());
        
        DiscoveryService service = new DiscoveryService(List.of(p1, p2));
        List<DiscoveredEvent> results = service.search("test");
        
        assertEquals(0, results.size());
    }

    @Test
    void discoverEvents_NoProviders_ReturnsEmptyList() {
        DiscoveryService service = new DiscoveryService(List.of());
        List<DiscoveredEvent> results = service.search("test");
        assertEquals(0, results.size());
    }

    @Test
    void search_IgnoresUnconfiguredProviders() {
        EventProvider p1 = mock(EventProvider.class);
        when(p1.isConfigured()).thenReturn(false);
        
        DiscoveryService service = new DiscoveryService(List.of(p1));
        List<DiscoveredEvent> results = service.search("test");
        
        assertEquals(0, results.size());
    }

    @Test
    void search_HandlesEmptyQuery() {
        DiscoveryService service = new DiscoveryService(List.of());
        assertEquals(0, service.search(null).size());
        assertEquals(0, service.search(" ").size());
    }
}
