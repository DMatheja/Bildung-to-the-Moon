package de.bildung.moon.model;

/**
 * Eine zentrale Klasse zur Speicherung aller statischen Spielkonstanten.
 */
public final class GameConstants {
    
    // Privater Konstruktor, da dies eine Utility-Klasse ist
    private GameConstants() {}

    // --- Game & Physics Constants ---
    public static final int GRID_WIDTH = 7;
    public static final int GRID_HEIGHT = 13;
    public static final int CELL_SIZE = 70;
    public static final int SIDE_PANEL_WIDTH = 500;
    public static final int STARTING_MONEY = 1000;
    public static final double PIXELS_PER_METER = 1.0;
    public static final double METER_PER_MONEY = 100; //1 Cent pro Meter, muss dann später auf ganze 50er gerundet werden

    // --- Earth Constants for Atmosphere Model ---
    public static final double SEA_LEVEL_GRAVITY = 9.81;
    public static final double MOON_SURFACE_GRAVITY = 1.62;
    public static final double EARTH_RADIUS = 6371000;
    public static final double MOON_RADIUS = 1737500;
    public static final double LUNAR_DISTANCE = 384399000;
    public static final double SEA_LEVEL_AIR_DENSITY = 1.225;
    public static final double SCALE_HEIGHT = 8500;
    public static final double DRAG_CONSTANT = 0.008;
}
