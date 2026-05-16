package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Small Java2D icons used by Swing buttons and navigation items.
 */
public class SimpleLineIcon implements Icon {
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

    private final Type type;
    private final int size;
    private final Color color;

    public SimpleLineIcon(Type type, Color color) {
        this(type, 18, color);
    }

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
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(Math.max(1.5f, size / 11f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(color);

        int w = size;
        int h = size;
        int cx = x + w / 2;
        int cy = y + h / 2;
        switch (type) {
            case USER -> {
                g.drawOval(x + w / 3, y + h / 6, w / 3, h / 3);
                g.drawArc(x + w / 5, y + h / 2, w * 3 / 5, h / 2, 20, 140);
            }
            case BRIEFCASE -> {
                g.drawRoundRect(x + w / 8, y + h / 3, w * 3 / 4, h / 2, 3, 3);
                g.drawLine(x + w / 3, y + h / 3, x + w / 3, y + h / 4);
                g.drawLine(x + w * 2 / 3, y + h / 3, x + w * 2 / 3, y + h / 4);
                g.drawLine(x + w / 3, y + h / 4, x + w * 2 / 3, y + h / 4);
                g.drawLine(x + w / 8, y + h / 2, x + w * 7 / 8, y + h / 2);
            }
            case STAR -> {
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
                g.drawRoundRect(x + w / 4, y + h / 8, w / 2, h * 3 / 4, 3, 3);
                g.drawLine(x + w / 3, y + h / 3, x + w * 2 / 3, y + h / 3);
                g.drawLine(x + w / 3, y + h / 2, x + w * 2 / 3, y + h / 2);
                g.drawLine(x + w / 3, y + h * 2 / 3, x + w / 2, y + h * 2 / 3);
            }
            case BELL -> {
                g.drawArc(x + w / 4, y + h / 5, w / 2, h / 2, 0, 180);
                g.drawLine(x + w / 4, y + h / 2, x + w / 5, y + h * 3 / 4);
                g.drawLine(x + w * 3 / 4, y + h / 2, x + w * 4 / 5, y + h * 3 / 4);
                g.drawLine(x + w / 5, y + h * 3 / 4, x + w * 4 / 5, y + h * 3 / 4);
                g.drawArc(x + w * 2 / 5, y + h * 3 / 4, w / 5, h / 7, 180, 180);
            }
            case SEARCH -> {
                g.drawOval(x + w / 6, y + h / 6, w / 2, h / 2);
                g.drawLine(x + w * 3 / 5, y + h * 3 / 5, x + w * 5 / 6, y + h * 5 / 6);
            }
            case LOGOUT -> {
                g.drawLine(x + w / 6, cy, x + w * 3 / 4, cy);
                g.drawLine(x + w * 3 / 4, cy, x + w * 3 / 5, y + h / 3);
                g.drawLine(x + w * 3 / 4, cy, x + w * 3 / 5, y + h * 2 / 3);
                g.drawArc(x + w / 8, y + h / 5, w / 2, h * 3 / 5, 90, 180);
            }
            case TRASH -> {
                g.drawLine(x + w / 4, y + h / 4, x + w * 3 / 4, y + h / 4);
                g.drawLine(x + w / 3, y + h / 4, x + w / 3, y + h * 4 / 5);
                g.drawLine(x + w * 2 / 3, y + h / 4, x + w * 2 / 3, y + h * 4 / 5);
                g.drawLine(x + w / 3, y + h * 4 / 5, x + w * 2 / 3, y + h * 4 / 5);
                g.drawLine(x + w * 2 / 5, y + h / 6, x + w * 3 / 5, y + h / 6);
            }
            case SAVE -> {
                g.drawRoundRect(x + w / 6, y + h / 6, w * 2 / 3, h * 2 / 3, 3, 3);
                g.drawLine(x + w / 3, y + h / 6, x + w / 3, y + h / 3);
                g.drawLine(x + w / 3, y + h / 3, x + w * 2 / 3, y + h / 3);
                g.drawLine(x + w / 3, y + h * 2 / 3, x + w * 2 / 3, y + h * 2 / 3);
            }
            case REFRESH -> {
                g.drawArc(x + w / 5, y + h / 5, w * 3 / 5, h * 3 / 5, 35, 270);
                g.drawLine(x + w * 4 / 5, y + h / 4, x + w * 4 / 5, y + h / 2);
                g.drawLine(x + w * 4 / 5, y + h / 4, x + w * 3 / 5, y + h / 4);
            }
            case EYE -> {
                g.drawArc(x + w / 8, y + h / 4, w * 3 / 4, h / 2, 20, 140);
                g.drawArc(x + w / 8, y + h / 4, w * 3 / 4, h / 2, 200, 140);
                g.drawOval(cx - w / 10, cy - h / 10, w / 5, h / 5);
            }
            case SEND -> {
                Path2D path = new Path2D.Double();
                path.moveTo(x + w / 6.0, y + h / 5.0);
                path.lineTo(x + w * 5 / 6.0, cy);
                path.lineTo(x + w / 6.0, y + h * 4 / 5.0);
                path.lineTo(x + w / 3.0, cy);
                path.closePath();
                g.draw(path);
            }
            case EDIT -> {
                g.drawLine(x + w / 4, y + h * 3 / 4, x + w * 3 / 4, y + h / 4);
                g.drawLine(x + w * 2 / 3, y + h / 5, x + w * 4 / 5, y + h / 3);
                g.drawLine(x + w / 5, y + h * 4 / 5, x + w / 3, y + h * 3 / 4);
            }
            case CHECK -> {
                g.drawLine(x + w / 5, cy, x + w * 2 / 5, y + h * 3 / 4);
                g.drawLine(x + w * 2 / 5, y + h * 3 / 4, x + w * 4 / 5, y + h / 4);
            }
        }
        g.dispose();
    }
}
