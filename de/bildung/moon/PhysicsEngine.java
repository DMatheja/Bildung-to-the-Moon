package de.bildung.moon;

import static de.bildung.moon.GameConstants.*;
import de.bildung.moon.particle.DebrisParticle;
import de.bildung.moon.particle.ExhaustParticle;

/**
 * Enthält die gesamte Physik-Logik für den Raketenflug.
 * Wird vom GameManager im Game-Loop aufgerufen.
 */
public class PhysicsEngine {

    private final GameModel model;
    private final GameManager gameManager; // Benötigt für Aktionen wie detachStage

    public PhysicsEngine(GameModel model, GameManager gameManager) {
        this.model = model;
        this.gameManager = gameManager;
    }

    /**
     * Haupt-Update-Schleife für die Physik.
     * @param panelWidth Die aktuelle Breite des Canvas-Panels.
     * @param panelHeight Die aktuelle Höhe des Canvas-Panels.
     * @param actualDeltaTime Die reale vergangene Zeit (für Timewarp-Timer).
     * @param deltaTime Die skalierte vergangene Zeit (für Physik).
     */
    public void updatePhysics(int panelWidth, int panelHeight, double actualDeltaTime, double deltaTime) {
        
        // --- 1. Partikel aktualisieren ---
        model.particles.forEach(p -> {
            p.update(deltaTime);
            // Cloud-Wrapping-Logik
            if (p instanceof de.bildung.moon.particle.Cloud) {
                if (panelWidth > 0) {
                    if (p.x > panelWidth + 200) p.x = -200;
                    if (p.x < -200) p.x = panelWidth + 200;
                }
            }
        });
        model.particles.removeIf(p -> p.isDead());
        
        // Debris-Partikel
        if (model.currentState == GameState.LAUNCHING && model.rocketVelY < 0 && model.altitude < 2000) {
            if (Math.random() > 0.7) {
                double rocketX = 0;
                int activeParts = 0;
                for (RocketPart p : model.placedParts) {
                    if (!p.isDetached) {
                        rocketX += p.worldX;
                        activeParts++;
                    }
                }
                if (activeParts > 0) {
                    rocketX /= activeParts;
                    // Höhe ist jetzt verfügbar
                    model.particles.add(new DebrisParticle(rocketX, model.cameraWorldY, panelHeight));
                }
            }
        }

        // --- 2. Abgetrennte Stufen aktualisieren ---
        for (DetachedStage ds : model.detachedStages) {
            double stageMass = ds.parts.stream().mapToDouble(p -> p.type.mass).sum();
            if (stageMass <= 0) continue;
            double stageGravity = stageMass * getGravityAt(ds.altitude);
            double stageDrag = -DRAG_CONSTANT * getAirDensityAt(ds.altitude) * ds.velY * Math.abs(ds.velY);
            double netForce = stageGravity + stageDrag;
            double acceleration = netForce / stageMass;
            ds.velY += acceleration * deltaTime;
            ds.altitude += -ds.velY * deltaTime;
            for (RocketPart p : ds.parts) {
                p.worldY += ds.velY * deltaTime * PIXELS_PER_METER;
            }
        }

        // Physik-Update nur im LAUNCHING-Status
        if (model.currentState != GameState.LAUNCHING) return;
        
        model.missionTime += deltaTime;
        
        // --- 3. Masse aktualisieren ---
        model.currentTotalMass = 0;
        for (RocketPart p : model.placedParts) {
            if (!p.isDetached) model.currentTotalMass += p.type.mass + p.currentFuel;
        }
        if (model.currentTotalMass <= 0) {
            model.isOutOfFuel = true;
            return;
        }

        // --- 4. Schub und Treibstoffverbrauch ---
        double totalThrust = 0;
        Stage activeStage = null;
        for (int i = model.stages.size() - 1; i >= 0; i--) {
            Stage s = model.stages.get(i);
            if (s.hasActiveEngine()) {
                if (s.getTotalCurrentFuel() > 0) {
                    activeStage = s;
                    break;
                } else if (model.autoDetachEnabled && GameManager.getActiveStageCount(model) > 1) {
                    // Benachrichtigt den GameManager, diese Stufe abzutrennen
                    gameManager.detachStage(s); 
                }
            }
        }

        double currentGravity = getGravityAt(model.altitude);
        if (activeStage != null) {
            model.isOutOfFuel = false;
            model.timeSinceOutOfFuel = -1.0;
            // model.timeScale = 1.0; // Dies wird jetzt vom GameManager gesteuert
            
            totalThrust = activeStage.stageEngines.stream().mapToDouble(e -> e.type.thrust).sum();
            int isp = activeStage.stageEngines.get(0).type.specificImpulse;
            double fuelConsumption = (totalThrust / (isp * SEA_LEVEL_GRAVITY)) * deltaTime;
            activeStage.consumeFuel(fuelConsumption);
            
            for (RocketPart engine : activeStage.stageEngines) {
                for (int i = 0; i < 10; i++) {
                    model.particles.add(new ExhaustParticle(engine.worldX + CELL_SIZE / 2.0, engine.worldY + CELL_SIZE, model.rocketVelY));
                }
            }
        } else {
            // Kein Treibstoff mehr
            if (!model.isOutOfFuel) {
                model.isOutOfFuel = true;
                model.timeSinceOutOfFuel = 0.0;
            }
            if (model.timeSinceOutOfFuel >= 0) {
                model.timeSinceOutOfFuel += actualDeltaTime; // Echte Zeit für den Timer
            }
            // Die Time-Warp-Logik selbst befindet sich jetzt im GameManager
        }
        
        // --- 5. Kräfteberechnung ---
        double forceThrust = -totalThrust;
        double forceGravity = model.currentTotalMass * currentGravity;
        double airDensity = getAirDensityAt(model.altitude);
        double forceDrag = -DRAG_CONSTANT * airDensity * model.rocketVelY * Math.abs(model.rocketVelY);
        
        double netForce = forceThrust + forceGravity + forceDrag;
        double acceleration = netForce / model.currentTotalMass;
        model.rocketVelY += acceleration * deltaTime;
        model.gForce = Math.abs(acceleration / SEA_LEVEL_GRAVITY);
        model.altitude += -model.rocketVelY * deltaTime;
        
        model.dynamicPressure = 0.5 * airDensity * model.rocketVelY * model.rocketVelY;
        if (model.dynamicPressure > model.maxDynamicPressure) {
            model.maxDynamicPressure = model.dynamicPressure;
        }
        
        // --- 6. Teile bewegen ---
        for (RocketPart part : model.placedParts) {
            if (!part.isDetached) {
                part.worldY += model.rocketVelY * deltaTime * PIXELS_PER_METER;
            }
        }
    }
    
    /**
     * Berechnet die Luftdichte in einer bestimmten Höhe.
     */
    public static double getAirDensityAt(double alt) {
        if (alt < 0) return SEA_LEVEL_AIR_DENSITY;
        return SEA_LEVEL_AIR_DENSITY * Math.exp(-alt / SCALE_HEIGHT);
    }

    /**
     * Berechnet die Schwerkraft in einer bestimmten Höhe.
     */
    public static double getGravityAt(double alt) {
        return SEA_LEVEL_GRAVITY * Math.pow(EARTH_RADIUS / (EARTH_RADIUS + alt), 2);
    }
}

