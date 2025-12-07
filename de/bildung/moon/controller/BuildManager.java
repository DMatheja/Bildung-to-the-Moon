package de.bildung.moon.controller;

import de.bildung.moon.model.GameModel;
import de.bildung.moon.model.PartType;
import de.bildung.moon.model.RocketPart;
import de.bildung.moon.model.Stage;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import static de.bildung.moon.model.GameConstants.*;

/**
 * Verwaltet die gesamte Logik, die im Hangar (BUILDING State) stattfindet.
 */
public class BuildManager {

    private final GameModel model;

    public BuildManager(GameModel model) {
        this.model = model;
    }

    /**
     * Versucht, ein Teil auf dem Gitter zu platzieren.
     */
    public void tryPlacePart(int gridX, int gridY) {
        if (model.selectedPartType == null || model.grid[gridX][gridY] != null) return;

        int mirroredX = (GRID_WIDTH - 1) - gridX;
        boolean isCenter = (gridX == mirroredX);

        if (isCenter) {
            if (model.playerMoney >= model.selectedPartType.cost && isPlacementLegal(model, model.selectedPartType, gridX, gridY)) {
                model.playerMoney -= model.selectedPartType.cost;
                RocketPart newPart = new RocketPart(model.selectedPartType, gridX, gridY);
                model.grid[gridX][gridY] = newPart;
                model.rocket.add(newPart);
                recalculateStages(model);
            }
        } else {
            if (model.playerMoney < model.selectedPartType.cost * 2 || model.grid[mirroredX][gridY] != null) return;

            if (isPlacementLegal(model, model.selectedPartType, gridX, gridY) && isPlacementLegal(model, model.selectedPartType, mirroredX, gridY)) {
                model.playerMoney -= model.selectedPartType.cost * 2;

                RocketPart part1 = new RocketPart(model.selectedPartType, gridX, gridY);
                model.grid[gridX][gridY] = part1;
                model.rocket.add(part1);

                RocketPart part2 = new RocketPart(model.selectedPartType, mirroredX, gridY);
                model.grid[mirroredX][gridY] = part2;
                model.rocket.add(part2);

                recalculateStages(model);
            }
        }
    }

    /**
     * Entfernt ein Teil (und sein Spiegelbild) vom Gitter.
     */
    public void removePart(int gridX, int gridY) {
        RocketPart partToRemove = model.grid[gridX][gridY];
        if (partToRemove != null) {
            int mirroredX = (GRID_WIDTH - 1) - gridX;
            boolean isCenter = (gridX == mirroredX);

            if (isCenter) {
                model.playerMoney += partToRemove.type.cost;
                model.grid[gridX][gridY] = null;
                model.rocket.remove(partToRemove);
            } else {
                RocketPart mirroredPart = model.grid[mirroredX][gridY];

                model.playerMoney += partToRemove.type.cost;
                model.grid[gridX][gridY] = null;
                model.rocket.remove(partToRemove);

                if (mirroredPart != null) {
                    model.playerMoney += mirroredPart.type.cost;
                    model.grid[mirroredX][gridY] = null;
                    model.rocket.remove(mirroredPart);
                }
            }
            recalculateStages(model);
        }
    }

    /**
     * Wählt ein Teil aus dem Shop basierend auf der Klickposition aus.
     * Blockiert die Auswahl, wenn das Level zu niedrig ist.
     */
    public void selectPartFromShop(Point clickPos) {
        int shopItemHeight = 160;
        int yOffset = 150;
        for (PartType type : PartType.values()) {
            Rectangle itemBounds = new Rectangle(10, (int) (yOffset - model.shopScrollY), SIDE_PANEL_WIDTH - 20, shopItemHeight + 20);
            if (itemBounds.contains(clickPos)) {

                // --- PROGRESSION CHECK ---
                // Prüfen, ob das Level des Spielers ausreicht
                if (model.currentLevel >= type.requiredLevel) {
                    model.selectedPartType = type;
                } else {
                    // Level zu niedrig: Teil wird nicht ausgewählt.
                    // Optional: Hier könnte man ein Sound-Feedback abspielen.
                    System.out.println("Teil gesperrt! Benötigt Level " + type.requiredLevel);
                }
                break;
            }
            yOffset += shopItemHeight + 40;
        }
    }

