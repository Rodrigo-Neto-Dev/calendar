package com.example.meetings.controller;

import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import com.example.meetings.service.ICalService;
import com.example.meetings.service.MeetingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.example.meetings.config.SecurityConfig;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Tag;

@Tag("integration")
@WebMvcTest(ICalController.class)
@Import(SecurityConfig.class)
public class ICalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private MeetingService meetingService;

    @MockBean
    private ICalService icalService;

    @Test
    void exportCalendar_ValidToken_ReturnsIcsFile() throws Exception {
        User user = new User("testuser", "test@example.com", "pass");
        when(userRepository.findByIcalToken("valid-token")).thenReturn(Optional.of(user));
        when(meetingService.calendarFor(user)).thenReturn(List.of());
        when(icalService.render(any(), any())).thenReturn("BEGIN:VCALENDAR\nEND:VCALENDAR");

        mockMvc.perform(get("/ical/valid-token.ics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/calendar; charset=UTF-8"))
                .andExpect(content().string("BEGIN:VCALENDAR\nEND:VCALENDAR"));
    }

    @Test
    void exportCalendar_EmptyCalendar_ReturnsValidIcs() throws Exception {
        User user = new User("testuser", "test@example.com", "pass");
        when(userRepository.findByIcalToken("valid-token")).thenReturn(Optional.of(user));
        when(meetingService.calendarFor(user)).thenReturn(List.of());
        when(icalService.render(any(), any())).thenReturn("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nEND:VCALENDAR");

        mockMvc.perform(get("/ical/valid-token.ics"))
                .andExpect(status().isOk())
                .andExpect(content().string("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nEND:VCALENDAR"));
    }

    @Test
    void exportCalendar_InvalidToken_ReturnsNotFound() throws Exception {
        when(userRepository.findByIcalToken("invalid")).thenReturn(Optional.empty());

        mockMvc.perform(get("/ical/invalid.ics"))
                .andExpect(status().isNotFound());
    }
}
