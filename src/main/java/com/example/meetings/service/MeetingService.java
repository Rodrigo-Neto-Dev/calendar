package com.example.meetings.service;

import com.example.meetings.model.InviteStatus;
import com.example.meetings.model.Meeting;
import com.example.meetings.model.MeetingParticipant;
import com.example.meetings.model.User;
import com.example.meetings.repository.MeetingParticipantRepository;
import com.example.meetings.repository.MeetingRepository;
import com.example.meetings.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final UserRepository userRepository;

    public MeetingService(MeetingRepository meetingRepository,
                          MeetingParticipantRepository participantRepository,
                          UserRepository userRepository) {
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Meeting propose(User organizer, String title, String description,
                           Instant start, Instant end, List<String> inviteeUsernames) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        Meeting meeting = new Meeting(title, description, start, end, organizer);

        // Organizer auto-accepts; the slot is blocked on their calendar immediately.
        meeting.addParticipant(new MeetingParticipant(meeting, organizer, InviteStatus.ACCEPTED));

        Set<String> seen = new HashSet<>();
        seen.add(organizer.getUsername());
        for (String username : inviteeUsernames) {
            String normalized = username == null ? "" : username.trim();
            if (normalized.isEmpty() || !seen.add(normalized)) continue;
            User invitee = userRepository.findByUsername(normalized)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown invitee: " + normalized));
            meeting.addParticipant(new MeetingParticipant(meeting, invitee, InviteStatus.PENDING));
        }

        return meetingRepository.save(meeting);
    }

    public List<Meeting> calendarFor(User user) {
        return meetingRepository.findCalendarMeetings(user);
    }

    public List<MeetingParticipant> pendingInvitesFor(User user) {
        return participantRepository.findByUserAndStatus(user, InviteStatus.PENDING);
    }

    @Transactional
    public void respond(Long meetingId, User user, InviteStatus status) {
        if (status != InviteStatus.ACCEPTED && status != InviteStatus.DECLINED) {
            throw new IllegalArgumentException("Response must be ACCEPTED or DECLINED");
        }
        MeetingParticipant participant = participantRepository
                .findByMeetingIdAndUserId(meetingId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No invite found for this user"));
        participant.setStatus(status);
    }

    /** Used by the iCal feed; declined invites are filtered out elsewhere. */
    public List<Meeting> calendarForIcalToken(String token) {
        User user = userRepository.findByIcalToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid iCal token"));
        return new ArrayList<>(calendarFor(user));
    }
}
