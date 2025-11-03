package de.bildung.moon;

import de.bildung.moon.particle.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import static de.bildung.moon.GameConstants.*;

/**
 * Der "View" der Anwendung.
 * Diese Klasse ist AUSSCHLIESSLICH für das Zeichnen zuständig.
 * Sie liest Daten aus dem GameModel, ändert sie aber nicht.
 */
public class GameRenderer {

    private final GameModel model;

    public GameRenderer(GameModel model) {
        this.model = model;
    }

    /**
     * Haupt-Zeichenmethode, wird vom GameCanvas aufgerufen.
     */
    public void paintComponent(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (!model.particlesInitialized && g2d.getClipBounds() != null) {
            // Greife auf Breite und Höhe über die Clip-Grenzen zurück (oder übergebe sie vom Canvas)
            int w = g2d.getClipBounds().width;
            int h = g2d.getClipBounds().height;
            if (w > 0 && h > 0) {
                // Hässlicher Workaround: Da der Renderer die Canvas-Größe nicht kennt,
                // rufen wir die Partikel-Initialisierung (eine Zustandsänderung)
                // vom GameManager aus auf, wenn der Canvas erstellt wird.
                // Für dieses Refactoring lassen wir es vorerst hier.
                // Besser: GameManager.resetBackgroundParticles(w, h) im Canvas-Konstruktor aufrufen.
                // ... (Logik zum Zurücksetzen der Partikel wurde in GameManager verschoben)
                // model.particlesInitialized = true; // Dies sollte der GameManager tun.
            }
        }

        updateUIRectangles(g2d.getClipBounds());
        if (model.currentState == GameState.BUILDING) {
            drawBuildMode(g2d);
        } else {
            drawLaunchMode(g2d);
        }
    }

    /**
     * Aktualisiert die Positionen der UI-Elemente basierend auf der Fenstergröße.
     */
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

    private void drawBuildMode(Graphics2D g2d) {
        drawGrid(g2d);
        drawPlacedParts(g2d);
        drawShop(g2d);
        drawGhostPart(g2d);
        drawBuildUI(g2d);
    }

