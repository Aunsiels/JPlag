package de.jplag.cli;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.*;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import javax.swing.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import de.jplag.cli.server.ReportViewer;

@Timeout(value = 5, unit = TimeUnit.MINUTES)
class ReportViewerTest {
    @Test
    void testStartViewer() throws Exception {
        assumeTrue(Desktop.isDesktopSupported());
        ReportViewer viewer = new ReportViewer();

        int port = viewer.start();
        Desktop.getDesktop().browse(URI.create("http://localhost:" + port));

        // Open Dialog to keep the test running
        JOptionPane.showMessageDialog(null, "Press OK to stop the server");
        viewer.stop();
    }

}