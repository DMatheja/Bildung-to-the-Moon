package de.bildung.moon.controller;

import de.bildung.moon.GameCanvas;
import de.bildung.moon.model.ButtonType;
import de.bildung.moon.model.GameModel;
import de.bildung.moon.model.GameState;
import de.bildung.moon.model.PartType;

import static de.bildung.moon.model.GameConstants.*;

import java.awt.*;
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

    // --- UI & Input Status ---



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
                canvas.buildRenderer.updateScrolling(e.getWheelRotation() * 25, canvas.getHeight());
                canvas.repaint();
            }
        }
    }

    private void handleMouseClick(Point clickPos) {
        int gridStartX = SIDE_PANEL_WIDTH;
        int gridEndX = gridStartX + GRID_WIDTH * CELL_SIZE;
        ButtonType bType = canvas.getButton(clickPos);
        if (null != model.currentState) switch (model.currentState) {
            case BUILDING -> {
                if (clickPos.x >= gridStartX && clickPos.x < gridEndX) {
                    int gridX = (clickPos.x - gridStartX) / CELL_SIZE;
                    int gridY = clickPos.y / CELL_SIZE;
                    buildManager.tryPlacePart(gridX, gridY);
                } else if (clickPos.x < gridStartX) {
                    buildManager.selectPart(canvas.buildRenderer.findPartType(clickPos));
                }   if (bType == ButtonType.launchButton && !model.rocket.isEmpty()) {
                    gameManager.prepareForLaunch();
                }   if (bType == ButtonType.autoDetachCheckbox) {
                    model.autoDetachEnabled = !model.autoDetachEnabled;
                }
            }
            case READY_FOR_LAUNCH, COUNTDOWN -> {
                if (bType == ButtonType.startButton && model.currentState == GameState.READY_FOR_LAUNCH) {
                    model.currentState = GameState.COUNTDOWN;
                }   if (bType == ButtonType.backToHangarButton) {
                    gameManager.resetToBuilding();
                }
            }
            case LAUNCHING -> {
                if (bType == ButtonType.detachButton && GameManager.getActiveStageCount(model) > 1) {
                    gameManager.detachStage();
                }   if (bType == ButtonType.selfDestructButton && (model.rocketVelY > 0 || model.isOutOfFuel)) {
                    gameManager.explode();
                }
            }
            case EXPLODED,MOON -> {
                if (bType == ButtonType.backToHangarButton) {
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
