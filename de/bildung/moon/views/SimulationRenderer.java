package de.bildung.moon.views;

import de.bildung.moon.controller.GameManager;
import de.bildung.moon.controller.PhysicsEngine;
import de.bildung.moon.model.*;
import de.bildung.moon.particle.*;

import java.awt.*;
import java.awt.geom.AffineTransform;

import static de.bildung.moon.model.GameConstants.*;

public class SimulationRenderer {
    private final GameModel model;

    public SimulationRenderer(GameModel model) {
        this.model = model;
    }

    public void drawLaunchMode(Graphics2D g2d) {
        double focusY = 0;
        int activePartCount = 0;
        for (RocketPart p : model.placedParts) {
            if (!p.isDetached) {
                focusY += p.worldY;
                activePartCount++;
            }
        }
        if (activePartCount > 0) {
            focusY /= activePartCount;
        } else if (!model.particles.isEmpty()) {
            double particleCenterY = 0;
            int explosionParticles = 0;
            for (Particle p : model.particles) {
                if (p instanceof ExplosionParticle) {
                    particleCenterY += p.y;
                    explosionParticles++;
                }
            }
            if (explosionParticles > 0) {
                focusY = particleCenterY / explosionParticles;
            }
        }

        int h = g2d.getClipBounds().height;
        model.cameraWorldY = focusY - (h / 2.0);

        AffineTransform originalTransform = g2d.getTransform();

        // Layer 1: Sky and Parallax Background (Screen Space)
        drawSky(g2d);
        drawParallaxParticles(g2d);

        // Layer 2: World Objects (World Space)
        g2d.translate(0, -model.cameraWorldY);
        drawGround(g2d);
        drawWorldParticles(g2d);
        drawDetachedStages(g2d);
        if (model.currentState != GameState.EXPLODED) {
            drawPlacedParts(g2d);
        }

        // Layer 3: UI (Screen Space)
        g2d.setTransform(originalTransform);
        drawLaunchUI(g2d);
    }

    private void drawWorldParticles(Graphics2D g2d) {
        for (Particle p : model.particles) {
            if (!(p instanceof Star || p instanceof Cloud || p instanceof Bird || p instanceof Satellite)) {
                p.draw(g2d, model.cameraWorldY, model.altitude);
            }
        }
    }

    private void drawParallaxParticles(Graphics2D g2d) {
        for (Particle p : model.particles) {
            if (p instanceof Star || p instanceof Cloud || p instanceof Bird || p instanceof Satellite) {
                p.draw(g2d, model.cameraWorldY, model.altitude);
            }
        }
    }

    private void drawDetachedStages(Graphics2D g2d) {
        for (DetachedStage ds : model.detachedStages) {
            for (RocketPart p : ds.parts) {
                drawPart(g2d, p.type, (int) p.worldX, (int) p.worldY, 1.0f);
            }
        }
    }

    private void drawSky(Graphics2D g2d) {
        Rectangle bounds = g2d.getClipBounds();
        float skyHue = (float) Math.max(0, 200 - model.altitude / 80000) / 360f;
        float brightness = (float) Math.max(0.05, 0.9 - model.altitude / 120000.0);
        float saturation = (float) Math.max(0.1, 0.7 - model.altitude / 150000.0);
        Color skyTop = Color.getHSBColor(skyHue, saturation, brightness);
        Color skyBottom = Color.getHSBColor(skyHue, saturation * 0.8f, brightness * 0.6f);

        g2d.setPaint(new GradientPaint(0, 0, skyTop, 0, bounds.height, skyBottom));
        g2d.fillRect(0, 0, bounds.width, bounds.height);
    }

    private void drawGround(Graphics2D g2d) {
        Rectangle bounds = g2d.getClipBounds();
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillRect(-bounds.width, GRID_HEIGHT * CELL_SIZE, bounds.width * 3, bounds.height * 10);
    }