    public static boolean isPlacementLegal(GameModel model, PartType partType, int x, int y) {
        if (model.rocket.isEmpty()) {
            return partType == PartType.COCKPIT && x == GRID_WIDTH / 2;
        }

        boolean isAdjacent = (y > 0 && model.grid[x][y - 1] != null) || (y < GRID_HEIGHT - 1 && model.grid[x][y + 1] != null) ||
                (x > 0 && model.grid[x - 1][y] != null) || (x < GRID_WIDTH - 1 && model.grid[x + 1][y] != null);
        if (!isAdjacent) return false;

        if (partType.thrust > 0) {
            if (y > 0) {
                for (int i = 0; i < GRID_WIDTH; i++) {
                    if (model.grid[i][y - 1] != null && model.grid[i][y - 1].type.thrust > 0) return false;
                }
            }
            if (y < GRID_HEIGHT - 1) {
                for (int i = 0; i < GRID_WIDTH; i++) {
                    if (model.grid[i][y + 1] != null && model.grid[i][y + 1].type.thrust > 0) return false;
                }
            }
            for (int i = 0; i < GRID_WIDTH; i++) {
                if (model.grid[i][y] != null && model.grid[i][y].type == PartType.FUEL_TANK) return false;
            }

            RocketPart partAbove = (y > 0) ? model.grid[x][y - 1] : null;
            return partAbove != null && partAbove.type == PartType.FUEL_TANK;
        }

        if (partType == PartType.FUEL_TANK) {
            for (int i = 0; i < GRID_WIDTH; i++) {
                if (model.grid[i][y] != null && model.grid[i][y].type.thrust > 0) return false;
            }
        }

        return true;
    }

    public static void recalculateStages(GameModel model) {
        model.stages.clear();
        List<RocketPart> allEngines = model.rocket.stream().filter(p -> p.type.thrust > 0).toList();
        List<RocketPart> unassignedParts = new ArrayList<>(model.rocket);

        allEngines.stream()
                .collect(Collectors.groupingBy(e -> e.gridY))
                .values().stream()
                .sorted(Comparator.comparingInt(list -> list.getFirst().gridY))
                .forEach(engineGroup -> {
                    Stage newStage = new Stage(engineGroup);
                    List<RocketPart> partsToSearch = new ArrayList<>(engineGroup);
                    while (!partsToSearch.isEmpty()) {
                        RocketPart current = partsToSearch.removeFirst();
                        if (unassignedParts.contains(current) && !newStage.parts.contains(current)) {
                            newStage.parts.add(current);
                            unassignedParts.remove(current);
                            int x = current.gridX, y = current.gridY;
                            if (y > 0 && model.grid[x][y - 1] != null && model.grid[x][y - 1].type.thrust == 0)
                                partsToSearch.add(model.grid[x][y - 1]);
                            if (x > 0 && model.grid[x - 1][y] != null && model.grid[x - 1][y].type.thrust == 0)
                                partsToSearch.add(model.grid[x - 1][y]);
                            if (x < GRID_WIDTH - 1 && model.grid[x + 1][y] != null && model.grid[x + 1][y].type.thrust == 0)
                                partsToSearch.add(model.grid[x + 1][y]);
                        }
                    }
                    model.stages.add(newStage);
                });

        if (!unassignedParts.isEmpty()) {
            Stage finalStage = new Stage();
            finalStage.parts.addAll(unassignedParts);
            model.stages.add(0, finalStage);
        }
    }
}