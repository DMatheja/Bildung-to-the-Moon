package de.bildung.moon.controller;

import de.bildung.moon.GameCanvas;
import de.bildung.moon.model.GameModel;
import de.bildung.moon.model.GameState;
import de.bildung.moon.model.PartType;

import static de.bildung.moon.model.GameConstants.*;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.SwingUtilities;

/**
 * Verarbeitet alle Mauseingaben.
 * Erhält Referenzen auf die Manager-Klassen, um Aktionen auszulösen,
 * und auf das GameModel, um den Zustand zu ändern (z.B. Mausposition).
 */
public class InputHandler extends MouseAdapter {

    private final GameModel model;
    private final BuildManager buildManager;
    private final GameManager gameManager;
    private final GameCanvas canvas; // Wird für repaint() benötigt

    public InputHandler(GameModel model, BuildManager buildManager, GameManager gameManager, GameCanvas canvas) {
        this.model = model;
        this.buildManager = buildManager;
        this.gameManager = gameManager;
        this.canvas = canvas;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        model.mousePos = e.getPoint();
        canvas.repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            handleMouseClick(e.getPoint());
        } else if (SwingUtilities.isRightMouseButton(e)) {
            handleRightClick(e.getPoint());
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (model.currentState == GameState.BUILDING && canvas.getWidth() > 0) {
            if (model.mousePos.x < SIDE_PANEL_WIDTH) {
                model.shopScrollY += e.getWheelRotation() * 25;
                // TODO: Diese Logik sollte in eine UI-Management-Klasse
                int totalItemHeight = PartType.values().length * 180;
                int visibleHeight = canvas.getHeight() - 140 - 150;
                int maxScroll = Math.max(0, totalItemHeight - visibleHeight);
                if (model.shopScrollY < 0) model.shopScrollY = 0;
                if (model.shopScrollY > maxScroll) model.shopScrollY = maxScroll;
                canvas.repaint();
            }
        }
    }

    private void handleMouseClick(Point clickPos) {
        int gridStartX = SIDE_PANEL_WIDTH;
        int gridEndX = gridStartX + GRID_WIDTH * CELL_SIZE;

        if (null != model.currentState) switch (model.currentState) {
            case BUILDING -> {
                if (clickPos.x >= gridStartX && clickPos.x < gridEndX) {
                    int gridX = (clickPos.x - gridStartX) / CELL_SIZE;
                    int gridY = clickPos.y / CELL_SIZE;
                    buildManager.tryPlacePart(gridX, gridY);
                } else if (clickPos.x < gridStartX) {
                    buildManager.selectPartFromShop(clickPos);
                }   if (model.launchButton.contains(clickPos) && !model.placedParts.isEmpty()) {
                    gameManager.prepareForLaunch();
                }   if (model.autoDetachCheckbox.contains(clickPos)) {
                    model.autoDetachEnabled = !model.autoDetachEnabled;
                }
            }
            case READY_FOR_LAUNCH, COUNTDOWN -> {
                if (model.startButton.contains(clickPos) && model.currentState == GameState.READY_FOR_LAUNCH) {
                    model.currentState = GameState.COUNTDOWN;
                }   if (model.backToHangarButton.contains(clickPos)) {
                    gameManager.resetToBuilding();
                }
            }
            case LAUNCHING -> {
                if (model.detachButton.contains(clickPos) && GameManager.getActiveStageCount(model) > 1) {
                    gameManager.detachStage();
                }   if (model.selfDestructButton.contains(clickPos) && (model.rocketVelY > 0 || model.isOutOfFuel)) {
                    gameManager.explode();
                }
            }
            case EXPLODED -> {
                if (model.backToHangarButton.contains(clickPos)) {
                    gameManager.resetToBuilding();
                }
            }
            default -> {
            }
        }
        canvas.repaint();
    }

    private void handleRightClick(Point clickPos) {
        if (model.currentState != GameState.BUILDING) return;
        
        int gridStartX = SIDE_PANEL_WIDTH;
        if (clickPos.x >= gridStartX && clickPos.x < gridStartX + GRID_WIDTH * CELL_SIZE) {
            int gridX = (clickPos.x - gridStartX) / CELL_SIZE;
            int gridY = clickPos.y / CELL_SIZE;
            if (gridX < GRID_WIDTH && gridY < GRID_HEIGHT) {
                buildManager.removePart(gridX, gridY);
                canvas.repaint();
            }
        }
    }
}
