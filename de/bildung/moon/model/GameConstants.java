package de.bildung.moon.model;

/**
 * Eine zentrale Klasse zur Speicherung aller statischen Spielkonstanten.
 */
public final class GameConstants {
    
    // Privater Konstruktor, da dies eine Utility-Klasse ist
    private GameConstants() {}

    // --- Game & Physics Constants ---
    public static final int GRID_WIDTH = 5;
    public static final int GRID_HEIGHT = 9;
    public static final int CELL_SIZE = 100;
    public static final int SIDE_PANEL_WIDTH = 350;
    public static final int STARTING_MONEY = 1000;
    public static final double PIXELS_PER_METER = 1.0;
    public static final int MONEY_PER_METER = 0.01; //1 Cent pro Meter, muss dann später auf ganze 50er gerundet werden

    // --- Earth Constants for Atmosphere Model ---
    public static final double SEA_LEVEL_GRAVITY = 9.81;
    public static final double EARTH_RADIUS = 6371000;
    public static final double SEA_LEVEL_AIR_DENSITY = 1.225;
    public static final double SCALE_HEIGHT = 8500;
    public static final double DRAG_CONSTANT = 0.008;
}
