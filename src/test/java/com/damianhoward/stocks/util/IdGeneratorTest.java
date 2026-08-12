package com.damianhoward.stocks.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IdGeneratorTest {

    @Test
    void generatedIdsAreUuidsAndUnique() {
        String first = IdGenerator.generateId();
        String second = IdGenerator.generateId();
        assertNotNull(first);
        // UUID.fromString throws if it doesn't parse — confirms format
        assertDoesNotThrow(() -> UUID.fromString(first));
        assertNotEquals(first, second);
    }
}
