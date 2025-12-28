package de.bildung.moon.controller;

import static de.bildung.moon.model.GameConstants.*;

import de.bildung.moon.*;
import de.bildung.moon.model.*;
import de.bildung.moon.particle.*;
import de.bildung.moon.views.SimulationRenderer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

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
        // Time Warp ist nur aktiv, wenn die Rakete fliegt
        if (model.currentState != GameState.LAUNCHING) {
            model.timeScale = 1.0;
            return;
        }

        // Timer für eventuelle Verzögerungen weiterlaufen lassen
        if (model.timeSinceOutOfFuel >= 0 && model.timeSinceOutOfFuel <= 1.0) {
            model.timeSinceOutOfFuel += actualDeltaTime;
        }

        // Sicherheitschecks: Kein TimeWarp unter 10km Höhe oder in Mondnähe (10km Abstand)
        // Mondposition ist fest bei 384.399.000m definiert
        double distanceToMoon = Math.abs(384399000 - model.altitude);

        if (model.altitude < 10000 || distanceToMoon < 10000) {
            model.timeScale = 1.0;
            return; // Sicherheitsabbruch, damit man nicht in den Boden crasht
        }

        // Neue Logik: Time Warp basierend auf G-Kraft
        // Im freien Fall (Orbit) ist die G-Kraft nahe 0 -> schneller Time Warp
        // Beim Start oder Bremsen ist die G-Kraft hoch -> normale Zeit
        double g = Math.abs(model.gForce);

        if (g < 0.1) {
            model.timeScale = 2000.0; // Quasi Schwerelosigkeit -> Maximaler Warp
        } else if (g < 0.5) {
            model.timeScale = 200.0;  // Geringe Kräfte -> Hoher Warp
        } else if (g < 1.0) {
            model.timeScale = 10.0;   // Leichte Kräfte -> Kleiner Warp
        } else {
            model.timeScale = 1.0;    // 1G oder mehr (Schub/Atmosphäre) -> Normalzeit
        }
    }

    private void checkGameStatus() {
        if (model.currentState != GameState.LAUNCHING) return;
        
        // Prüfen, ob alle Teile abgetrennt sind
        if (model.rocket.stream().allMatch(p -> p.isDetached)) {
            stopGameLoop();
        } 
        // Prüfen, ob abgestürzt
        else if (model.altitude < -1.0) {
            explode();
        } else {
            model.maxAltitude = Math.max(model.altitude, model.maxAltitude);
        }
        if(model.altitude > LUNAR_DISTANCE) model.currentState = GameState.MOON;
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
        int maxGridY = model.rocket.stream().mapToInt(p -> p.gridY).max().orElse(GRID_HEIGHT - 1);
        double yOffset = (GRID_HEIGHT - 1 - maxGridY) * CELL_SIZE;
        int gridStartX = SIDE_PANEL_WIDTH;
        for (RocketPart p : model.rocket) {
            p.worldY = p.gridY * CELL_SIZE + yOffset;
            p.worldX = gridStartX + p.gridX * CELL_SIZE;
        }

        // Gesamtmasse berechnen
        model.currentTotalMass = 0;
        for (RocketPart p : model.rocket) {
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
        //Money Logik
        int earnedMoney = (int) (model.maxAltitude / METER_PER_MONEY);
        //Level Logik
        if (model.maxAltitude > model.highScoreAltitude) {
            model.highScoreAltitude = model.maxAltitude;

            // Level 5: Deep Space (Ionen-Antrieb) - 0.1 Gm
            if (model.highScoreAltitude >= 100000000) {
                model.currentLevel = 5;
            }
            // Level 4: Mond-Orbit / Schwerlast (Saturn V / Behemoth) - 1 Mm
            else if (model.highScoreAltitude >= 18000000) { //tipp: T1 Triebwerk 5 mal als Basis mit günstigen Treibstoff
                model.currentLevel = 4;
            }
            // Level 3: Suborbital / Vakuum (Mercury / Voyager) - 1000 km
            else if (model.highScoreAltitude >= 1000000) {
                model.currentLevel = 3;
            }
            // Level 2: Erste Schritte (Apollo / T1 / Proton Tank) - 35 km
            else if (model.highScoreAltitude >= 79000) { //tip: 3 Fuel Tanks
                model.currentLevel = 2;
            }
            // Level 1: Start
            else {
                model.currentLevel = 1;
            }
        }
        stopGameLoop();
        model.currentState = GameState.BUILDING;
        model.detachedStages.clear();
        // model.particlesInitialized = false; // Partikel müssen nicht neu geladen werden
        model.playerMoney += earnedMoney;
        model.currentTotalMass = 0;
        for (RocketPart part : model.rocket) {
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
        for (RocketPart p : model.rocket) {
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

