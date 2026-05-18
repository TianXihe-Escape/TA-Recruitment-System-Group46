package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Small Java2D icons used by Swing buttons and navigation items.
 *
 * These icons are drawn procedurally with Java2D instead of loading image
 * assets, which keeps the application lightweight and makes it easy to reuse
 * the same icon shapes at different sizes and colors.
 */
public class SimpleLineIcon implements Icon {
    /**
     * Supported icon shapes rendered by this helper.
     *
     * Each enum value maps to one branch inside paintIcon where the matching
     * Java2D lines, arcs, or paths are drawn.
     */
    public enum Type {
        USER,
        BRIEFCASE,
        STAR,
        DOCUMENT,
        BELL,
        SEARCH,
        LOGOUT,
        TRASH,
        SAVE,
        REFRESH,
        EYE,
        SEND,
        EDIT,
        FILE,
        CHECK
    }

    /**
     * Which symbolic shape should be drawn for this icon instance.
     */
    private final Type type;
    /**
     * Square icon size in pixels.
     */
    private final int size;
    /**
     * Foreground stroke color used while drawing the icon.
     */
    private final Color color;

    /**
     * Creates an icon with the default 18px size.
     */
    public SimpleLineIcon(Type type, Color color) {
        this(type, 18, color);
    }

    /**
     * Creates an icon with a caller-provided size and color.
     */
    public SimpleLineIcon(Type type, int size, Color color) {
        this.type = type;
        this.size = size;
        this.color = color;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        // A copied Graphics2D context keeps the icon's rendering hints and
        // stroke settings isolated from any parent component painting logic.
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Stroke width scales with icon size so the line art stays readable
        // without looking too thin on larger buttons or too heavy on small ones.
        g.setStroke(new BasicStroke(Math.max(1.5f, size / 11f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(color);

        // Most shapes are positioned from the same width/height and centre-point
        // references so every icon variant stays visually centered in its box.
        int w = size;
        int h = size;
        int cx = x + w / 2;
        int cy = y + h / 2;
        switch (type) {
            case USER -> {
                // Head plus shoulder arc.
                g.drawOval(x + w / 3, y + h / 6, w / 3, h / 3);
                g.drawArc(x + w / 5, y + h / 2, w * 3 / 5, h / 2, 20, 140);
            }
            case BRIEFCASE -> {
                // Briefcase body with handle and centre seam.
                g.drawRoundRect(x + w / 8, y + h / 3, w * 3 / 4, h / 2, 3, 3);
                g.drawLine(x + w / 3, y + h / 3, x + w / 3, y + h / 4);
                g.drawLine(x + w * 2 / 3, y + h / 3, x + w * 2 / 3, y + h / 4);
                g.drawLine(x + w / 3, y + h / 4, x + w * 2 / 3, y + h / 4);
                g.drawLine(x + w / 8, y + h / 2, x + w * 7 / 8, y + h / 2);
            }
            case STAR -> {
                // Ten points alternate between outer and inner radius to form a star.
                Path2D path = new Path2D.Double();
                for (int i = 0; i < 10; i++) {
                    double angle = Math.toRadians(-90 + i * 36);
                    double radius = i % 2 == 0 ? w * 0.42 : w * 0.18;
                    double px = cx + Math.cos(angle) * radius;
                    double py = cy + Math.sin(angle) * radius;
                    if (i == 0) {
                        path.moveTo(px, py);
                    } else {
                        path.lineTo(px, py);
                    }
                }
                path.closePath();
                g.draw(path);
            }
            case DOCUMENT, FILE -> {
                // Rounded document outline with a few internal text lines.
                g.drawRoundRect(x + w / 4, y + h / 8, w / 2, h * 3 / 4, 3, 3);
                g.drawLine(x + w / 3, y + h / 3, x + w * 2 / 3, y + h / 3);
                g.drawLine(x + w / 3, y + h / 2, x + w * 2 / 3, y + h / 2);
                g.drawLine(x + w / 3, y + h * 2 / 3, x + w / 2, y + h * 2 / 3);
            }
            case BELL -> {
                // Bell dome, sides, base, and clapper hint.
                g.drawArc(x + w / 4, y + h / 5, w / 2, h / 2, 0, 180);
                g.drawLine(x + w / 4, y + h / 2, x + w / 5, y + h * 3 / 4);
                g.drawLine(x + w * 3 / 4, y + h / 2, x + w * 4 / 5, y + h * 3 / 4);
                g.drawLine(x + w / 5, y + h * 3 / 4, x + w * 4 / 5, y + h * 3 / 4);
                g.drawArc(x + w * 2 / 5, y + h * 3 / 4, w / 5, h / 7, 180, 180);
            }
            case SEARCH -> {
                // Magnifying glass circle plus handle.
                g.drawOval(x + w / 6, y + h / 6, w / 2, h / 2);
                g.drawLine(x + w * 3 / 5, y + h * 3 / 5, x + w * 5 / 6, y + h * 5 / 6);
            }
            case LOGOUT -> {
                // Outward arrow with a partial enclosing arc.
                g.drawLine(x + w / 6, cy, x + w * 3 / 4, cy);
                g.drawLine(x + w * 3 / 4, cy, x + w * 3 / 5, y + h / 3);
                g.drawLine(x + w * 3 / 4, cy, x + w * 3 / 5, y + h * 2 / 3);
                g.drawArc(x + w / 8, y + h / 5, w / 2, h * 3 / 5, 90, 180);
            }
            case TRASH -> {
                // Bin lid, sides, and base.
                g.drawLine(x + w / 4, y + h / 4, x + w * 3 / 4, y + h / 4);
                g.drawLine(x + w / 3, y + h / 4, x + w / 3, y + h * 4 / 5);
                g.drawLine(x + w * 2 / 3, y + h / 4, x + w * 2 / 3, y + h * 4 / 5);
                g.drawLine(x + w / 3, y + h * 4 / 5, x + w * 2 / 3, y + h * 4 / 5);
                g.drawLine(x + w * 2 / 5, y + h / 6, x + w * 3 / 5, y + h / 6);
            }
            case SAVE -> {
                // Floppy-like outline with top notch and lower label line.
                g.drawRoundRect(x + w / 6, y + h / 6, w * 2 / 3, h * 2 / 3, 3, 3);
                g.drawLine(x + w / 3, y + h / 6, x + w / 3, y + h / 3);
                g.drawLine(x + w / 3, y + h / 3, x + w * 2 / 3, y + h / 3);
                g.drawLine(x + w / 3, y + h * 2 / 3, x + w * 2 / 3, y + h * 2 / 3);
            }
            case REFRESH -> {
                // Circular refresh arc with a short arrow head.
                g.drawArc(x + w / 5, y + h / 5, w * 3 / 5, h * 3 / 5, 35, 270);
                g.drawLine(x + w * 4 / 5, y + h / 4, x + w * 4 / 5, y + h / 2);
                g.drawLine(x + w * 4 / 5, y + h / 4, x + w * 3 / 5, y + h / 4);
            }
            case EYE -> {
                // Outer eye contour plus pupil.
                g.drawArc(x + w / 8, y + h / 4, w * 3 / 4, h / 2, 20, 140);
                g.drawArc(x + w / 8, y + h / 4, w * 3 / 4, h / 2, 200, 140);
                g.drawOval(cx - w / 10, cy - h / 10, w / 5, h / 5);
            }
            case SEND -> {
                // Paper-plane style shape built from a simple polygon path.
                Path2D path = new Path2D.Double();
                path.moveTo(x + w / 6.0, y + h / 5.0);
                path.lineTo(x + w * 5 / 6.0, cy);
                path.lineTo(x + w / 6.0, y + h * 4 / 5.0);
                path.lineTo(x + w / 3.0, cy);
                path.closePath();
                g.draw(path);
            }
            case EDIT -> {
                // Pencil-like diagonal body with a short tip section.
                g.drawLine(x + w / 4, y + h * 3 / 4, x + w * 3 / 4, y + h / 4);
                g.drawLine(x + w * 2 / 3, y + h / 5, x + w * 4 / 5, y + h / 3);
                g.drawLine(x + w / 5, y + h * 4 / 5, x + w / 3, y + h * 3 / 4);
            }
            case CHECK -> {
                // Two-stroke checkmark.
                g.drawLine(x + w / 5, cy, x + w * 2 / 5, y + h * 3 / 4);
                g.drawLine(x + w * 2 / 5, y + h * 3 / 4, x + w * 4 / 5, y + h / 4);
            }
        }
        g.dispose();
    }
}
