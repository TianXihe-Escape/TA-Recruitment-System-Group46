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
import java.awt.event.MouseWheelEvent;

/**
 * Centralized theme and styling system for the TA Recruitment System's Swing user interface.
 * This class provides a comprehensive visual design system that ensures consistency across
 * all UI components, implementing a modern, professional appearance suitable for an
 * educational institution application.
 *
 * Key design principles:
 * - Consistent color palette with semantic color meanings
 * - Typography hierarchy using carefully selected fonts
 * - Unified component styling for buttons, forms, tables, and dialogs
 * - Responsive and accessible design considerations
 * - Cross-platform compatibility with Nimbus Look and Feel as base
 *
 * The theme system includes:
 * - Color constants for backgrounds, surfaces, text, and accents
 * - Font management with fallback support for different platforms
 * - Component styling methods for all major Swing components
 * - Custom scrollbar implementations with smooth gradients
 * - Mouse wheel forwarding for improved user experience
 * - Internationalization support with English dialog labels
 *
 * Usage:
 * Call UiTheme.install() at application startup to apply the theme globally.
 * Use the various styling methods (styleTextField, styleTable, etc.) to apply
 * consistent styling to individual components.
 *
 * @author TA Recruitment System Development Team
 * @version 1.0.0
 * @since 2026-04-09
 */
public final class UiTheme {

    /**
     * Primary background color for the application.
     * A light blue-gray tone that provides a clean, professional backdrop
     * for all major UI elements and page content.
     */
    public static final Color BACKGROUND = new Color(243, 246, 251);

    /**
     * Surface color for cards, panels, and elevated content.
     * Pure white provides maximum contrast and readability for content areas.
     */
    public static final Color SURFACE = Color.WHITE;

    /**
     * Alternative surface color for secondary content areas.
     * A slightly tinted white that provides subtle visual hierarchy
     * without being as prominent as the primary surface color.
     */
    public static final Color SURFACE_ALT = new Color(248, 250, 253);

    /**
     * Border color for separating UI elements.
     * A muted blue-gray that provides subtle definition without being harsh.
     */
    public static final Color BORDER = new Color(217, 224, 234);

    /**
     * Primary text color for headings and important content.
     * A dark blue-gray that ensures excellent readability and accessibility.
     */
    public static final Color TEXT = new Color(28, 39, 54);

    /**
     * Secondary text color for labels, captions, and less important text.
     * A medium gray that provides sufficient contrast while indicating lower importance.
     */
    public static final Color MUTED_TEXT = new Color(97, 109, 126);

    /**
     * Primary brand color for buttons, links, and interactive elements.
     * A professional blue that conveys trust and reliability.
     */
    public static final Color PRIMARY = new Color(36, 99, 235);

    /**
     * Light variant of the primary color for backgrounds and highlights.
     * Used for selection states, hover effects, and subtle accents.
     */
    public static final Color PRIMARY_SOFT = new Color(225, 236, 255);

    /**
     * Success color for positive actions and confirmations.
     * A green tone that clearly indicates successful operations.
     */
    public static final Color SUCCESS = new Color(22, 163, 74);

    /**
     * Warning color for cautionary messages and non-critical alerts.
     * An amber tone that draws attention without causing alarm.
     */
    public static final Color WARNING = new Color(245, 158, 11);

    /**
     * Danger/error color for critical alerts and destructive actions.
     * A red tone that clearly indicates problems or dangerous operations.
     */
    public static final Color DANGER = new Color(220, 38, 38);

    /**
     * Unit increment for smooth scrolling (pixels per wheel click).
     * Controls how much content moves with each mouse wheel increment.
     */
    private static final int SCROLL_UNIT_INCREMENT = 32;

    /**
     * Block increment for page-based scrolling (pixels per page).
     * Controls how much content moves when clicking in the scrollbar track.
     */
    private static final int SCROLL_BLOCK_INCREMENT = 96;

    /**
     * Resolved font family name based on system availability.
     * Uses a prioritized list of CJK and Western fonts for optimal display
     * across different platforms and languages.
     */
    private static final String FONT_FAMILY = resolveFontFamily();

    /**
     * Font for main titles and headings.
     * Large, bold font used for page titles and major section headers.
     */
    private static final Font TITLE_FONT = uiFont(Font.BOLD, 28);

