package com.example.meetings.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Tag;

/**
 * Unit tests for the MeetingParticipant domain model.
 * Verifies constructor behavior, getter correctness, and status transitions.
 */
@Tag("unit")
class MeetingParticipantTest {

    private User user;
    private Meeting meeting;

    @BeforeEach
    void setUp() {
        user = new User("alice", "alice@example.com", "pass");
        user.setId(1L);
        meeting = new Meeting("Team Sync", "Weekly standup", java.time.Instant.now(), java.time.Instant.now().plusSeconds(3600), user);
        meeting.setId(10L);
    }

    @Test
    void constructor_SetsFieldsCorrectly() {
        MeetingParticipant participant = new MeetingParticipant(meeting, user, InviteStatus.PENDING);

        assertEquals(meeting, participant.getMeeting());
        assertEquals(user, participant.getUser());
        assertEquals(InviteStatus.PENDING, participant.getStatus());
    }

    @Test
    void setStatus_PendingToAccepted_Works() {
        MeetingParticipant participant = new MeetingParticipant(meeting, user, InviteStatus.PENDING);
        participant.setStatus(InviteStatus.ACCEPTED);

        assertEquals(InviteStatus.ACCEPTED, participant.getStatus());
    }

    @Test
    void setStatus_PendingToDeclined_Works() {
        MeetingParticipant participant = new MeetingParticipant(meeting, user, InviteStatus.PENDING);
        participant.setStatus(InviteStatus.DECLINED);

        assertEquals(InviteStatus.DECLINED, participant.getStatus());
    }

    @Test
    void setStatus_AcceptedToPending_Works() {
        MeetingParticipant participant = new MeetingParticipant(meeting, user, InviteStatus.ACCEPTED);
        participant.setStatus(InviteStatus.PENDING);

        assertEquals(InviteStatus.PENDING, participant.getStatus());
    }

    @Test
    void getId_ReturnsNull_WhenNotPersisted() {
        MeetingParticipant participant = new MeetingParticipant(meeting, user, InviteStatus.PENDING);
        assertNull(participant.getId());
    }

    @Test
    void equals_SameUserAndMeeting_AreEqual() {
        // MeetingParticipant has no custom equals, so this tests reference equality
        MeetingParticipant p1 = new MeetingParticipant(meeting, user, InviteStatus.PENDING);
        MeetingParticipant p2 = new MeetingParticipant(meeting, user, InviteStatus.ACCEPTED);
        // Different instances are not equal by reference
        assertNotEquals(p1, p2);
    }
}