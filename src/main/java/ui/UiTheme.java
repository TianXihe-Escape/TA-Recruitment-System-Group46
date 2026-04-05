package ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * Shared visual system for the Swing UI.
 */
public final class UiTheme {
    public static final Color BACKGROUND = new Color(243, 246, 251);
    public static final Color SURFACE = Color.WHITE;
    public static final Color SURFACE_ALT = new Color(248, 250, 253);
    public static final Color BORDER = new Color(217, 224, 234);
    public static final Color TEXT = new Color(28, 39, 54);
    public static final Color MUTED_TEXT = new Color(97, 109, 126);
    public static final Color PRIMARY = new Color(36, 99, 235);
    public static final Color PRIMARY_SOFT = new Color(225, 236, 255);
    public static final Color SUCCESS = new Color(22, 163, 74);
    public static final Color WARNING = new Color(245, 158, 11);
    public static final Color DANGER = new Color(220, 38, 38);

    private static final String FONT_FAMILY = resolveFontFamily();
    private static final Font TITLE_FONT = uiFont(Font.BOLD, 28);
    private static final Font SECTION_FONT = uiFont(Font.BOLD, 18);
    private static final Font BODY_FONT = uiFont(Font.PLAIN, 14);
    private static final Font SMALL_FONT = uiFont(Font.PLAIN, 12);
    private static final Font BUTTON_FONT = uiFont(Font.BOLD, 13);

    private UiTheme() {
    }