    private void drawLaunchUI(Graphics2D g2d) {
        Rectangle bounds = g2d.getClipBounds();
        int uiStartX = SIDE_PANEL_WIDTH + GRID_WIDTH * CELL_SIZE;

        // --- Left Panel ---
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
        g2d.drawString("FLIGHT DATA", 15, 30);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2d.drawString(String.format("Velocity: %.1f m/s", -model.rocketVelY), 15, 60);
        g2d.drawString(String.format("G-Force: %.1f G", model.gForce), 15, 90);
        g2d.drawString(String.format("Mass: %.0f kg", model.currentTotalMass), 15, 120);

        // --- Right Panel ---
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
        g2d.drawString("TELEMETRY", uiStartX + 15, 30);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2d.drawString(String.format("Altitude: %.0f m", model.altitude), uiStartX + 15, 60);
        g2d.drawString(String.format("Max Alt: %.0f m", model.maxAltitude), uiStartX + 15, 90);
        g2d.drawString(String.format("Atm. Press: %.1f %%", (PhysicsEngine.getAirDensityAt(model.altitude) / SEA_LEVEL_AIR_DENSITY) * 100), uiStartX + 15, 120);
        g2d.drawString(String.format("Q: %.1f kPa", model.dynamicPressure / 1000), uiStartX + 15, 150);
        g2d.drawString(String.format("Max Q: %.1f kPa", model.maxDynamicPressure / 1000), uiStartX + 15, 180);

        if (model.timeScale > 1.0) {
            g2d.setColor(Color.CYAN);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
            g2d.drawString(String.format("Time Warp: %.0fx", model.timeScale), bounds.width / 2 - 80, 100);
        }

        String timeString;
        if (model.currentState == GameState.READY_FOR_LAUNCH || (model.currentState == GameState.COUNTDOWN && model.missionTime < 0)) {
            timeString = String.format("T- %.1f", -model.missionTime);
        } else {
            timeString = String.format("T+ %.1f", model.missionTime);
        }
        g2d.setColor(model.missionTime < 0 ? Color.RED : Color.YELLOW);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 48));
        g2d.drawString(timeString, bounds.width / 2 - 100, 60);

        int uiX = uiStartX + 15;
        int uiY = 220;
        int stageNum = 1;
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
        g2d.drawString("STAGES", uiX, uiY - 5);
        uiY += 30;

        for (int i = model.stages.size() - 1; i >= 0; i--) {
            Stage stage = model.stages.get(i);
            if (!stage.hasActiveEngine()) continue;
            double capacity = stage.parts.stream().mapToDouble(p -> p.type.fuelCapacity).sum();
            if (capacity > 0) {
                double fuelRatio = stage.getTotalCurrentFuel() / capacity;
                g2d.setColor(Color.GRAY);
                g2d.drawRect(uiX, uiY, 290, 30);
                g2d.setColor(Color.GREEN);
                g2d.fillRect(uiX + 2, uiY + 2, (int) (286 * fuelRatio), 26);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("SansSerif", Font.BOLD, 22));
                g2d.drawString("Stage " + stageNum + " Fuel", uiX, uiY - 5);
                uiY += 50;
            }
            stageNum++;
        }
        if (model.currentState == GameState.READY_FOR_LAUNCH || model.currentState == GameState.COUNTDOWN) {
            if (model.currentState == GameState.READY_FOR_LAUNCH) {
                g2d.setColor(Color.RED);
                g2d.fill(model.startButton);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("SansSerif", Font.BOLD, 32));
                g2d.drawString("START", model.startButton.x + 90, model.startButton.y + 45);
            }
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fill(model.backToHangarButton);
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
            g2d.drawString("Back to Hangar", model.backToHangarButton.x + 50, model.backToHangarButton.y + 40);
        } else if (model.currentState == GameState.LAUNCHING) {
            if (GameManager.getActiveStageCount(model) > 1) {
                g2d.setColor(Color.ORANGE);
                g2d.fill(model.detachButton);
                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
                g2d.drawString("Detach Stage", model.detachButton.x + 35, model.detachButton.y + 35);
            }
            if (model.rocketVelY > 0 || model.isOutOfFuel) {
                g2d.setColor(Color.RED);
                g2d.fill(model.selfDestructButton);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
                g2d.drawString("SELF-DESTRUCT", model.selfDestructButton.x + 15, model.selfDestructButton.y + 35);
            }
        } else if (model.currentState == GameState.EXPLODED) {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 60));
            g2d.drawString("ROCKET DESTROYED", bounds.width / 2 - 350, bounds.height / 2);
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fill(model.backToHangarButton);
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
            g2d.drawString("Back to Hangar", model.backToHangarButton.x + 50, model.backToHangarButton.y + 40);
        }
    }

    private void drawPart(Graphics2D g2d, PartType type, int x, int y, float scale) {
        int size = (int) (CELL_SIZE * scale);
        int inset = (int) (CELL_SIZE * (1 - scale) / 2);
        x += inset;
        y += inset;
        g2d.setColor(type.color);
        switch (type) {
            case COCKPIT:
                g2d.fillPolygon(new int[]{x, x + size, x + size / 2}, new int[]{y + size, y + size, y}, 3);
                break;
            case FUEL_TANK:
                g2d.fillRect(x, y, size, size);
                break;
            case A4_ENGINE:
            case ENGINE_T1:
                g2d.fillRect(x, y, size, size / 2);
                g2d.fillPolygon(new int[]{x, x + size, x + size - 20, x + 20}, new int[]{y + size / 2, y + size / 2, y + size, y + size}, 4);
                break;
        }
    }

    private void drawPlacedParts(Graphics2D g2d) {
        int gridStartX = (model.currentState == GameState.BUILDING) ? SIDE_PANEL_WIDTH : 0;
        for (RocketPart part : model.placedParts) {
            if ((model.currentState == GameState.LAUNCHING || model.currentState == GameState.EXPLODED) && part.isDetached)
                continue;

            double drawX = (model.currentState == GameState.BUILDING) ? gridStartX + part.gridX * CELL_SIZE : part.worldX;
            double drawY = (model.currentState == GameState.BUILDING) ? part.gridY * CELL_SIZE : part.worldY;

            drawPart(g2d, part.type, (int) drawX, (int) drawY, 1.0f);
        }
    }
}
