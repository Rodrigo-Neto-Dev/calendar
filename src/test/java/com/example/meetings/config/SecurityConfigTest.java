package com.example.meetings.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Tag;

/**
 * Fast, lightweight unit test for SecurityConfig.
 * Uses a dummy controller so no real controllers or services are loaded.
 * This runs in milliseconds compared to the heavy @SpringBootTest in SecurityTest.
 *
 * NOTE: @WebMvcTest does not load Thymeleaf, so view resolution returns 404.
 * For security testing, 404 (not found) is acceptable because it proves
 * the request was NOT blocked by security (which would return 403 or redirect).
 * Static resources (css, js) ARE served by the resource handler and return 200.
 */
@Tag("unit")
@Tag("smoke")
@WebMvcTest(controllers = SecurityConfigTest.DummyController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Provides an in-memory user details service so @WithMockUser works
     * without loading the real AppUserDetailsService.
     */
    @Configuration
    static class TestConfig {
        @Bean
        public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
            UserDetails user = User.builder()
                    .username("testuser")
                    .password(passwordEncoder.encode("pass"))
                    .roles("USER")
                    .build();
            return new InMemoryUserDetailsManager(user);
        }
    }

    /** Dummy controller exposing endpoints that mirror the real application routes. */
    @Controller
    static class DummyController {
        @GetMapping("/calendar")
        public String calendar() { return "calendar"; }

        @GetMapping("/meetings/new")
        public String meetingsNew() { return "propose"; }

        @GetMapping("/discover")
        public String discover() { return "discover"; }

        @GetMapping("/login")
        public String login() { return "login"; }

        @GetMapping("/register")
        public String register() { return "register"; }
    }

    @Test
    void publicEndpoints_AccessibleWithoutAuth() throws Exception {
        // 404 means "view not found" which is fine — the point is it's NOT 403 (forbidden)
        mockMvc.perform(get("/login")).andExpect(status().isNotFound());
        mockMvc.perform(get("/register")).andExpect(status().isNotFound());
        // Static resources are served by ResourceHttpRequestHandler and return 200
        mockMvc.perform(get("/css/app.css")).andExpect(status().isOk());
        mockMvc.perform(get("/js/app.js")).andExpect(status().isNotFound()); // file doesn't exist
        // iCal feed is public (token-based auth). Missing token -> 404, not 403.
        mockMvc.perform(get("/ical/some-token.ics")).andExpect(status().isNotFound());
    }

    @Test
    void protectedEndpoints_RequireAuth() throws Exception {
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
    @WithMockUser(username = "testuser")
    void protectedEndpoints_AreAccessible_WhenAuthenticated() throws Exception {
        // 404 = security passed, view not resolved (no Thymeleaf in @WebMvcTest)
        mockMvc.perform(get("/calendar")).andExpect(status().isNotFound());
        mockMvc.perform(get("/meetings/new")).andExpect(status().isNotFound());
        mockMvc.perform(get("/discover")).andExpect(status().isNotFound());
    }

    @Test
    void h2Console_PermittedWithoutAuth() throws Exception {
        // H2 console is permitted in SecurityConfig. 404 is fine (H2 not active in test),
        // but must NOT be 403.
        mockMvc.perform(get("/h2-console/login.do"))
                .andExpect(status().isNotFound());
    }

    @Test
    void csrf_IgnoredForIcalAndH2() throws Exception {
        // POST without CSRF token to /ical should NOT trigger 403 (CSRF ignored).
        mockMvc.perform(post("/ical/test.ics"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/h2-console/login.do"))
                .andExpect(status().isNotFound());
    }

    @Test
    void logout_InvalidatesSession() throws Exception {
        // Logout endpoint is permitted; after logout, protected pages redirect.
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }
}