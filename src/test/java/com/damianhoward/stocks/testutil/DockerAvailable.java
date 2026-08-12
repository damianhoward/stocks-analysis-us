package com.damianhoward.stocks.testutil;

import org.testcontainers.DockerClientFactory;

public final class DockerAvailable {

    private DockerAvailable() {}

    /** Used by JUnit's {@code @EnabledIf} to skip Testcontainers tests when no Docker daemon is reachable. */
    public static boolean check() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }
}
