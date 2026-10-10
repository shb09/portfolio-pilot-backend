package com.portfoliopilot.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandoffStoreTest {

    private final HandoffStore store = new HandoffStore();

    @Test
    void consumeOnceThenGone() {
        String code = store.create("asha@test.com");

        assertEquals("asha@test.com", store.consume(code).orElseThrow());
        assertTrue(store.consume(code).isEmpty(), "replay must fail");
    }

    @Test
    void unknownCodeEmpty() {
        assertTrue(store.consume("nope").isEmpty());
        assertTrue(store.consume(null).isEmpty());
        assertTrue(store.consume("  ").isEmpty());
    }
}
