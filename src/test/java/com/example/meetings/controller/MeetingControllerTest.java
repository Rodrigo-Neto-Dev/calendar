package com.example.meetings.controller;

import com.example.meetings.model.User;
import com.example.meetings.service.MeetingService;
import com.example.meetings.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Tag;

@Tag("smoke")
@Tag("integration")
@WebMvcTest(MeetingController.class)
class MeetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MeetingService meetingService;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser
    void proposeForm_Accessible() throws Exception {
        mockMvc.perform(get("/meetings/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("propose"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void propose_Success() throws Exception {
        User user = new User("testuser", "test@example.com", "pass");
        when(userService.requireByUsername("testuser")).thenReturn(user);

        mockMvc.perform(post("/meetings/new")
                        .param("title", "Meeting")
                        .param("start", "2099-01-01T10:00")
                        .param("end", "2099-01-01T11:00")
                        .param("invitees", "other")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void propose_Failure_ShowsFormWithError() throws Exception {
        User user = new User("testuser", "test@example.com", "pass");
        when(userService.requireByUsername("testuser")).thenReturn(user);
        when(meetingService.propose(any(), anyString(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Invalid time"));

        mockMvc.perform(post("/meetings/new")
                        .param("title", "Meeting")
                        .param("start", "2099-01-01T11:00")
                        .param("end", "2099-01-01T10:00")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Invalid time"))
                .andExpect(view().name("propose"));
    }
}
