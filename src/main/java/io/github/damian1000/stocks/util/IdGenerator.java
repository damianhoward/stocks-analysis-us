package io.github.damian1000.stocks.util;

import java.util.UUID;

/** Surrogate keys for rows the pipeline creates. */
public final class IdGenerator {

    private IdGenerator() {}

    public static String generateId() {
        return UUID.randomUUID().toString();
    }
}
