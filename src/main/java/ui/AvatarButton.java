package ui;

import javax.swing.*;
import java.awt.*;

/**
 * Circular user avatar button for account menus.
 *
 * The button draws a lightweight initials-based avatar instead of depending on
 * uploaded profile pictures, which keeps account menus visually distinctive
 * without introducing image loading or storage requirements.
 */
public class AvatarButton extends JButton {
    /**
     * Short label rendered inside the circular avatar, typically one or two initials.
     */
    private String initials;

    /**
     * Creates a fixed-size circular avatar button.
     *
     * The button is paint-driven, so its visible appearance comes from
     * paintComponent rather than Swing's default content-area rendering.
     */
    public AvatarButton(String initials) {
        this.initials = sanitize(initials);
        setPreferredSize(new Dimension(46, 46));
        setMinimumSize(new Dimension(46, 46));
        setMaximumSize(new Dimension(46, 46));
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("Account and profile settings");
    }

    /**
     * Replaces the displayed initials and repaints the avatar immediately.
     */
    public void setInitials(String initials) {
        this.initials = sanitize(initials);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        // A copied Graphics2D context keeps custom rendering isolated from any
        // parent painter state and allows clean disposal after drawing finishes.
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int diameter = Math.min(getWidth(), getHeight()) - 4;
        int x = (getWidth() - diameter) / 2;
        int y = (getHeight() - diameter) / 2;

        // The diagonal gradient makes the avatar feel more like an intentional
        // UI element than a flat colored badge.
        GradientPaint paint = new GradientPaint(
                x, y, new Color(47, 111, 237),
                x + diameter, y + diameter, new Color(20, 184, 166)
        );
        g.setPaint(paint);
        g.fillOval(x, y, diameter, diameter);
        // A subtle outline helps the avatar edge stay visible on light surfaces.
        g.setColor(new Color(255, 255, 255, 120));
        g.drawOval(x, y, diameter - 1, diameter - 1);

        // Slightly larger type is used for single-character initials so the
        // avatar stays visually balanced regardless of name length.
        g.setFont(UiTheme.uiFont(Font.BOLD, initials.length() > 1 ? 13 : 16));
        FontMetrics metrics = g.getFontMetrics();
        int textX = getWidth() / 2 - metrics.stringWidth(initials) / 2;
        int textY = getHeight() / 2 + (metrics.getAscent() - metrics.getDescent()) / 2;
        g.setColor(Color.WHITE);
        g.drawString(initials, textX, textY);
        g.dispose();
    }

    /**
     * Normalises any provided avatar label into a compact uppercase initials string.
     *
     * Blank values fall back to a safe default so the button always has visible text.
     */
    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "TA";
        }
        String trimmed = value.trim();
        return trimmed.length() <= 2 ? trimmed.toUpperCase() : trimmed.substring(0, 2).toUpperCase();
    }
}
