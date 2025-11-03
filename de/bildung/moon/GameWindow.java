package de.bildung.moon;

import javax.swing.*;
import java.awt.*;

/**
 * Das Hauptfenster (JFrame), das den GameCanvas (JPanel) enthält.
 */
public class GameWindow extends JFrame {
    private final JFrame parent;
    private final GameCanvas gameCanvas;

    GameWindow(JFrame parent) {
        super("Bildung to the Moon - Rocket Builder");
        this.parent = parent;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        gameCanvas = new GameCanvas();
        add(gameCanvas, BorderLayout.CENTER);
        pack();
        setMinimumSize(new Dimension(1200, 800));
        setLocationRelativeTo(null);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                onReturn();
            }
        });
        setVisible(true);
    }

    private void onReturn() {
        gameCanvas.stopGameLoop();
        dispose();
        if (parent != null) {
            parent.setVisible(true);
        }
    }
}
