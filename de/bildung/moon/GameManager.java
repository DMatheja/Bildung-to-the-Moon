package de.bildung.moon;

import static de.bildung.moon.GameConstants.*;
import de.bildung.moon.particle.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;

/**
 * Verwaltet den Spielzustand, den Game-Loop und wichtige Zustandsübergänge
 * (z.B. vom Hangar zum Launchpad).
 */
public class GameManager implements ActionListener { // Implementiert ActionListener für den Timer

    private final GameModel model;
    private final PhysicsEngine physicsEngine;
    private final GameCanvas canvas;
    private Timer gameLoop;

    public GameManager(GameModel model, GameCanvas canvas) {
        this.model = model;
        this.canvas = canvas;
        // Die PhysicsEngine benötigt jetzt eine Referenz auf den GameManager (this) für Callbacks
        this.physicsEngine = new PhysicsEngine(model, this); 
    }
    
    // Die Game-Loop-Logik wird in eine eigene Methode ausgelagert
    @Override
    public void actionPerformed(ActionEvent e) {
        // 1. Zeitberechnung
        long now = System.nanoTime();
        double actualDeltaTime = (now - model.lastUpdateTime) / 1_000_000_000.0;
        model.lastUpdateTime = now;

        // 2. Timewarp-Logik (Game-Logic)
        handleTimeWarp(actualDeltaTime);
        double deltaTime = actualDeltaTime * model.timeScale;

        // 3. Countdown-Logik (Game-Logic)
        handleCountdown(deltaTime);

        // 4. Physik-Update (Physics-Logic)
        // Ruft die korrigierte Methode mit allen Parametern auf
        physicsEngine.updatePhysics(canvas.getWidth(), canvas.getHeight(), actualDeltaTime, deltaTime);

        // 5. Statusprüfungen nach der Physik (Game-Logic)
        checkGameStatus();

        // 6. Neuzeichnen
        canvas.repaint();
    }

    private void handleCountdown(double deltaTime) {
        if (model.currentState == GameState.COUNTDOWN) {
            model.missionTime += deltaTime;
            if (model.missionTime >= 0) {
                startLaunch(); // Interne Methode, um den Status zu ändern
            }
        }
    }

    private void handleTimeWarp(double actualDeltaTime) {
        if (model.currentState != GameState.LAUNCHING || !model.isOutOfFuel) {
            model.timeScale = 1.0;
            return;
        }
        
        // Timer für Time-Warp-Verzögerung
        if (model.timeSinceOutOfFuel >= 0 && model.timeSinceOutOfFuel <= 1.0) {
             model.timeSinceOutOfFuel += actualDeltaTime;
        }
        
        if (model.timeSinceOutOfFuel > 1.0) {
            if (model.altitude < 1000) model.timeScale = 1.0;
            else if (Math.abs(model.rocketVelY) < 100 && model.altitude > 80000) model.timeScale = 100.0;
            else if (model.altitude > 100000) model.timeScale = 50.0;
            else if (model.altitude > 50000) model.timeScale = 25.0;
            else if (model.altitude > 20000) model.timeScale = 10.0;
            else if (model.altitude > 5000) model.timeScale = 5.0;
            else model.timeScale = 2.0;
        } else {
            model.timeScale = 1.0;
        }
    }

    private void checkGameStatus() {
        if (model.currentState != GameState.LAUNCHING) return;
        
        // Prüfen, ob alle Teile abgetrennt sind
        if (model.placedParts.stream().allMatch(p -> p.isDetached)) {
            stopGameLoop();
        } 
        // Prüfen, ob abgestürzt
        else if (model.altitude < -1.0) {
            explode();
        } else {
            model.maxAltitude = Math.max(model.altitude, model.maxAltitude);
        }
    }

