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

    @Test
    void render_WithMeetings_RendersVeventFields() {
        User owner = new User("owner", "owner@example.com", "pass");
        User bob = new User("bob", "bob@example.com", "pass");
        Meeting meeting = new Meeting("Test Meeting", "Meeting Description", Instant.parse("2026-06-12T10:00:00Z"), Instant.parse("2026-06-12T11:00:00Z"), owner);
        meeting.setId(42L);
        
        MeetingParticipant p1 = new MeetingParticipant(meeting, owner, InviteStatus.ACCEPTED);
        MeetingParticipant p2 = new MeetingParticipant(meeting, bob, InviteStatus.PENDING);
        meeting.addParticipant(p1);
        meeting.addParticipant(p2);

        String ical = iCalService.render(owner, List.of(meeting));

        assertTrue(ical.contains("UID:meeting-42@meetings-app"));
        assertTrue(ical.contains("DTSTART:20260612T100000Z"));
        assertTrue(ical.contains("DTEND:20260612T110000Z"));
        assertTrue(ical.contains("SUMMARY:Test Meeting"));
        assertTrue(ical.contains("DESCRIPTION:Meeting Description"));
        assertTrue(ical.contains("ORGANIZER;CN=owner:mailto:owner@example.com"));
        assertTrue(ical.contains("ATTENDEE;CN=owner;PARTSTAT=ACCEPTED:mailto:owner@example.com"));
        assertTrue(ical.contains("ATTENDEE;CN=bob;PARTSTAT=NEEDS-ACTION:mailto:bob@example.com"));
    }

    @Test
    void render_ConfirmedAndTentativeStatus() {
        User owner = new User("owner", "owner@example.com", "pass");
        Meeting meeting = new Meeting("Test Meeting", "Description", Instant.now(), Instant.now().plusSeconds(3600), owner);
        meeting.setId(1L);

        // Scenario 1: No participants -> TENTATIVE
        String ical1 = iCalService.render(owner, List.of(meeting));
        assertTrue(ical1.contains("STATUS:TENTATIVE"));

        // Scenario 2: Organizer accepted -> CONFIRMED (since organizer is the only participant)
        MeetingParticipant p1 = new MeetingParticipant(meeting, owner, InviteStatus.ACCEPTED);
        meeting.addParticipant(p1);
        String ical2 = iCalService.render(owner, List.of(meeting));
        assertTrue(ical2.contains("STATUS:CONFIRMED"));

        // Scenario 3: Add pending invitee -> TENTATIVE
        User invitee = new User("invitee", "inv@example.com", "pass");
        MeetingParticipant p2 = new MeetingParticipant(meeting, invitee, InviteStatus.PENDING);
        meeting.addParticipant(p2);
        String ical3 = iCalService.render(owner, List.of(meeting));
        assertTrue(ical3.contains("STATUS:TENTATIVE"));
    }
}