    public static void install() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        UIManager.put("control", BACKGROUND);
        UIManager.put("info", SURFACE);
        UIManager.put("nimbusBase", PRIMARY);
        UIManager.put("nimbusBlueGrey", new Color(236, 241, 248));
        UIManager.put("nimbusFocus", new Color(120, 164, 255));
        UIManager.put("text", TEXT);
        UIManager.put("defaultFont", BODY_FONT);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("ScrollPane.background", SURFACE);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.selectionBackground", PRIMARY_SOFT);
        UIManager.put("Table.selectionForeground", TEXT);
    }

    public static void styleFrame(JFrame frame) {
        frame.getContentPane().setBackground(BACKGROUND);
    }

    public static Font uiFont(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    public static JPanel createPagePanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));
        return panel;
    }

    public static JPanel createHeader(String title, String subtitle) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(TEXT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(BODY_FONT);
        subtitleLabel.setForeground(MUTED_TEXT);
        subtitleLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        header.add(titleLabel);
        header.add(subtitleLabel);
        return header;
    }

    public static JPanel createCard(String title, String subtitle) {
        JPanel card = new JPanel(new BorderLayout(16, 16));
        card.setBackground(SURFACE);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(18, 18, 18, 18)
        ));

        if (title != null && !title.isBlank()) {
            JPanel header = new JPanel();
            header.setOpaque(false);
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(SECTION_FONT);
            titleLabel.setForeground(TEXT);
            header.add(titleLabel);

            if (subtitle != null && !subtitle.isBlank()) {
                JLabel subtitleLabel = new JLabel(subtitle);
                subtitleLabel.setFont(BODY_FONT);
                subtitleLabel.setForeground(MUTED_TEXT);
                subtitleLabel.setBorder(new EmptyBorder(4, 0, 0, 0));
                header.add(subtitleLabel);
            }

            card.add(header, BorderLayout.NORTH);
        }

        return card;
    }

    public static JPanel createFormGrid() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        return form;
    }

    public static void addFormRow(JPanel form, int row, String label, JComponent component) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.weightx = 0;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.insets = new Insets(row == 0 ? 0 : 10, 0, 6, 0);

        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(SMALL_FONT);
        labelComponent.setForeground(MUTED_TEXT);
        form.add(labelComponent, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 0;
        fieldConstraints.gridy = row + 1;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(0, 0, 0, 0);
        form.add(component, fieldConstraints);
    }

    public static void styleTextField(JTextComponent component) {
        component.setFont(BODY_FONT);
        component.setForeground(TEXT);
        component.setBackground(SURFACE_ALT);
        component.setCaretColor(TEXT);
        component.setBorder(inputBorder());
        component.setSelectionColor(PRIMARY_SOFT);
        component.setMargin(new Insets(8, 10, 8, 10));
    }

    public static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(BODY_FONT);
        comboBox.setForeground(TEXT);
        comboBox.setBackground(SURFACE_ALT);
        comboBox.setBorder(inputBorder());
        comboBox.setFocusable(true);
    }

    public static void styleTextArea(JTextArea area, int rows) {
        area.setFont(BODY_FONT);
        area.setForeground(TEXT);
        area.setBackground(SURFACE_ALT);
        area.setCaretColor(TEXT);
        area.setBorder(inputBorder());
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setRows(rows);
        area.setMargin(new Insets(10, 10, 10, 10));
    }

    public static JButton createPrimaryButton(String text) {
        return createActionButton(text);
    }

    public static JButton createSecondaryButton(String text) {
        return createActionButton(text);
    }

    public static JButton createDangerButton(String text) {
        return createActionButton(text);
    }

    public static JPanel createButtonRow(int align, JButton... buttons) {
        JPanel panel = new JPanel(new FlowLayout(align, 10, 10));
        panel.setOpaque(false);
        for (JButton button : buttons) {
            panel.add(button);
        }
        return panel;
    }

    public static void styleTable(JTable table) {
        table.setFont(BODY_FONT);
        table.setForeground(TEXT);
        table.setBackground(SURFACE);
        table.setRowHeight(32);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(PRIMARY_SOFT);
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setDefaultEditor(Object.class, null);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setCellSelectionEnabled(false);

        JTableHeader header = table.getTableHeader();
        header.setFont(uiFont(Font.BOLD, 12));
        header.setBackground(SURFACE_ALT);
        header.setForeground(MUTED_TEXT);
        header.setBorder(new LineBorder(BORDER, 1, true));
        header.setReorderingAllowed(false);
    }

    public static JScrollPane wrapTable(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        styleScrollPane(scrollPane);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(new LineBorder(BORDER, 1, true));
        scrollPane.getViewport().setBackground(SURFACE);
        scrollPane.setBackground(SURFACE);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollBar(scrollPane.getHorizontalScrollBar());
        styleScrollBar(scrollPane.getVerticalScrollBar());
    }

    public static void styleDialogScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        styleDialogScrollBar(scrollPane.getVerticalScrollBar());
        styleDialogScrollBar(scrollPane.getHorizontalScrollBar());
    }

    public static void styleSplitPane(JSplitPane splitPane) {
        splitPane.setOpaque(false);
        splitPane.setBorder(null);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerSize(10);
    }

    public static void styleTabs(JTabbedPane tabs) {
        tabs.setFont(BODY_FONT);
        tabs.setBackground(SURFACE);
        tabs.setForeground(TEXT);
    }

    public static JLabel createBadge(String text, Color background, Color foreground) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setBackground(background);
        label.setForeground(foreground);
        label.setFont(uiFont(Font.BOLD, 12));
        label.setBorder(new EmptyBorder(6, 10, 6, 10));
        return label;
    }

    public static JScrollPane wrapPage(JComponent component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BACKGROUND);
        scrollPane.setBackground(BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(18);
        styleScrollBar(scrollPane.getHorizontalScrollBar());
        styleScrollBar(scrollPane.getVerticalScrollBar());
        return scrollPane;
    }

    public static void setColumnWidths(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnModel().getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setPreferredWidth(widths[i]);
            column.setMinWidth(Math.min(widths[i], 80));
        }
    }

    private static JButton baseButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        return button;
    }

    private static JButton createActionButton(String text) {
        JButton button = baseButton(text);
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
        return button;
    }

    private static String resolveFontFamily() {
        String[] preferredFonts = {
                "Microsoft YaHei UI",
                "Microsoft YaHei",
                "PingFang SC",
                "Hiragino Sans GB",
                "Noto Sans CJK SC",
                "WenQuanYi Micro Hei",
                "SimHei",
                "Arial Unicode MS",
                "Segoe UI",
                Font.DIALOG
        };
        String[] availableFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String preferredFont : preferredFonts) {
            for (String availableFont : availableFonts) {
                if (availableFont.equalsIgnoreCase(preferredFont)) {
                    return availableFont;
                }
            }
        }
        return Font.DIALOG;
    }

    private static Border inputBorder() {
        return new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(2, 2, 2, 2)
        );
    }

    private static void styleScrollBar(JScrollBar scrollBar) {
        scrollBar.setOpaque(false);
        scrollBar.setPreferredSize(scrollBar.getOrientation() == Adjustable.VERTICAL
                ? new Dimension(14, 0)
                : new Dimension(0, 14));
        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(156, 177, 208);
                thumbDarkShadowColor = thumbColor;
                thumbHighlightColor = thumbColor;
                thumbLightShadowColor = thumbColor;
                trackColor = new Color(232, 238, 247);
                trackHighlightColor = trackColor;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int x = trackBounds.x + 2;
                int y = trackBounds.y + 2;
                int width = Math.max(0, trackBounds.width - 4);
                int height = Math.max(0, trackBounds.height - 4);

                GradientPaint trackPaint = scrollbar.getOrientation() == Adjustable.VERTICAL
                        ? new GradientPaint(x, y, new Color(244, 247, 252), x + width, y, new Color(226, 233, 244))
                        : new GradientPaint(x, y, new Color(244, 247, 252), x, y + height, new Color(226, 233, 244));
                g2.setPaint(trackPaint);
                g2.fillRoundRect(x, y, width, height, 12, 12);
                g2.setColor(new Color(208, 217, 231));
                g2.drawRoundRect(x, y, Math.max(0, width - 1), Math.max(0, height - 1), 12, 12);
                g2.setColor(new Color(255, 255, 255, 170));
                g2.drawRoundRect(x + 1, y + 1, Math.max(0, width - 3), Math.max(0, height - 3), 11, 11);
                g2.dispose();
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                    return;
                }
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int x = thumbBounds.x + 2;
                int y = thumbBounds.y + 2;
                int width = Math.max(0, thumbBounds.width - 4);
                int height = Math.max(0, thumbBounds.height - 4);

                g2.setColor(new Color(123, 149, 188, 70));
                g2.fillRoundRect(x + 1, y + 2, width, height, 12, 12);

                GradientPaint thumbPaint = scrollbar.getOrientation() == Adjustable.VERTICAL
                        ? new GradientPaint(x, y, new Color(189, 206, 231), x + width, y, new Color(140, 165, 201))
                        : new GradientPaint(x, y, new Color(189, 206, 231), x, y + height, new Color(140, 165, 201));
                g2.setPaint(thumbPaint);
                g2.fillRoundRect(x, y, width, height, 12, 12);

                g2.setColor(new Color(255, 255, 255, 175));
                g2.drawRoundRect(x, y, Math.max(0, width - 1), Math.max(0, height - 1), 12, 12);
                g2.setColor(new Color(117, 142, 177));
                g2.drawRoundRect(x, y, Math.max(0, width - 1), Math.max(0, height - 1), 12, 12);
                g2.dispose();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }
        });
    }

    private static void styleDialogScrollBar(JScrollBar scrollBar) {
        scrollBar.setOpaque(false);
        scrollBar.setPreferredSize(scrollBar.getOrientation() == Adjustable.VERTICAL
                ? new Dimension(10, 0)
                : new Dimension(0, 10));
        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(108, 108, 108);
                thumbDarkShadowColor = thumbColor;
                thumbHighlightColor = thumbColor;
                thumbLightShadowColor = thumbColor;
                trackColor = new Color(243, 246, 251);
                trackHighlightColor = trackColor;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(243, 246, 251));
                g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
                g2.dispose();
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                    return;
                }
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int x = thumbBounds.x + 1;
                int y = thumbBounds.y + 2;
                int width = Math.max(0, thumbBounds.width - 2);
                int height = Math.max(22, thumbBounds.height - 4);

                g2.setColor(new Color(96, 96, 96));
                g2.fillRoundRect(x, y, width, height, 8, 8);
                g2.dispose();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }
        });
    }
}
