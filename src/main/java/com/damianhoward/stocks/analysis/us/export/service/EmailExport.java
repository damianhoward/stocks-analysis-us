package com.damianhoward.stocks.analysis.us.export.service;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;

@Component
public class EmailExport {

    private static final Logger log = LoggerFactory.getLogger(EmailExport.class);

    @Value("${stocks.analysis.us.email.enabled:false}")
    private boolean enabled;

    @Value("${stocks.analysis.us.email.host:}")
    private String host;

    @Value("${stocks.analysis.us.email.port:587}")
    private int port;

    @Value("${stocks.analysis.us.email.username:}")
    private String username;

    @Value("${stocks.analysis.us.email.password:}")
    private String password;

    @Value("${stocks.analysis.us.email.from:}")
    private String from;

    @Value("${stocks.analysis.us.email.from-name:}")
    private String fromName;

    @Value("${stocks.analysis.us.email.to:}")
    private String to;

    @Value("${stocks.analysis.us.email.to-name:}")
    private String toName;

    public void emailExport(LocalDate date, String attachmentName, String fileName, String attachmentPath) {
        if (!enabled) {
            log.info("Email export disabled; generated report remains at {}", attachmentPath);
            return;
        }
        validateConfiguration();

        try {
            log.info("Sending email with attachment {}", attachmentPath);
            String filePath = new File(attachmentPath).getAbsolutePath();

            EmailAttachment attachment = new EmailAttachment();
            attachment.setDescription(attachmentName);
            attachment.setName(fileName);
            attachment.setPath(filePath);
            attachment.setDisposition(EmailAttachment.ATTACHMENT);

            MultiPartEmail email = new MultiPartEmail();
            email.setStartTLSEnabled(true);
            email.setHostName(host);
            email.setSmtpPort(port);
            email.addTo(to, toName);
            email.setFrom(from, fromName);
            email.setSubject("Stock Analysis " + date);
            email.setMsg("Stock analysis for "+date);
            email.setAuthenticator(new DefaultAuthenticator(username, password));

            email.attach(attachment);
            email.send();

            log.info("Completed sending email with attachment {}", attachmentPath);
        } catch (EmailException e) {
            throw new RuntimeException(e);
        }
    }

    private void validateConfiguration() {
        if (isBlank(host) || isBlank(username) || isBlank(password) || isBlank(from) || isBlank(to)) {
            throw new IllegalStateException("Email export is enabled but SMTP configuration is incomplete");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