    private void drawLaunchMode(Graphics2D g2d) {
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

    private void drawGrid(Graphics2D g2d) {
        int gridStartX = SIDE_PANEL_WIDTH;
        g2d.setColor(Color.GRAY);
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                g2d.drawRect(gridStartX + x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
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

    private void drawShop(Graphics2D g2d) {
        int shopX = 0;
        Rectangle bounds = g2d.getClipBounds();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(shopX, 0, SIDE_PANEL_WIDTH, bounds.height);
        
        Shape oldClip = g2d.getClip();
        g2d.clipRect(shopX, 150, SIDE_PANEL_WIDTH, bounds.height - 150);
        
        int yOffset = 150;
        int itemHeight = 160;

        for (PartType type : PartType.values()) {
            int itemY = (int) (yOffset - model.shopScrollY);

            if (type == model.selectedPartType) {
                g2d.setColor(Color.YELLOW);
                g2d.drawRect(shopX + 10, itemY, SIDE_PANEL_WIDTH - 20, itemHeight + 20);
            }
            
            drawPart(g2d, type, shopX + 15, itemY + 45, 0.7f);

            g2d.setColor(Color.WHITE);
            int textX = shopX + 120;
            int textY = itemY + 35;
            
            g2d.setFont(new Font("SansSerif", Font.BOLD, 22));
            g2d.drawString(type.name + " ($" + type.cost + ")", textX, textY);
            
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 18));
            textY += 30;
            g2d.drawString(String.format("Mass: %.0f kg", type.mass), textX, textY);
            
            if (type.fuelCapacity > 0) {
                textY += 25;
                g2d.drawString(String.format("Fuel: %.0f kg", type.fuelCapacity), textX, textY);
            }
            
            if (type.thrust > 0) {
                textY += 25;
                g2d.drawString(String.format("Thrust: %.1f kN", type.thrust / 1000), textX, textY);
                textY += 25;
                g2d.drawString(String.format("Isp: %d s", type.specificImpulse), textX, textY);
            }
            
            yOffset += itemHeight + 40;
        }
        g2d.setClip(oldClip);

        g2d.setColor(Color.BLACK);
        g2d.fillRect(shopX, 0, SIDE_PANEL_WIDTH, 150);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 30));
        g2d.drawString("Shop", shopX + 120, 50);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 24));
        g2d.drawString("Money: $" + model.playerMoney, shopX + 20, 90);
        
        double totalDryMass = 0;
        for (RocketPart part : model.placedParts) totalDryMass += part.type.mass;
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g2d.drawString(String.format("Dry Mass: %.0f kg", totalDryMass), shopX + 20, 120);
    }

    private void drawBuildUI(Graphics2D g2d) {
        Rectangle bounds = g2d.getClipBounds();
        int uiX = SIDE_PANEL_WIDTH + GRID_WIDTH * CELL_SIZE;
        g2d.setColor(Color.BLACK);
        g2d.fillRect(uiX, 0, SIDE_PANEL_WIDTH, bounds.height);

        g2d.setColor(model.placedParts.isEmpty() ? Color.GRAY : Color.GREEN);
        g2d.fill(model.launchButton);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 30));
        g2d.drawString("To Launchpad", model.launchButton.x + 35, model.launchButton.y + 45);
        
        g2d.setColor(Color.WHITE);
        g2d.draw(model.autoDetachCheckbox);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g2d.drawString("Auto-Detach", model.autoDetachCheckbox.x + 50, model.autoDetachCheckbox.y + 26);
        if (model.autoDetachEnabled) {
            g2d.setColor(Color.GREEN);
            g2d.setStroke(new BasicStroke(4));
            int checkX = model.autoDetachCheckbox.x + 15;
            int checkY = model.autoDetachCheckbox.y + 18;
            g2d.drawLine(checkX, checkY, checkX + 8, checkY + 8);
            g2d.drawLine(checkX + 8, checkY + 8, checkX + 20, checkY - 8);
            g2d.setStroke(new BasicStroke(1));
        }
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

    private void drawGhostPart(Graphics2D g2d) {
        int gridStartX = SIDE_PANEL_WIDTH;
        if (model.selectedPartType != null && model.mousePos.x >= gridStartX && model.mousePos.x < gridStartX + GRID_WIDTH * CELL_SIZE) {
            int gridX = (model.mousePos.x - gridStartX) / CELL_SIZE;
            int gridY = model.mousePos.y / CELL_SIZE;
            if (gridX >= GRID_WIDTH || gridY >= GRID_HEIGHT) return;
            
            int mirroredX = (GRID_WIDTH - 1) - gridX;
            boolean isCenter = (gridX == mirroredX);

            // Draw original ghost part
            drawSingleGhost(g2d, gridX, gridY);

            // Draw mirrored ghost part if not in center
            if (!isCenter) {
                drawSingleGhost(g2d, mirroredX, gridY);
            }
        }
    }

    private void drawSingleGhost(Graphics2D g2d, int gridX, int gridY) {
        int gridStartX = SIDE_PANEL_WIDTH;
        int x = gridStartX + gridX * CELL_SIZE;
        int y = gridY * CELL_SIZE;
        Composite old = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        drawPart(g2d, model.selectedPartType, x, y, 1.0f);
        g2d.setComposite(old);
        if (!BuildManager.isPlacementLegal(model, model.selectedPartType, gridX, gridY) || model.grid[gridX][gridY] != null) {
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(4));
            g2d.drawLine(x, y, x + CELL_SIZE, y + CELL_SIZE);
            g2d.drawLine(x + CELL_SIZE, y, x, y + CELL_SIZE);
            g2d.setStroke(new BasicStroke(1));
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
}
