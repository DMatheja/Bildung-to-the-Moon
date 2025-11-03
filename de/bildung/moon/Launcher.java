package de.bildung.moon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Simple launcher UI for the "Bildung to the Moon" game.
 * Dies ist der Haupteinstiegspunkt der Anwendung.
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
        SwingUtilities.invokeLater(() -> new GameWindow(frame));
        frame.setVisible(false);
    }

    private void onSettings(ActionEvent e) {
        JOptionPane.showMessageDialog(frame, "Settings not yet implemented.", "Settings", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onQuit() {
        frame.dispose();
        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Launcher::new);
    }
}
