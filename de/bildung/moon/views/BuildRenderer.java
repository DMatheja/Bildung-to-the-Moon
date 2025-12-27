package de.bildung.moon.views;

import de.bildung.moon.controller.BuildManager;
import de.bildung.moon.model.*;
import static de.bildung.moon.model.GameConstants.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * Der "View" der Anwendung.
 * Diese Klasse ist AUSSCHLIESSLICH für das Zeichnen zuständig.
 * Sie liest Daten aus dem GameModel, ändert sie aber nicht.
 */
public class BuildRenderer {

    private final GameModel model;

    public BuildRenderer(GameModel model) {
        this.model = model;
    }

    /**
     * Aktualisiert die Positionen der UI-Elemente basierend auf der Fenstergröße.
     */

    public void drawBuildMode(Graphics2D g2d) {
        drawGrid(g2d);
        drawPlacedParts(g2d);
        drawShop(g2d);
        drawGhostPart(g2d);
        drawBuildUI(g2d);
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
        for (RocketPart part : model.rocket) {
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

            // --- ÄNDERUNG: Nutze getDisplayColor für ausgegraute Teile ---
            // drawPart ruft nun implizit die Farbe ab, wir müssen sie aber vorher setzen,
            // falls wir drawPart manuell aufrufen würden, aber hier passiert das in drawPartHelper oder inline.
            // Moment, drawPart setzt die Farbe selbst. Aber um sicherzugehen, setzen wir sie hier für Text etc.

            // Da drawPart unten die Farbe setzt, übergeben wir hier einfach die Logik für den Rahmen/Hintergrund des Items falls gewünscht
            // Hier zeichnen wir das Teil im Shop:
            drawPartInShop(g2d, type, shopX + 15, itemY + 45, 0.7f);

            g2d.setColor(Color.WHITE);
            // Wenn gesperrt, Text etwas dunkler
            if (model.currentLevel < type.requiredLevel) {
                g2d.setColor(Color.GRAY);
            }

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

            // Optional: Schloss-Symbol oder Text "Locked"
            if (model.currentLevel < type.requiredLevel) {
                g2d.setColor(Color.RED);
                g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
                g2d.drawString("LOCKED (Lvl " + type.requiredLevel + ")", textX, textY + 30);
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
        for (RocketPart part : model.rocket) totalDryMass += part.type.mass;
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g2d.drawString(String.format("Dry Mass: %.0f kg", totalDryMass), shopX + 20, 120);
    }

    private void drawBuildUI(Graphics2D g2d) {
        Rectangle bounds = g2d.getClipBounds();
        int uiX = SIDE_PANEL_WIDTH + GRID_WIDTH * CELL_SIZE;
        g2d.setColor(Color.BLACK);
        g2d.fillRect(uiX, 0, SIDE_PANEL_WIDTH, bounds.height);

        // --- PROGRESSION INFO (OBEN RECHTS) ---
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 24));

        String levelStr = "Level: " + model.currentLevel;
        String scoreStr = String.format("Highscore: %.0f m", model.highScoreAltitude);

        // Etwas Abstand vom rechten Rand
        int marginX = 20;
        int currentY = 50;

        // Level zeichnen
        g2d.drawString(levelStr, uiX + marginX, currentY);

        // Highscore darunter
        currentY += 35;
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g2d.drawString(scoreStr, uiX + marginX, currentY);

        // --- Buttons ---
        g2d.setColor(model.rocket.isEmpty() ? Color.GRAY : Color.GREEN);
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
        if (!BuildManager.isPlacementLegal(model, model.selectedPartType, gridX, gridY)
                || (model.grid[gridX][gridY] != null && model.grid[gridX][gridY].type.superType != model.selectedPartType.superType)) {
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

        int drawX = x + inset;
        int drawY = y + inset;

        // Normale Farbe
        g2d.setColor(type.color);
        drawShapes(g2d,type.Shapes, drawX, drawY, size);
    }

    // Eigene Methode für den Shop, die die Progression berücksichtigt
    private void drawPartInShop(Graphics2D g2d, PartType type, int x, int y, float scale) {
        int size = (int) (CELL_SIZE * scale);
        int inset = (int) (CELL_SIZE * (1 - scale) / 2);

        int drawX = x + inset;
        int drawY = y + inset;

        // HIER IST DIE ÄNDERUNG: Nutze getDisplayColor statt color
        g2d.setColor(type.getDisplayColor(model.currentLevel));

        drawShapes(g2d,type.Shapes, drawX, drawY, size);
    }

    public void drawShapes(Graphics2D g2d, java.util.List<Shape> shapes, double x, double y, double scale) {
        // 1. Save the current state of the graphics context
        AffineTransform oldTransform = g2d.getTransform();

        // 2. Move the "pen" to the desired location
        g2d.translate(x, y);

        // 3. Scale the coordinate system
        // (e.g., 2.0 makes everything twice as big)
        g2d.scale(scale/10, scale/10);

        // 4. Draw all shapes in the new coordinate space
        for (Shape shape : shapes) {
            g2d.fill(shape); // Use g2d.draw(shape) for outlines
        }

        // 5. IMPORTANT: Restore the transform so other things draw correctly
        g2d.setTransform(oldTransform);
    }
}