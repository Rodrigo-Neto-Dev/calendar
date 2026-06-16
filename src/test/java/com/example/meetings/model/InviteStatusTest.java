package com.example.meetings.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Tag;

/**
 * Unit tests for the InviteStatus enum.
 * Defensive tests ensuring the enum contract is stable.
 */
@Tag("unit")
class InviteStatusTest {

    @Test
    void enum_HasThreeValues() {
        InviteStatus[] values = InviteStatus.values();
        assertEquals(3, values.length);
        assertTrue(java.util.Arrays.asList(values).contains(InviteStatus.PENDING));
        assertTrue(java.util.Arrays.asList(values).contains(InviteStatus.ACCEPTED));
        assertTrue(java.util.Arrays.asList(values).contains(InviteStatus.DECLINED));
    }

    @Test
    void enum_OrderIsStable() {
        // Defensive: if someone reorders the enum, these ordinals would shift,
        // potentially breaking serialization or database mappings.
        assertEquals(0, InviteStatus.PENDING.ordinal());
        assertEquals(1, InviteStatus.ACCEPTED.ordinal());
        assertEquals(2, InviteStatus.DECLINED.ordinal());
    }

    @Test
    void enum_ValueOf_WorksForAll() {
        assertEquals(InviteStatus.PENDING, InviteStatus.valueOf("PENDING"));
        assertEquals(InviteStatus.ACCEPTED, InviteStatus.valueOf("ACCEPTED"));
        assertEquals(InviteStatus.DECLINED, InviteStatus.valueOf("DECLINED"));
    }

    @Test
    void enum_ValueOf_ThrowsForInvalid() {
        assertThrows(IllegalArgumentException.class, () -> InviteStatus.valueOf("UNKNOWN"));
    }
}