    /**
     * Bereitet das Spiel für den Start vor.
     */
    public void prepareForLaunch() {
        model.currentState = GameState.READY_FOR_LAUNCH;
        model.rocketVelY = 0;
        model.altitude = 0;
        model.maxAltitude = 0;
        model.gForce = 0;
        model.dynamicPressure = 0;
        model.maxDynamicPressure = 0;
        model.missionTime = -3.0;
        model.isOutOfFuel = false;
        model.timeScale = 1.0;
        model.timeSinceOutOfFuel = -1.0;
        model.detachedStages.clear();
        model.particles.removeIf(p -> p instanceof ExplosionParticle || p instanceof ExhaustParticle || p instanceof DebrisParticle);
        
        // Rakete auf dem Boden positionieren
        int maxGridY = model.placedParts.stream().mapToInt(p -> p.gridY).max().orElse(GRID_HEIGHT - 1);
        double yOffset = (GRID_HEIGHT - 1 - maxGridY) * CELL_SIZE;
        int gridStartX = SIDE_PANEL_WIDTH;
        for (RocketPart p : model.placedParts) {
            p.worldY = p.gridY * CELL_SIZE + yOffset;
            p.worldX = gridStartX + p.gridX * CELL_SIZE;
        }

        // Gesamtmasse berechnen
        model.currentTotalMass = 0;
        for (RocketPart p : model.placedParts) {
            model.currentTotalMass += p.type.mass + p.currentFuel;
        }

        // Game Loop starten
        model.lastUpdateTime = System.nanoTime();
        // Der GameManager hört jetzt selbst auf den Timer
        gameLoop = new Timer(16, this); 
        gameLoop.start();
    }

    /**
     * Setzt das Spiel in den Bau-Modus zurück.
     */
    public void resetToBuilding() {
        stopGameLoop();
        model.currentState = GameState.BUILDING;
        model.detachedStages.clear();
        // model.particlesInitialized = false; // Partikel müssen nicht neu geladen werden

        model.currentTotalMass = 0;
        for (RocketPart part : model.placedParts) {
            part.currentFuel = part.type.fuelCapacity;
            part.worldX = SIDE_PANEL_WIDTH + part.gridX * CELL_SIZE;
            part.worldY = part.gridY * CELL_SIZE;
            part.isDetached = false;
        }
        BuildManager.recalculateStages(model);
    }

    /**
     * Startet die Countdown-Logik (die in updateGame behandelt wird).
     */
    public void startLaunch() {
        model.currentState = GameState.LAUNCHING;
        model.rocketVelY = 0;
        model.altitude = 0;
        model.maxAltitude = 0;
        model.gForce = 0;
    }

    /**
     * Löst die Explosion der Rakete aus.
     */
    public void explode() {
        model.currentState = GameState.EXPLODED;
        for (RocketPart p : model.placedParts) {
            if (!p.isDetached) {
                double partCenterX = p.worldX + (CELL_SIZE / 2.0);
                double partCenterY = p.worldY + (CELL_SIZE / 2.0);
                for (int i = 0; i < 25; i++) {
                    model.particles.add(new ExplosionParticle(partCenterX, partCenterY));
                }
            }
        }
    }

    /**
     * Löst die unterste aktive Stufe ab.
     */
    public void detachStage() {
        for (int i = model.stages.size() - 1; i >= 0; i--) {
            Stage stage = model.stages.get(i);
            if (stage.hasActiveEngine()) {
                detachStage(stage);
                break;
            }
        }
    }

    /**
     * Löst eine bestimmte Stufe ab. (Wird von PhysicsEngine aufgerufen)
     */
    public void detachStage(Stage stageToDetach) {
        if (stageToDetach.parts.stream().anyMatch(p -> !p.isDetached)) {
            model.detachedStages.add(new DetachedStage(stageToDetach.parts, model.rocketVelY, model.altitude));
            for (RocketPart part : stageToDetach.parts) {
                part.isDetached = true;
            }
        }
    }

    /**
     * Zählt die Anzahl der Stufen, die noch Triebwerke haben.
     */
    public static long getActiveStageCount(GameModel model) {
        return model.stages.stream().filter(Stage::hasActiveEngine).count();
    }

    /**
     * Stoppt den Game-Loop.
     */
    public void stopGameLoop() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
    }
    
    /**
     * Initialisiert die Hintergrundpartikel (Sterne, Wolken etc.).
     * Sollte vom Canvas aufgerufen werden, wenn seine Größe bekannt ist.
     */
    public void resetBackgroundParticles(int w, int h) {
        model.particles.clear();
        for (int i = 0; i < 50; i++) { model.particles.add(new Cloud(w, h)); }
        for (int i = 0; i < 20; i++) { model.particles.add(new Bird(w, h)); }
        for (int i = 0; i < 10; i++) { model.particles.add(new Satellite(w, h)); }
        for (int i = 0; i < 400; i++) { model.particles.add(new Star(w, h)); }
        model.particlesInitialized = true;
    }
}

