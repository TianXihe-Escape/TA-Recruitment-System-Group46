import model.User;
import service.DataService;
import ui.AdminDashboardFrame;
import ui.LoginFrame;
import ui.MOManagementFrame;
import ui.TADashboardFrame;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Renders the main application frames to PNG files for documentation.
 */
public class CaptureScreenshots {
    private static final Path OUTPUT_DIR = Paths.get("docs", "screenshots");

    public static void main(String[] args) throws Exception {
        DataService dataService = new DataService();
        dataService.ensureDataFiles();
        Files.createDirectories(OUTPUT_DIR);

        User taUser = dataService.getUserRepository()
                .findByUsername("ta1@bupt.edu.cn")
                .orElseThrow(() -> new IllegalStateException("TA demo user not found."));
        User moUser = dataService.getUserRepository()
                .findByUsername("mo1@bupt.edu.cn")
                .orElseThrow(() -> new IllegalStateException("MO demo user not found."));
        User adminUser = dataService.getUserRepository()
                .findByUsername("admin@bupt.edu.cn")
                .orElseThrow(() -> new IllegalStateException("Admin demo user not found."));

        capture(new LoginFrame(dataService), OUTPUT_DIR.resolve("login-frame.png"));
        capture(new TADashboardFrame(dataService, taUser), OUTPUT_DIR.resolve("ta-dashboard.png"));
        capture(new MOManagementFrame(dataService, moUser), OUTPUT_DIR.resolve("mo-dashboard.png"));
        capture(new AdminDashboardFrame(dataService, adminUser), OUTPUT_DIR.resolve("admin-dashboard.png"));
    }

    private static void capture(JFrame frame, Path outputPath) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame.setVisible(true);
            frame.validate();
            frame.repaint();
        });

        Thread.sleep(300);

        SwingUtilities.invokeAndWait(() -> {
            BufferedImage image = new BufferedImage(
                    frame.getWidth(),
                    frame.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            frame.paint(graphics);
            graphics.dispose();
            try {
                ImageIO.write(image, "png", outputPath.toFile());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } finally {
                frame.dispose();
            }
        });
    }
}