    /**
     * Font for section headers and card titles.
     * Medium-sized bold font for subsection headings and important labels.
     */
    private static final Font SECTION_FONT = uiFont(Font.BOLD, 18);

    /**
     * Font for body text and general content.
     * Standard-sized regular font for readable body text and descriptions.
     */
    private static final Font BODY_FONT = uiFont(Font.PLAIN, 14);

    /**
     * Font for small text and captions.
     * Smaller font for labels, footnotes, and secondary information.
     */
    private static final Font SMALL_FONT = uiFont(Font.PLAIN, 12);

    /**
     * Font for button text and call-to-action elements.
     * Bold font that ensures button text is prominent and readable.
     */
    private static final Font BUTTON_FONT = uiFont(Font.BOLD, 13);

    /**
     * Private constructor to prevent instantiation.
     * This utility class contains only static methods and constants,
     * so instantiation is unnecessary and should be prevented.
     */
    private UiTheme() {
        // Utility class - no instantiation allowed
    }

    /**
     * Installs the custom UI theme globally across the application.
     * This method should be called once at application startup, before creating any UI components.
     * It configures the Nimbus Look and Feel as the base, then overrides specific UI properties
     * to achieve the desired visual appearance.
     *
     * The installation process includes:
     * 1. Setting Nimbus as the system Look and Feel
     * 2. Configuring color properties for various UI elements
     * 3. Setting the default font for consistent typography
     * 4. Installing English labels for standard dialogs
     *
     * If Nimbus is not available, the method gracefully continues with the default Look and Feel,
     * though some styling may not be applied perfectly.
     */
    public static void install() {
        // Attempt to set Nimbus Look and Feel as the base theme
        // Nimbus provides a modern, cross-platform appearance that we can customize
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // If Nimbus installation fails, continue with default Look and Feel
            // The application will still function, though styling may be less polished
        }

        // Configure Nimbus color properties to match our theme
        // These override the default Nimbus colors with our custom palette
        UIManager.put("control", BACKGROUND);  // Background for controls like buttons
        UIManager.put("info", SURFACE);  // Background for tooltips and info panels
        UIManager.put("nimbusBase", PRIMARY);  // Base color for various Nimbus elements
        UIManager.put("nimbusBlueGrey", new Color(236, 241, 248));  // Blue-gray accent color
        UIManager.put("nimbusFocus", new Color(120, 164, 255));  // Focus ring color
        UIManager.put("text", TEXT);  // Default text color
        UIManager.put("defaultFont", BODY_FONT);  // Default font for all components

        // Override specific component background colors
        UIManager.put("OptionPane.background", SURFACE);  // Dialog background
        UIManager.put("Panel.background", BACKGROUND);  // Panel background
        UIManager.put("ScrollPane.background", SURFACE);  // Scroll pane background
        UIManager.put("Table.background", SURFACE);  // Table background
        UIManager.put("Table.selectionBackground", PRIMARY_SOFT);  // Selected table row background
        UIManager.put("Table.selectionForeground", TEXT);  // Selected table row text color

