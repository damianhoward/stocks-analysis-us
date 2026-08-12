package com.damianhoward.stocks.analysis.us.export.service;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailExportTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Test
    void disabledExportIsNoOp() {
        // No SMTP needed; the export just logs and returns.
        EmailExport export = new EmailExport();
        ReflectionTestUtils.setField(export, "enabled", false);
        export.emailExport(LocalDate.now(), "name", "name.xls", "/tmp/name.xls");
    }

    @Test
    void enabledButIncompleteConfigThrows() {
        EmailExport export = new EmailExport();
        ReflectionTestUtils.setField(export, "enabled", true);
        // host/username/password/from/to all blank — must reject.
        ReflectionTestUtils.setField(export, "host", "");
        ReflectionTestUtils.setField(export, "username", "");
        ReflectionTestUtils.setField(export, "password", "");
        ReflectionTestUtils.setField(export, "from", "");
        ReflectionTestUtils.setField(export, "to", "");

        assertThrows(IllegalStateException.class,
                () -> export.emailExport(LocalDate.now(), "name", "name.xls", "/tmp/name.xls"));
    }

    @Test
    void enabledWithMissingSingleFieldStillThrows() {
        EmailExport export = new EmailExport();
        ReflectionTestUtils.setField(export, "enabled", true);
        ReflectionTestUtils.setField(export, "host", "smtp.example.com");
        ReflectionTestUtils.setField(export, "username", "user");
        ReflectionTestUtils.setField(export, "password", ""); // missing
        ReflectionTestUtils.setField(export, "from", "a@b.com");
        ReflectionTestUtils.setField(export, "to", "c@d.com");

        assertThrows(IllegalStateException.class,
                () -> export.emailExport(LocalDate.now(), "n", "n.xls", "/tmp/n.xls"));
    }

    @Test
    void enabledWithNullFieldIsTreatedAsBlank() {
        EmailExport export = new EmailExport();
        ReflectionTestUtils.setField(export, "enabled", true);
        ReflectionTestUtils.setField(export, "host", "smtp.example.com");
        ReflectionTestUtils.setField(export, "username", "user");
        ReflectionTestUtils.setField(export, "password", "pw");
        ReflectionTestUtils.setField(export, "from", "a@b.com");
        ReflectionTestUtils.setField(export, "to", null); // null, not empty

        assertThrows(IllegalStateException.class,
                () -> export.emailExport(LocalDate.now(), "n", "n.xls", "/tmp/n.xls"));
    }

    @Test
    void enabledSendsEmailWithAttachment(@TempDir Path tempDir) throws Exception {
        greenMail.setUser("user", "password");
        Path attachment = Files.writeString(tempDir.resolve("report.xls"), "report-bytes");

        EmailExport export = new EmailExport();
        ReflectionTestUtils.setField(export, "enabled", true);
        ReflectionTestUtils.setField(export, "host", "127.0.0.1");
        ReflectionTestUtils.setField(export, "port", greenMail.getSmtp().getPort());
        ReflectionTestUtils.setField(export, "username", "user");
        ReflectionTestUtils.setField(export, "password", "password");
        ReflectionTestUtils.setField(export, "from", "from@example.com");
        ReflectionTestUtils.setField(export, "fromName", "Sender");
        ReflectionTestUtils.setField(export, "to", "to@example.com");
        ReflectionTestUtils.setField(export, "toName", "Recipient");

        LocalDate date = LocalDate.of(2026, 6, 26);
        export.emailExport(date, "Stock report", "report.xls", attachment.toString());

        assertTrue(greenMail.waitForIncomingEmail(5000, 1), "email should be received");
        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);
        assertEquals("Stock Analysis " + date, received[0].getSubject());
        assertTrue(GreenMailUtil.getBody(received[0]).contains("Stock analysis for " + date));
    }

    @Test
    void wrapsEmailFailureFromUnreachableHost(@TempDir Path tempDir) throws Exception {
        Path attachment = Files.writeString(tempDir.resolve("report.xls"), "report-bytes");

        EmailExport export = new EmailExport();
        ReflectionTestUtils.setField(export, "enabled", true);
        // Configuration is complete but the host is bogus, so send() fails.
        ReflectionTestUtils.setField(export, "host", "invalid.host.invalid");
        ReflectionTestUtils.setField(export, "port", 2);
        ReflectionTestUtils.setField(export, "username", "user");
        ReflectionTestUtils.setField(export, "password", "password");
        ReflectionTestUtils.setField(export, "from", "from@example.com");
        ReflectionTestUtils.setField(export, "to", "to@example.com");

        assertThrows(RuntimeException.class,
                () -> export.emailExport(LocalDate.now(), "Stock report", "report.xls", attachment.toString()));
    }
}
