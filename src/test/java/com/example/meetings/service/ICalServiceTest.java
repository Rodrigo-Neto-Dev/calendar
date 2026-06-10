package com.example.meetings.service;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;

@Tag("unit")
class ICalServiceTest {

    private final ICalService iCalService = new ICalService();

    @Test
    void render_EmptyCalendar_ValidIcal() {
        User owner = new User("owner", "owner@example.com", "pass");
        String ical = iCalService.render(owner, List.of());
        assertTrue(ical.contains("BEGIN:VCALENDAR"));
        assertTrue(ical.contains("END:VCALENDAR"));
    }

    @Test
    void render_EscapesSpecialCharacters() {
        User owner = new User("owner", "owner@example.com", "pass");
        Meeting meeting = new Meeting("Title; with, comma\nand newline", "Desc", Instant.now(), Instant.now().plusSeconds(3600), owner);
        meeting.setId(1L);

        String ical = iCalService.render(owner, List.of(meeting));

        assertTrue(ical.contains("SUMMARY:Title\\; with\\, comma\\nand newline"));
    }
}
