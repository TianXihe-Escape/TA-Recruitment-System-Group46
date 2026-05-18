package ui;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Decorates an existing icon with a small unread indicator dot.
 */
public class UnreadBadgeIcon implements Icon {
    private static final int DOT_SIZE = 8;
    private static final Color DOT_COLOR = new Color(220, 38, 38);

    private final Icon delegate;
    private final boolean unread;

    public UnreadBadgeIcon(Icon delegate, boolean unread) {
        this.delegate = delegate;
        this.unread = unread;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        delegate.paintIcon(component, graphics, x, y);
        if (!unread) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int dotX = x + delegate.getIconWidth() - DOT_SIZE + 1;
        int dotY = y - 1;
        g.setColor(Color.WHITE);
        g.fillOval(dotX - 1, dotY - 1, DOT_SIZE + 2, DOT_SIZE + 2);
        g.setColor(DOT_COLOR);
        g.fillOval(dotX, dotY, DOT_SIZE, DOT_SIZE);
        g.dispose();
    }

    @Override
    public int getIconWidth() {
        return delegate.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return delegate.getIconHeight();
    }
}
