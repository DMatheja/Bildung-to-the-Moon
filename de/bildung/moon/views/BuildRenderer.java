package de.bildung.moon.views;

import de.bildung.moon.controller.BuildManager;
import de.bildung.moon.model.*;

import java.awt.*;

import static de.bildung.moon.model.GameConstants.*;

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
        for (RocketPart part : model.rocket) totalDryMass += part.type.mass;
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g2d.drawString(String.format("Dry Mass: %.0f kg", totalDryMass), shopX + 20, 120);
    }

    private void drawBuildUI(Graphics2D g2d) {
        Rectangle bounds = g2d.getClipBounds();
        int uiX = SIDE_PANEL_WIDTH + GRID_WIDTH * CELL_SIZE;
        g2d.setColor(Color.BLACK);
        g2d.fillRect(uiX, 0, SIDE_PANEL_WIDTH, bounds.height);

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
