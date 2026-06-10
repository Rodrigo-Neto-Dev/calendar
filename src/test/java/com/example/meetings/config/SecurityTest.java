package com.example.meetings.config;

import com.example.meetings.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Tag;

@Tag("smoke")
@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Test
    void publicEndpoints_AreAccessibleByAnonymous() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
        mockMvc.perform(get("/css/app.css")).andExpect(status().isOk());
        mockMvc.perform(get("/ical/some-token.ics")).andExpect(status().isNotFound()); // NotFound because token doesn't exist, but NOT 403
    }

    @Test
    void protectedEndpoints_RedirectToLogin_WhenAnonymous() throws Exception {
        mockMvc.perform(get("/calendar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/meetings/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/discover"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "authuser")
    void protectedEndpoints_AreAccessible_WhenAuthenticated() throws Exception {
        // Create the user so requireByUsername doesn't fail
        try {
            userService.register("authuser", "auth@example.com", "pass");
        } catch (IllegalArgumentException e) {
            // Ignore if already exists from another test run if not using @Transactional
        }
        
        mockMvc.perform(get("/calendar")).andExpect(status().isOk());
        mockMvc.perform(get("/meetings/new")).andExpect(status().isOk());
        mockMvc.perform(get("/discover")).andExpect(status().isOk());
    }
}
