package com.example.meetings.service;

import com.example.meetings.discover.DiscoveredEvent;
import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import com.example.meetings.repository.MeetingParticipantRepository;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Tag;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingParticipantRepository participantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MeetingService meetingService;

    private User organizer;
    private User invitee;
    private Instant start;
    private Instant end;

    @BeforeEach
    void setUp() {
        organizer = new User("organizer", "org@example.com", "pass");
        invitee = new User("invitee", "inv@example.com", "pass");
        start = Instant.now().plus(Duration.ofHours(1));
        end = start.plus(Duration.ofHours(1));
    }

    @Test
    void propose_Success() {
        when(userRepository.findByUsername("invitee")).thenReturn(Optional.of(invitee));
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Meeting meeting = meetingService.propose(organizer, "Title", "Desc", start, end, List.of("invitee"));

        assertNotNull(meeting);
        assertEquals(2, meeting.getParticipants().size());
        assertTrue(meeting.getParticipants().stream().anyMatch(p -> p.getUser().equals(organizer) && p.getStatus() == InviteStatus.ACCEPTED));
        assertTrue(meeting.getParticipants().stream().anyMatch(p -> p.getUser().equals(invitee) && p.getStatus() == InviteStatus.PENDING));
        verify(meetingRepository).save(any(Meeting.class));
    }

    @Test
    void propose_InvalidTime_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            meetingService.propose(organizer, "Title", "Desc", end, start, List.of("invitee"))
        );
    }

    @Test
    void propose_UnknownInvitee_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> 
            meetingService.propose(organizer, "Title", "Desc", start, end, List.of("unknown"))
        );
    }

    @Test
    void respond_Success() {
        Meeting meeting = new Meeting("Title", "Desc", start, end, organizer);
        MeetingParticipant participant = new MeetingParticipant(meeting, invitee, InviteStatus.PENDING);
        when(participantRepository.findByMeetingIdAndUserId(1L, invitee.getId())).thenReturn(Optional.of(participant));

        meetingService.respond(1L, invitee, InviteStatus.ACCEPTED);

        assertEquals(InviteStatus.ACCEPTED, participant.getStatus());
    }

    @Test
    void respond_InvalidStatus_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            meetingService.respond(1L, invitee, InviteStatus.PENDING)
        );
    }

    @Test
    void respond_NoInviteFound_ThrowsException() {
        when(participantRepository.findByMeetingIdAndUserId(1L, invitee.getId())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> 
            meetingService.respond(1L, invitee, InviteStatus.ACCEPTED)
        );
    }

    @Test
    void propose_OrganizerInvitesSelf_IsSkipped() {
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Meeting meeting = meetingService.propose(organizer, "Title", "Desc", start, end, List.of("organizer"));

        assertNotNull(meeting);
        assertEquals(1, meeting.getParticipants().size());
        assertTrue(meeting.getParticipants().stream().anyMatch(p -> p.getUser().equals(organizer) && p.getStatus() == InviteStatus.ACCEPTED));
    }

    @Test
    void propose_DuplicateInvitees_AreDeduplicated() {
        when(userRepository.findByUsername("invitee")).thenReturn(Optional.of(invitee));
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Meeting meeting = meetingService.propose(organizer, "Title", "Desc", start, end, List.of("invitee", "invitee", "   ", "invitee"));

        assertNotNull(meeting);
        assertEquals(2, meeting.getParticipants().size());
    }

    @Test
    void copyFromDiscovered_Success_DefaultDuration() {
        DiscoveredEvent event = new DiscoveredEvent("Source", "extId", "Event", "Desc", start, null, "url", "venue");
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Meeting meeting = meetingService.copyFromDiscovered(organizer, event);

        assertNotNull(meeting);
        assertEquals(start.plus(Duration.ofHours(2)), meeting.getEndTime());
        assertEquals(1, meeting.getParticipants().size());
        verify(meetingRepository).save(any(Meeting.class));
    }

    @Test
    void copyFromDiscovered_WithExplicitEndTime_Success() {
        Instant explicitEnd = start.plus(Duration.ofHours(5));
        DiscoveredEvent event = new DiscoveredEvent("Source", "extId", "Event", "Desc", start, explicitEnd, "url", "venue");
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Meeting meeting = meetingService.copyFromDiscovered(organizer, event);

        assertNotNull(meeting);
        assertEquals(explicitEnd, meeting.getEndTime());
        assertEquals(1, meeting.getParticipants().size());
    }
    @Test
    void propose_NullTitle_StillPersists() {
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Meeting meeting = meetingService.propose(organizer, null, "Desc", start, end, List.of());

        assertNotNull(meeting);
        assertNull(meeting.getTitle());
        assertEquals(1, meeting.getParticipants().size());
        verify(meetingRepository).save(any(Meeting.class));
    }

    @Test
    void propose_NullOrganizer_ThrowsNpe() {
        // Meeting constructor requires non-null organizer (ManyToOne optional=false)
        assertThrows(NullPointerException.class, () ->
                meetingService.propose(null, "Title", "Desc", start, end, List.of())
        );
    }

    @Test
    void propose_EmptyInviteeList_OnlyOrganizer() {
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Meeting meeting = meetingService.propose(organizer, "Title", "Desc", start, end, List.of());

        assertNotNull(meeting);
        assertEquals(1, meeting.getParticipants().size());
        assertTrue(meeting.getParticipants().stream()
                .anyMatch(p -> p.getUser().equals(organizer) && p.getStatus() == InviteStatus.ACCEPTED));
    }

    @Test
    void respond_NullStatus_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                meetingService.respond(1L, invitee, null)
        );
    }
}

