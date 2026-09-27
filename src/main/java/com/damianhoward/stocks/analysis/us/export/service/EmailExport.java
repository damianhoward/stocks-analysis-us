package com.damianhoward.stocks.analysis.us.export.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.time.LocalDate;

@Component
@EnableConfigurationProperties(EmailSettings.class)
public class EmailExport {

    private static final Logger log = LoggerFactory.getLogger(EmailExport.class);

    // Spring Boot only defines a sender when spring.mail.host is configured, so its absence
    // is how an enabled export with no SMTP relay is told apart from a working one.
    private final ObjectProvider<JavaMailSender> mailSender;
    private final EmailSettings settings;

    public EmailExport(ObjectProvider<JavaMailSender> mailSender, EmailSettings settings) {
        this.mailSender = mailSender;
        this.settings = settings;
    }

    public void emailExport(LocalDate date, Path report) {
        if (!settings.enabled()) {
            log.info("Email export disabled; generated report remains at {}", report);
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || isBlank(settings.from()) || isBlank(settings.to())) {
            throw new IllegalStateException("Email export is enabled but SMTP configuration is incomplete");
        }

        log.info("Sending email with attachment {}", report);
        sender.send(compose(sender.createMimeMessage(), date, report));
        log.info("Completed sending email with attachment {}", report);
    }

    private MimeMessage compose(MimeMessage message, LocalDate date, Path report) {
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(settings.from(), settings.fromName());
            helper.setTo(new InternetAddress(settings.to(), settings.toName()));
            helper.setSubject("Stock Analysis " + date);
            helper.setText("Stock analysis for " + date);
            helper.addAttachment(report.getFileName().toString(), report.toFile());
            return message;
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailPreparationException("Could not build the export email for " + date, e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
