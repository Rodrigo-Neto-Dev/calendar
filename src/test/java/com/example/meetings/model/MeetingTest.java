package com.example.meetings.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class MeetingTest {

    private User organizer;
    private User invitee1;
    private User invitee2;
    private Instant start;
    private Instant end;

    @BeforeEach
    void setUp() {
        organizer = new User("org", "org@example.com", "pass");
        invitee1 = new User("inv1", "inv1@example.com", "pass");
        invitee2 = new User("inv2", "inv2@example.com", "pass");
        start = Instant.now();
        end = start.plus(1, ChronoUnit.HOURS);
    }

    @Test
    void isConfirmed_EmptyParticipants_ReturnsFalse() {
        Meeting meeting = new Meeting("Title", "Desc", start, end, organizer);
        assertFalse(meeting.isConfirmed());
    }

    @Test
    void isConfirmed_AllAccepted_ReturnsTrue() {
        Meeting meeting = new Meeting("Title", "Desc", start, end, organizer);
        MeetingParticipant p1 = new MeetingParticipant(meeting, organizer, InviteStatus.ACCEPTED);
        MeetingParticipant p2 = new MeetingParticipant(meeting, invitee1, InviteStatus.ACCEPTED);
        meeting.addParticipant(p1);
        meeting.addParticipant(p2);

        assertTrue(meeting.isConfirmed());
    }

    @Test
    void isConfirmed_HasPending_ReturnsFalse() {
        Meeting meeting = new Meeting("Title", "Desc", start, end, organizer);
        MeetingParticipant p1 = new MeetingParticipant(meeting, organizer, InviteStatus.ACCEPTED);
        MeetingParticipant p2 = new MeetingParticipant(meeting, invitee1, InviteStatus.PENDING);
        meeting.addParticipant(p1);
        meeting.addParticipant(p2);

        assertFalse(meeting.isConfirmed());
    }

    @Test
    void isConfirmed_HasDeclined_ReturnsFalse() {
        Meeting meeting = new Meeting("Title", "Desc", start, end, organizer);
        MeetingParticipant p1 = new MeetingParticipant(meeting, organizer, InviteStatus.ACCEPTED);
        MeetingParticipant p2 = new MeetingParticipant(meeting, invitee1, InviteStatus.DECLINED);
        meeting.addParticipant(p1);
        meeting.addParticipant(p2);

        assertFalse(meeting.isConfirmed());
    }

    @Test
    void gettersAndSetters_WorkCorrectly() {
        Meeting meeting = new Meeting("Title", "Desc", start, end, organizer);
        meeting.setId(99L);
        meeting.setTitle("New Title");
        meeting.setDescription("New Desc");
        Instant newStart = start.plus(1, ChronoUnit.DAYS);
        Instant newEnd = end.plus(1, ChronoUnit.DAYS);
        meeting.setStartTime(newStart);
        meeting.setEndTime(newEnd);

        assertEquals(99L, meeting.getId());
        assertEquals("New Title", meeting.getTitle());
        assertEquals("New Desc", meeting.getDescription());
        assertEquals(newStart, meeting.getStartTime());
        assertEquals(newEnd, meeting.getEndTime());
        assertEquals(organizer, meeting.getOrganizer());
    }
}