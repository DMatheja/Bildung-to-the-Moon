package de.bildung.moon;

import static de.bildung.moon.GameConstants.*;
import java.awt.*;
import javax.swing.*;

/**
 * Der Haupt-JPanel, der als "Controller" fungiert.
 * Er initialisiert alle Subsysteme (Model, Renderer, Physics, Input)
 * und delegiert das Zeichnen an den GameRenderer.
 */
public class GameCanvas extends JPanel {

    private final GameModel model;
    private final GameRenderer renderer;
    // private final PhysicsEngine physicsEngine; // Wird jetzt vom GameManager verwaltet
    private final BuildManager buildManager;
    private final GameManager gameManager;
    private final InputHandler inputHandler;

    public GameCanvas() {
        // 1. Erstelle das Model, das alle Daten enthält
        model = new GameModel();
        
        // 2. Erstelle die Subsysteme und gib ihnen eine Referenz auf das Model
        renderer = new GameRenderer(model);
        buildManager = new BuildManager(model);

        // Der GameManager initialisiert jetzt die PhysicsEngine intern,
        // da sie eng miteinander verbunden sind.
        // Wir übergeben 'this' (den GameCanvas), damit der GameManager
        // Zugriff auf getWidth() und getHeight() hat.
        gameManager = new GameManager(model, this); 

        // 3. Der InputHandler benötigt alle Manager, um Aktionen auszulösen
        inputHandler = new InputHandler(model, buildManager, gameManager, this);

        // 4. Richte die Eingabe-Listener ein
        addMouseListener(inputHandler);
        addMouseMotionListener(inputHandler);
        addMouseWheelListener(inputHandler);

        // 5. Panel-Setup
        int panelWidth = SIDE_PANEL_WIDTH + GRID_WIDTH * CELL_SIZE + SIDE_PANEL_WIDTH;
        int panelHeight = GRID_HEIGHT * CELL_SIZE;
        setPreferredSize(new Dimension(panelWidth, panelHeight));
        setBackground(Color.DARK_GRAY);
        
        // 6. Partikel initialisieren, sobald der Canvas bereit ist
        // (Wir verwenden SwingUtilities, um sicherzustellen, dass dies nach dem Packen passiert)
        SwingUtilities.invokeLater(() -> {
            gameManager.resetBackgroundParticles(getWidth(), getHeight());
        });
    }

    /**
     * Stoppt den Haupt-Game-Loop (delegiert an den GameManager).
     */
    public void stopGameLoop() {
        gameManager.stopGameLoop();
    }

    /**
     * Die paintComponent-Methode wird an den Renderer delegiert.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Stellt sicher, dass die Partikel initialisiert sind, falls invokeLater fehlschlägt
        if (!model.particlesInitialized && getWidth() > 0) {
             gameManager.resetBackgroundParticles(getWidth(), getHeight());
        }
        renderer.paintComponent((Graphics2D) g);
    }
}

