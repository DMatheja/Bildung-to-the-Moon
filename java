import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Simple launcher UI for the "Bildung to the Moon" game.
 * - Start Game: opens a placeholder game window (replace with your game later).
 * - Settings: simple dialog for future settings.
 * - Quit: exits the app.
 *
 * Save as Launcher.java and run with: javac Launcher.java && java Launcher
 */
public class Launcher {
    private final JFrame frame;

    public Launcher() {
        frame = new JFrame("Bildung to the Moon - Launcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 220);
        frame.setLocationRelativeTo(null);

        frame.setContentPane(createMainPanel());
        frame.setVisible(true);
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("<html><div style='text-align:center'>Bildung to the Moon</div></html>", JLabel.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        panel.add(title, BorderLayout.NORTH);

        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(3, 1, 8, 8));

        JButton startBtn = new JButton("Start Game");
        startBtn.addActionListener(this::onStart);
        buttons.add(startBtn);

        JButton settingsBtn = new JButton("Settings");
        settingsBtn.addActionListener(this::onSettings);
        buttons.add(settingsBtn);

        JButton quitBtn = new JButton("Quit");
        quitBtn.addActionListener(e -> onQuit());
        buttons.add(quitBtn);

        panel.add(buttons, BorderLayout.CENTER);

        JLabel hint = new JLabel("Tip: implement your game and replace the placeholder.", JLabel.CENTER);
        hint.setFont(hint.getFont().deriveFont(11f));
        panel.add(hint, BorderLayout.SOUTH);

        return panel;
    }

    private void onStart(ActionEvent e) {
        // Open the game window. Replace GameWindow contents with the real game later.
        SwingUtilities.invokeLater(() -> new GameWindow(frame));
        frame.setVisible(false);
    }

    private void onSettings(ActionEvent e) {
        // Minimal settings dialog for future expansion
        String current = "Window size: 800x600 (placeholder)";
        JOptionPane.showMessageDialog(frame, current, "Settings", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onQuit() {
        frame.dispose();
        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Launcher::new);
    }

    // Simple placeholder game window. Replace GameCanvas with your actual game canvas/class.
    private static class GameWindow extends JFrame {
        private final JFrame parent;

        GameWindow(JFrame parent) {
            super("Bildung to the Moon - Game (Placeholder)");
            this.parent = parent;
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(800, 600);
            setLocationRelativeTo(null);

            setLayout(new BorderLayout());
            add(new GameCanvas(), BorderLayout.CENTER);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton back = new JButton("Return to Menu");
            back.addActionListener(e -> onReturn());
            bottom.add(back);
            add(bottom, BorderLayout.SOUTH);

            addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    onReturn(); // ensure menu returns if user closes window
                }
            });

            setVisible(true);
        }

        private void onReturn() {
            dispose();
            if (parent != null) {
                parent.setVisible(true);
            }
        }
    }

    // Very small drawing panel as a placeholder for the future game view.
    private static class GameCanvas extends JPanel {
        GameCanvas() {
            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(800, 560));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // Placeholder visuals
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            String msg = "Game will start here";
            Font f = g2.getFont().deriveFont(Font.BOLD, 28f);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(msg)) / 2;
            int y = (getHeight() / 2) - (fm.getHeight() / 2) + fm.getAscent();
            g2.drawString(msg, x, y);

            g2.setFont(g2.getFont().deriveFont(12f));
            String hint = "Replace GameCanvas with your game rendering / loop.";
            int hx = (getWidth() - g2.getFontMetrics().stringWidth(hint)) / 2;
            g2.drawString(hint, hx, y + 30);

            g2.dispose();
        }
    }
}