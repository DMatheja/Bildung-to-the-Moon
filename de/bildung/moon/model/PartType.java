package de.bildung.moon.model;

import java.awt.Color;

/**
 * Definiert alle verfügbaren Raketenteile und ihre Eigenschaften.
 */
public enum PartType {  //Hier müssen die Render-Eigenschaften ergänzt werden ( Polygone ) Sind momentan im Renderer
    COCKPIT("Cockpit", 200, Color.CYAN, 50, 0, 0, 0),
    FUEL_TANK("Fuel Tank", 100, Color.LIGHT_GRAY, 10, 200, 0, 0),
    A4_ENGINE("A-4 Engine", 250, Color.decode("#C0C0C0"), 120, 0, 8000, 100),
    ENGINE_T1("T1 Engine", 500, Color.ORANGE, 50, 0, 50000, 300);

    public final String name;
    public final int cost;
    public final Color color;
    public final double mass;
    public final double fuelCapacity;
    public final double thrust;
    public final int specificImpulse;

    PartType(String n, int c, Color cl, double m, double fc, double t, int isp) {
        name = n;
        cost = c;
        color = cl;
        mass = m;
        fuelCapacity = fc;
        thrust = t;
        specificImpulse = isp;
    }
}