        // Install English labels for standard Swing dialogs
        // This ensures consistent language regardless of system locale
        installEnglishDialogLabels();
    }

    public static void styleFrame(JFrame frame) {
        frame.getContentPane().setBackground(BACKGROUND);
    }

    public static Font uiFont(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    /**
     * Creates a standard page panel with consistent padding and layout.
     * This method provides a uniform container for page-level content,
     * ensuring consistent margins and background across different screens.
     *
     * @return A JPanel configured as a page container with proper spacing and background.
     */
    public static JPanel createPagePanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));
        return panel;
    }

    /**
     * Creates a header panel with title and subtitle for page sections.
     * This provides a consistent way to display page titles and descriptions,
     * with proper typography and spacing.
     *
     * @param title    The main title text to display.
     * @param subtitle The subtitle or description text (can be null).
     * @return A JPanel containing the formatted header with title and subtitle.
     */
    public static JPanel createHeader(String title, String subtitle) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        // Create and style the main title label
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(TEXT);

        // Create and style the subtitle label if provided
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(BODY_FONT);
        subtitleLabel.setForeground(MUTED_TEXT);
        subtitleLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        // Add components to the header panel
        header.add(titleLabel);
        header.add(subtitleLabel);
        return header;
    }

    /**
     * Creates a card panel with optional title and subtitle.
     * Cards provide visual grouping and elevation for content sections,
     * with consistent styling and spacing.
     *
     * @param title    The card title (can be null for title-less cards).
     * @param subtitle The card subtitle or description (can be null).
     * @return A JPanel styled as a card with the specified title and subtitle.
     */
    public static JPanel createCard(String title, String subtitle) {
        JPanel card = new JPanel(new BorderLayout(16, 16));
        card.setBackground(SURFACE);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(18, 18, 18, 18)
        ));

        // Add header section if title is provided
        if (title != null && !title.isBlank()) {
            JPanel header = new JPanel();
            header.setOpaque(false);
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

            // Title label with section font
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(SECTION_FONT);
            titleLabel.setForeground(TEXT);
            header.add(titleLabel);

            // Subtitle label if provided
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

    /**
     * Creates a form grid panel for organizing form elements.
     * This provides a GridBagLayout container specifically designed for form layouts,
     * with consistent spacing and alignment.
     *
     * @return A JPanel configured with GridBagLayout for form content.
     */
    public static JPanel createFormGrid() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        return form;
    }

    /**
     * Adds a labeled form row to a form grid.
     * This method handles the complex GridBagLayout constraints to create
     * properly aligned label-field pairs in forms.
     *
     * @param form  The form panel to add the row to.
     * @param row   The row index for positioning (0-based).
     * @param label The text for the field label.
     * @param component The form component (text field, combo box, etc.).
     */
    public static void addFormRow(JPanel form, int row, String label, JComponent component) {
        // Configure constraints for the label
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;  // Left column
        labelConstraints.gridy = row;  // Specified row
        labelConstraints.weightx = 0;  // Don't expand horizontally
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;  // Top-left alignment
        labelConstraints.insets = new Insets(row == 0 ? 0 : 10, 0, 6, 0);  // Spacing between rows

        // Create and add the label
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(SMALL_FONT);
        labelComponent.setForeground(MUTED_TEXT);
        form.add(labelComponent, labelConstraints);

        // Configure constraints for the input component
        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 0;  // Same column (right below label)
        fieldConstraints.gridy = row + 1;  // Next row
        fieldConstraints.weightx = 1;  // Expand to fill available width
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;  // Fill horizontally
        fieldConstraints.insets = new Insets(0, 0, 0, 0);  // No additional insets
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

    public static void styleCheckBox(JCheckBox checkBox) {
        checkBox.setFont(BODY_FONT);
        checkBox.setForeground(MUTED_TEXT);
        checkBox.setOpaque(false);
        checkBox.setFocusPainted(false);
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
        JButton button = baseButton(text);
        button.setBackground(DANGER);
        button.setForeground(Color.WHITE);
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
        return button;
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
        return scrollPane;
    }

    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(new LineBorder(BORDER, 1, true));
        scrollPane.getViewport().setBackground(SURFACE);
        scrollPane.setBackground(SURFACE);
        applyScrollSpeed(scrollPane);
        styleScrollBar(scrollPane.getHorizontalScrollBar());
        styleScrollBar(scrollPane.getVerticalScrollBar());
    }

    public static void styleDialogScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        applyScrollSpeed(scrollPane);
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
        applyScrollSpeed(scrollPane);
        styleScrollBar(scrollPane.getHorizontalScrollBar());
        styleScrollBar(scrollPane.getVerticalScrollBar());
        installWheelForwarding(component);
        return scrollPane;
    }

    public static void styleFileChooser(JFileChooser chooser) {
        SwingUtilities.updateComponentTreeUI(chooser);
        styleComponentTree(chooser);
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

    private static void installEnglishDialogLabels() {
        UIManager.put("OptionPane.okButtonText", "OK");
        UIManager.put("OptionPane.cancelButtonText", "Cancel");
        UIManager.put("OptionPane.yesButtonText", "Yes");
        UIManager.put("OptionPane.noButtonText", "No");

        UIManager.put("FileChooser.openButtonText", "Open");
        UIManager.put("FileChooser.saveButtonText", "Save");
        UIManager.put("FileChooser.cancelButtonText", "Cancel");
        UIManager.put("FileChooser.updateButtonText", "Update");
        UIManager.put("FileChooser.helpButtonText", "Help");
        UIManager.put("FileChooser.directoryOpenButtonText", "Open");

        UIManager.put("FileChooser.openDialogTitleText", "Open");
        UIManager.put("FileChooser.saveDialogTitleText", "Save");
        UIManager.put("FileChooser.lookInLabelText", "Look in:");
        UIManager.put("FileChooser.saveInLabelText", "Save in:");
        UIManager.put("FileChooser.fileNameLabelText", "File name:");
        UIManager.put("FileChooser.filesOfTypeLabelText", "Files of type:");
        UIManager.put("FileChooser.folderNameLabelText", "Folder name:");
        UIManager.put("FileChooser.pathLabelText", "Path:");

        UIManager.put("FileChooser.upFolderToolTipText", "Up One Level");
        UIManager.put("FileChooser.homeFolderToolTipText", "Home");
        UIManager.put("FileChooser.newFolderToolTipText", "Create New Folder");
        UIManager.put("FileChooser.listViewButtonToolTipText", "List");
        UIManager.put("FileChooser.detailsViewButtonToolTipText", "Details");
        UIManager.put("FileChooser.fileNameHeaderText", "Name");
        UIManager.put("FileChooser.fileSizeHeaderText", "Size");
        UIManager.put("FileChooser.fileTypeHeaderText", "Type");
        UIManager.put("FileChooser.fileDateHeaderText", "Modified");
        UIManager.put("FileChooser.fileAttrHeaderText", "Attributes");
    }

    private static void applyScrollSpeed(JScrollPane scrollPane) {
        scrollPane.getHorizontalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
        scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
        scrollPane.getHorizontalScrollBar().setBlockIncrement(SCROLL_BLOCK_INCREMENT);
        scrollPane.getVerticalScrollBar().setBlockIncrement(SCROLL_BLOCK_INCREMENT);
        scrollPane.getHorizontalScrollBar().setFocusable(false);
        scrollPane.getVerticalScrollBar().setFocusable(false);
    }

    private static void installWheelForwarding(Component component) {
        if (shouldForwardWheel(component)) {
            component.addMouseWheelListener(UiTheme::forwardWheelToNearestScrollPane);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                installWheelForwarding(child);
            }
        }
    }

    private static boolean shouldForwardWheel(Component component) {
        return !(component instanceof JScrollPane
                || component instanceof JScrollBar
                || component instanceof JTable
                || component instanceof JTextArea
                || component instanceof JList<?>
                || component instanceof JTree);
    }

    private static void forwardWheelToNearestScrollPane(MouseWheelEvent event) {
        if (event.isConsumed() || event.getWheelRotation() == 0) {
            return;
        }
        Component source = event.getComponent();
        JScrollPane scrollPane = findScrollableAncestor(source);
        if (scrollPane == null) {
            return;
        }

        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        if (verticalBar == null || !verticalBar.isVisible() || !verticalBar.isEnabled()) {
            return;
        }

        int direction = event.getWheelRotation();
        int delta = event.getUnitsToScroll() * Math.max(1, verticalBar.getUnitIncrement(direction));
        int maxValue = verticalBar.getMaximum() - verticalBar.getVisibleAmount();
        int nextValue = Math.max(verticalBar.getMinimum(), Math.min(maxValue, verticalBar.getValue() + delta));
        verticalBar.setValue(nextValue);
        event.consume();
    }

    private static JScrollPane findScrollableAncestor(Component source) {
        Component current = source;
        while (current != null) {
            if (current instanceof JViewport viewport) {
                Container parent = viewport.getParent();
                if (parent instanceof JScrollPane scrollPane) {
                    JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
                    if (verticalBar != null && verticalBar.isVisible() && verticalBar.isEnabled()
                            && verticalBar.getMaximum() > verticalBar.getVisibleAmount()) {
                        return scrollPane;
                    }
                }
            }
            current = current.getParent();
        }
        return null;
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

    private static void styleComponentTree(Component component) {
        if (component instanceof JScrollPane scrollPane) {
            styleScrollPane(scrollPane);
        } else if (component instanceof JPanel panel) {
            panel.setBackground(BACKGROUND);
        } else if (component instanceof JTable table) {
            styleTable(table);
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                styleComponentTree(child);
            }
        }
    }
}
