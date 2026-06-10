package com.example.meetings.service;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.User;
import com.example.meetings.repository.MeetingParticipantRepository;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Tag;

@Tag("integration")
@SpringBootTest
@Transactional
public class MeetingServiceIntegrationTest {

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    private User organizer;
    private User invitee;

    @BeforeEach
    void setUp() {
        organizer = userService.register("org", "org@example.com", "pass");
        invitee = userService.register("inv", "inv@example.com", "pass");
    }

    @Test
    void proposeAndRespond_UpdatesDatabase() {
        Instant start = Instant.now().plus(Duration.ofHours(1));
        Instant end = start.plus(Duration.ofHours(1));
        
        Meeting meeting = meetingService.propose(organizer, "Team Sync", "Weekly", start, end, List.of("inv"));
        
        assertNotNull(meeting.getId());
        assertEquals(2, meeting.getParticipants().size());
        assertFalse(meeting.isConfirmed());

        meetingService.respond(meeting.getId(), invitee, InviteStatus.ACCEPTED);
        
        Meeting updated = meetingRepository.findById(meeting.getId()).orElseThrow();
        assertTrue(updated.isConfirmed());
    }

    @Test
    void respond_OneDeclined_DoesNotConfirm() {
        User owner = userRepository.save(new User("owner", "o@e.com", "p"));
        User invitee = userRepository.save(new User("invitee", "i@e.com", "p"));
        Meeting meeting = meetingService.propose(owner, "Title", "Desc", Instant.now(), Instant.now().plusSeconds(3600), List.of("invitee"));

        meetingService.respond(meeting.getId(), invitee, InviteStatus.DECLINED);
        
        Meeting updated = meetingRepository.findById(meeting.getId()).orElseThrow();
        assertFalse(updated.isConfirmed());
    }

    @Test
    void calendarForIcalToken_Success() {
        User user = userRepository.save(new User("tokenuser", "t@e.com", "p"));
        String token = user.getIcalToken();
        
        List<Meeting> meetings = meetingService.calendarForIcalToken(token);
        assertNotNull(meetings);
    }

    @Test
    void calendarForIcalToken_InvalidToken_Throws() {
        assertThrows(IllegalArgumentException.class, () -> meetingService.calendarForIcalToken("invalid"));
    }
}
