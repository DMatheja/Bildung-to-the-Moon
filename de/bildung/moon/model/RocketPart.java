package de.bildung.moon.model;

import static de.bildung.moon.model.GameConstants.CELL_SIZE;

/**
 * Datenklasse zur Speicherung des Zustands eines einzelnen Raketenteils.
 */
public class RocketPart {
    public PartType type;
    public int gridX, gridY;
    public double worldX, worldY;
    public boolean isDetached = false;
    public double currentFuel;

    public RocketPart(PartType type, int gridX, int gridY) {
        this.type = type;
        this.gridX = gridX;
        this.gridY = gridY;
        this.worldX = gridX * CELL_SIZE; // Standard-Welt-X (wird im Build-Modus verwendet)
        this.worldY = gridY * CELL_SIZE; // Standard-Welt-Y
        this.currentFuel = type.fuelCapacity;
    }
}
