package com.example.meetings.repository;

import com.example.meetings.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.junit.jupiter.api.Tag;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@DataJpaTest
class MeetingRepositoryTest {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findCalendarMeetings_IncludesOrganizedAndJoinedMeetings_FiltersDeclined() {
        User alice = new User("alice", "alice@example.com", "pass");
        User bob = new User("bob", "bob@example.com", "pass");
        entityManager.persist(alice);
        entityManager.persist(bob);

        Instant now = Instant.now();

        // 1. Alice is organizer
        Meeting organized = new Meeting("Alice Organized", "Desc", now.plus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS), alice);
        entityManager.persist(organized);

        // 2. Alice is accepted participant
        Meeting accepted = new Meeting("Alice Accepted", "Desc", now.plus(3, ChronoUnit.HOURS), now.plus(4, ChronoUnit.HOURS), bob);
        MeetingParticipant partAccepted = new MeetingParticipant(accepted, alice, InviteStatus.ACCEPTED);
        accepted.addParticipant(partAccepted);
        entityManager.persist(accepted);
        entityManager.persist(partAccepted);

        // 3. Alice is pending participant
        Meeting pending = new Meeting("Alice Pending", "Desc", now.plus(5, ChronoUnit.HOURS), now.plus(6, ChronoUnit.HOURS), bob);
        MeetingParticipant partPending = new MeetingParticipant(pending, alice, InviteStatus.PENDING);
        pending.addParticipant(partPending);
        entityManager.persist(pending);
        entityManager.persist(partPending);

        // 4. Alice is declined participant (should be excluded)
        Meeting declined = new Meeting("Alice Declined", "Desc", now.plus(7, ChronoUnit.HOURS), now.plus(8, ChronoUnit.HOURS), bob);
        MeetingParticipant partDeclined = new MeetingParticipant(declined, alice, InviteStatus.DECLINED);
        declined.addParticipant(partDeclined);
        entityManager.persist(declined);
        entityManager.persist(partDeclined);

        entityManager.flush();

        List<Meeting> calendar = meetingRepository.findCalendarMeetings(alice);

        assertEquals(3, calendar.size());
        assertEquals("Alice Organized", calendar.get(0).getTitle());
        assertEquals("Alice Accepted", calendar.get(1).getTitle());
        assertEquals("Alice Pending", calendar.get(2).getTitle());
    }

    @Test
    void findOverlapping_DetectsOverlapsCorrectly() {
        User alice = new User("alice", "alice@example.com", "pass");
        entityManager.persist(alice);

        Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        
        // Meeting: 10:00 to 11:00
        Meeting meeting = new Meeting("Meeting", "Desc", start.plus(10, ChronoUnit.HOURS), start.plus(11, ChronoUnit.HOURS), alice);
        entityManager.persist(meeting);
        entityManager.flush();

        // Overlap cases for range [9:30 - 10:30] (Starts before, ends inside)
        List<Meeting> overlap1 = meetingRepository.findOverlapping(alice, start.plus(9, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES), start.plus(10, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES));
        assertEquals(1, overlap1.size());

        // Overlap cases for range [10:30 - 11:30] (Starts inside, ends after)
        List<Meeting> overlap2 = meetingRepository.findOverlapping(alice, start.plus(10, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES), start.plus(11, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES));
        assertEquals(1, overlap2.size());

        // Overlap cases for range [10:15 - 10:45] (Completely inside)
        List<Meeting> overlap3 = meetingRepository.findOverlapping(alice, start.plus(10, ChronoUnit.HOURS).plus(15, ChronoUnit.MINUTES), start.plus(10, ChronoUnit.HOURS).plus(45, ChronoUnit.MINUTES));
        assertEquals(1, overlap3.size());

        // Overlap cases for range [9:00 - 12:00] (Completely encompasses)
        List<Meeting> overlap4 = meetingRepository.findOverlapping(alice, start.plus(9, ChronoUnit.HOURS), start.plus(12, ChronoUnit.HOURS));
        assertEquals(1, overlap4.size());

        // No overlap: ends exactly at start [9:00 - 10:00]
        List<Meeting> noOverlap1 = meetingRepository.findOverlapping(alice, start.plus(9, ChronoUnit.HOURS), start.plus(10, ChronoUnit.HOURS));
        assertTrue(noOverlap1.isEmpty());

        // No overlap: starts exactly at end [11:00 - 12:00]
        List<Meeting> noOverlap2 = meetingRepository.findOverlapping(alice, start.plus(11, ChronoUnit.HOURS), start.plus(12, ChronoUnit.HOURS));
        assertTrue(noOverlap2.isEmpty());
    }
}
