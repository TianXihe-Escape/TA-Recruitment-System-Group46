package ui;

import javax.swing.JTable;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * JTable variant that paints a small hint when there are no rows to display.
 */
public class PlaceholderTable extends JTable {
    private String emptyMessage;

    public PlaceholderTable(TableModel model, String emptyMessage) {
        super(model);
        this.emptyMessage = emptyMessage;
    }

    public void setEmptyMessage(String emptyMessage) {
        this.emptyMessage = emptyMessage;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (getRowCount() > 0 || emptyMessage == null || emptyMessage.isBlank()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(new Color(120, 134, 155));
        g2.setFont(UiTheme.uiFont(Font.ITALIC, 14));

        int textWidth = g2.getFontMetrics().stringWidth(emptyMessage);
        int x = Math.max(12, (getWidth() - textWidth) / 2);
        int y = Math.max(24, getHeight() / 2);
        g2.drawString(emptyMessage, x, y);
        g2.dispose();
    }
}
