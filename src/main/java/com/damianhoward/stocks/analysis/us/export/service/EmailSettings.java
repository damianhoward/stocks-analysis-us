package com.damianhoward.stocks.analysis.us.export.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Who the export email is from and to. The SMTP connection itself is Spring Boot's
 * {@code spring.mail.*}, so this carries only what the mail sender does not know.
 */
@ConfigurationProperties("stocks.analysis.us.email")
public record EmailSettings(boolean enabled, String from, String fromName, String to, String toName) {
}
