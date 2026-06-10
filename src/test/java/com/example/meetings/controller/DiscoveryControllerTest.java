package com.example.meetings.controller;

import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.discover.DiscoveryService;
import com.example.meetings.model.User;
import com.example.meetings.service.MeetingService;
import com.example.meetings.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.example.meetings.config.SecurityConfig;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Tag;

@Tag("integration")
@WebMvcTest(DiscoveryController.class)
@Import(SecurityConfig.class)
class DiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DiscoveryService discoveryService;

    @MockBean
    private MeetingService meetingService;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser
    void discover_AuthenticatedAccess_ReturnsDiscoverView() throws Exception {
        when(discoveryService.providers()).thenReturn(List.of());

        mockMvc.perform(get("/discover"))
                .andExpect(status().isOk())
                .andExpect(view().name("discover"))
                .andExpect(model().attributeExists("providers", "anyConfigured", "q", "results"));
    }

    @Test
    @WithMockUser
    void discover_WithQuery_ReturnsResults() throws Exception {
        var event = new DiscoveredEvent("Source", "1", "Event", "Desc", Instant.now(), null, "url", "venue");
        
        com.example.meetings.discover.EventProvider mockProvider = org.mockito.Mockito.mock(com.example.meetings.discover.EventProvider.class);
        when(mockProvider.isConfigured()).thenReturn(true);
        when(discoveryService.providers()).thenReturn(List.of(mockProvider));
        
        when(discoveryService.search("music")).thenReturn(List.of(event));

        mockMvc.perform(get("/discover").param("q", "music"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("results", List.of(event)));
    }

    @Test
    @WithMockUser(username = "testuser")
    void copy_Success_RedirectsToCalendar() throws Exception {
        User user = new User("testuser", "test@example.com", "pass");
        when(userService.requireByUsername("testuser")).thenReturn(user);

        mockMvc.perform(post("/discover/copy")
                        .param("source", "Source")
                        .param("externalId", "123")
                        .param("title", "Event")
                        .param("start", Instant.now().toString())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));

        verify(meetingService).copyFromDiscovered(any(), any());
    }
}
