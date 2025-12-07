package de.bildung.moon.model;

import java.awt.Color;
import static de.bildung.moon.model.GameConstants.CELL_SIZE;

/**
 * Datenklasse zur Speicherung des Zustands eines einzelnen Raketenteils.
 * Diese Klasse repräsentiert sowohl Prototypen (ohne Grid-Koordinaten)
 * als auch platzierte Instanzen (mit Grid-Koordinaten und Welt-Position).
 */
public class RocketPart {
    public enum ShapeKind { TRIANGLE, SQUARE, ENGINE }

    // Kategorisierung (Cockpit / Tank / Engine)
    public PartType category;

    // Eigenschaften
    public String name;
    public int cost;
    public Color color;
    public double mass;
    public double fuelCapacity;
    public double thrust;
    public int specificImpulse;
    public ShapeKind shapeKind;

    // Platzierungs- / Simulationszustand
    public int gridX = -1, gridY = -1;
    public double worldX = 0, worldY = 0;
    public boolean isDetached = false;
    public double currentFuel;

    /**
     * Konstruktor für Prototypen (keine Grid-Koordinaten).
     */
    public RocketPart(PartType category, String name, int cost, Color color, double mass, double fuelCapacity, double thrust, int specificImpulse, ShapeKind shapeKind) {
        this.category = category;
        this.name = name;
        this.cost = cost;
        this.color = color;
        this.mass = mass;
        this.fuelCapacity = fuelCapacity;
        this.thrust = thrust;
        this.specificImpulse = specificImpulse;
        this.shapeKind = shapeKind;
        this.currentFuel = fuelCapacity;
    }

    /**
     * Erstellt eine platzierte Kopie dieses Prototyps an den angegebenen Gitter-Koordinaten.
     */
    public RocketPart copyForGrid(int gridX, int gridY) {
        RocketPart p = new RocketPart(this.category, this.name, this.cost, this.color, this.mass, this.fuelCapacity, this.thrust, this.specificImpulse, this.shapeKind);
        p.gridX = gridX;
        p.gridY = gridY;
        p.worldX = gridX * CELL_SIZE;
        p.worldY = gridY * CELL_SIZE;
        p.currentFuel = this.fuelCapacity;
        p.isDetached = false;
        return p;
    }
}
