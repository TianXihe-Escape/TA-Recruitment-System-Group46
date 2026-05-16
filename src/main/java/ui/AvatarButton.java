package ui;

import javax.swing.*;
import java.awt.*;

/**
 * Circular user avatar button for account menus.
 */
public class AvatarButton extends JButton {
    private String initials;

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

    public void setInitials(String initials) {
        this.initials = sanitize(initials);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int diameter = Math.min(getWidth(), getHeight()) - 4;
        int x = (getWidth() - diameter) / 2;
        int y = (getHeight() - diameter) / 2;

        GradientPaint paint = new GradientPaint(
                x, y, new Color(47, 111, 237),
                x + diameter, y + diameter, new Color(20, 184, 166)
        );
        g.setPaint(paint);
        g.fillOval(x, y, diameter, diameter);
        g.setColor(new Color(255, 255, 255, 120));
        g.drawOval(x, y, diameter - 1, diameter - 1);

        g.setFont(UiTheme.uiFont(Font.BOLD, initials.length() > 1 ? 13 : 16));
        FontMetrics metrics = g.getFontMetrics();
        int textX = getWidth() / 2 - metrics.stringWidth(initials) / 2;
        int textY = getHeight() / 2 + (metrics.getAscent() - metrics.getDescent()) / 2;
        g.setColor(Color.WHITE);
        g.drawString(initials, textX, textY);
        g.dispose();
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "TA";
        }
        String trimmed = value.trim();
        return trimmed.length() <= 2 ? trimmed.toUpperCase() : trimmed.substring(0, 2).toUpperCase();
    }
}
