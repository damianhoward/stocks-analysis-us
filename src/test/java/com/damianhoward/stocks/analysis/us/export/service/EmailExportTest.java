package com.damianhoward.stocks.analysis.us.export.service;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.MailSendException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the export inside Spring Boot's mail auto-configuration, so the tests cover how the
 * {@code spring.mail.*} and {@code stocks.analysis.us.email.*} properties reach the sender, not
 * only the sending itself.
 */
class EmailExportTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    private static final LocalDate DATE = LocalDate.of(2026, 6, 26);

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
            .withUserConfiguration(EmailExport.class);

    private static String[] addresses() {
        return new String[] {
                "stocks.analysis.us.email.from=from@example.com",
                "stocks.analysis.us.email.from-name=Sender",
                "stocks.analysis.us.email.to=to@example.com",
                "stocks.analysis.us.email.to-name=Recipient"};
    }

    @Test
    void disabledExportSendsNothingEvenWithoutARelay() {
        context.withPropertyValues("stocks.analysis.us.email.enabled=false")
                .run(ctx -> ctx.getBean(EmailExport.class).emailExport(DATE, Path.of("unused.xls")));
    }

    @Test
    void enabledWithoutAHostRefusesToRun() {
        context.withPropertyValues("stocks.analysis.us.email.enabled=true")
                .withPropertyValues(addresses())
                .run(ctx -> assertThrows(IllegalStateException.class,
                        () -> ctx.getBean(EmailExport.class).emailExport(DATE, Path.of("unused.xls"))));
    }

    @Test
    void enabledWithoutARecipientRefusesToRun() {
        context.withPropertyValues(
                        "stocks.analysis.us.email.enabled=true",
                        "stocks.analysis.us.email.from=from@example.com",
                        "spring.mail.host=127.0.0.1")
                .run(ctx -> assertThrows(IllegalStateException.class,
                        () -> ctx.getBean(EmailExport.class).emailExport(DATE, Path.of("unused.xls"))));
    }

    @Test
    void sendsTheReportAsAnAttachment(@TempDir Path tempDir) throws Exception {
        greenMail.setUser("user", "password");
        Path report = Files.writeString(tempDir.resolve("report.xls"), "report-bytes");

        context.withPropertyValues(
                        "stocks.analysis.us.email.enabled=true",
                        "spring.mail.host=127.0.0.1",
                        "spring.mail.port=" + greenMail.getSmtp().getPort(),
                        "spring.mail.username=user",
                        "spring.mail.password=password",
                        "spring.mail.properties.mail.smtp.auth=true")
                .withPropertyValues(addresses())
                .run(ctx -> ctx.getBean(EmailExport.class).emailExport(DATE, report));

        assertTrue(greenMail.waitForIncomingEmail(5000, 1), "email should be received");
        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);
        MimeMessage message = received[0];
        assertEquals("Stock Analysis " + DATE, message.getSubject());
        assertEquals("Sender <from@example.com>", message.getFrom()[0].toString());
        assertEquals("Recipient <to@example.com>", message.getAllRecipients()[0].toString());
        assertTrue(GreenMailUtil.getBody(message).contains("Stock analysis for " + DATE));

        MimeMultipart parts = (MimeMultipart) message.getContent();
        Part attachment = parts.getBodyPart(1);
        assertEquals(Part.ATTACHMENT, attachment.getDisposition());
        assertEquals("report.xls", attachment.getFileName());
        assertEquals("report-bytes", new String(attachment.getInputStream().readAllBytes()));
    }

    @Test
    void anUnreachableRelaySurfacesAsASendFailure(@TempDir Path tempDir) throws Exception {
        Path report = Files.writeString(tempDir.resolve("report.xls"), "report-bytes");

        context.withPropertyValues(
                        "stocks.analysis.us.email.enabled=true",
                        "spring.mail.host=invalid.host.invalid",
                        "spring.mail.port=2")
                .withPropertyValues(addresses())
                .run(ctx -> assertThrows(MailSendException.class,
                        () -> ctx.getBean(EmailExport.class).emailExport(DATE, report)));
    }
}
