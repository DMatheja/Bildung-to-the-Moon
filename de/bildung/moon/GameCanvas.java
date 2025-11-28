package de.bildung.moon;

import de.bildung.moon.controller.BuildManager;
import de.bildung.moon.controller.GameManager;
import de.bildung.moon.controller.InputHandler;
import de.bildung.moon.model.GameModel;
import de.bildung.moon.model.GameState;
import de.bildung.moon.views.BuildRenderer;
import de.bildung.moon.views.SimulationRenderer;

import static de.bildung.moon.model.GameConstants.*;
import java.awt.*;
import javax.swing.*;

/**
 * Der Haupt-JPanel, der als "Controller" fungiert.
 * Er initialisiert alle Subsysteme (Model, Renderer, Physics, Input)
 * und delegiert das Zeichnen an den GameRenderer.
 */
public class GameCanvas extends JPanel {

    private final GameModel model;
    private final BuildRenderer renderer;
    private final SimulationRenderer simRenderer;
    // private final PhysicsEngine physicsEngine; // Wird jetzt vom GameManager verwaltet
    private final BuildManager buildManager;
    private final GameManager gameManager;
    private final InputHandler inputHandler;

    public GameCanvas() {
        // 1. Erstelle das Model, das alle Daten enthält
        model = new GameModel();
        
        // 2. Erstelle die Subsysteme und gib ihnen eine Referenz auf das Model
        renderer = new BuildRenderer(model);
        simRenderer = new SimulationRenderer(model);
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
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        updateUIRectangles(g2d.getClipBounds());
        if (model.currentState == GameState.BUILDING) {
            renderer.drawBuildMode(g2d);
        } else {
            simRenderer.drawLaunchMode(g2d);
        }
    }

    private void updateUIRectangles(Rectangle bounds) {
        if (bounds == null) return;
        int h = bounds.height;
        int uiStartX = SIDE_PANEL_WIDTH + GRID_WIDTH * CELL_SIZE;

        model.launchButton.setBounds(uiStartX + 30, h - 80, 290, 60);
        model.autoDetachCheckbox.setBounds(uiStartX + 30, h - 125, 290, 35);

        model.startButton.setBounds(uiStartX + 30, h - 80, 290, 60);
        model.backToHangarButton.setBounds(uiStartX + 30, h - 150, 290, 60);
        model.detachButton.setBounds(15, h - 80, 220, 50);
        model.selfDestructButton.setBounds(15, h - 140, 220, 50);
    }
}

