package com.example.meetings.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class UserTest {

    @Test
    void constructor_GeneratesUniqueIcalToken() {
        User user1 = new User("alice", "alice@example.com", "hash1");
        User user2 = new User("bob", "bob@example.com", "hash2");

        assertNotNull(user1.getIcalToken());
        assertNotNull(user2.getIcalToken());
        assertNotEquals(user1.getIcalToken(), user2.getIcalToken());
        assertEquals("alice", user1.getUsername());
        assertEquals("alice@example.com", user1.getEmail());
        assertEquals("hash1", user1.getPasswordHash());
    }

    @Test
    void gettersAndSetters_WorkCorrectly() {
        User user = new User("alice", "alice@example.com", "hash");
        user.setId(123L);
        user.setEmail("new@example.com");
        user.setPasswordHash("newhash");

        assertEquals(123L, user.getId());
        assertEquals("new@example.com", user.getEmail());
        assertEquals("newhash", user.getPasswordHash());
    }
}
