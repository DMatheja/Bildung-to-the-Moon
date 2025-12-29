package de.bildung.moon.model;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

public enum PartType {

    // ==========================================
    // COCKPITS (Dreiecke)
    // ==========================================
    BASIC_COCKPIT("Basis Cockpit", 200, Color.CYAN, 10, 0, 0, 0, 1, PartSuperType.COCKPIT, createCockpitShapes()),
    APOLLO_COCKPIT("Apollo Kommandomodul", 300, Color.MAGENTA, 6, 0, 0, 0, 2, PartSuperType.COCKPIT, createCockpitShapes()),
    MERCURY_COCKPIT("Mercury Kapsel", 400, Color.GREEN, 4, 50, 0, 0, 3, PartSuperType.COCKPIT, createCockpitShapes()),

    // ==========================================
    // TANKS (Quadrate)
    // ==========================================
    BASIC_FUEL_TANK("Treibstofftank (Klein)", 100, Color.LIGHT_GRAY, 20, 200, 0, 0, 1, PartSuperType.FUEL_TANK, createTankShapes()),
    PROTON_FUEL_TANK("Proton UR-500 Tank", 500, Color.LIGHT_GRAY.brighter(), 25, 400, 0, 0, 3, PartSuperType.FUEL_TANK, createTankShapes()),
    SATURN_FUEL_TANK("Behemoth Erdantrieb", 1000, Color.LIGHT_GRAY.darker(), 50, 700, 0, 0, 4, PartSuperType.FUEL_TANK, createTankShapes()),

    // ==========================================
    // TRIEBWERKE
    // ==========================================
    A4_ENGINE("A-4 Triebwerk", 150, Color.decode("#C0C0C0"), 20, 0, 8000, 100, 1, PartSuperType.ENGINE, createEngineShapes(0.2)),
    ENGINE_T1("T1 Mehrzweck-Triebwerk", 750, Color.ORANGE, 50, 0, 30000, 200, 2, PartSuperType.ENGINE, createEngineShapes(0.2)),

    VACUUM_ENGINE("Vakuum-Triebwerk", 1500, Color.decode("#4682B4"), 50, 0, 15000, 350, 3, PartSuperType.ENGINE,
            List.of(
                    new Rectangle2D.Double(0, 0, 1.0, 0.33),
                    createNozzlePath(0.33, 0.05)
            )),

    HEAVY_LIFTER_ENGINE("F-1 Behemoth", 2000, Color.decode("#2F4F4F"), 150, 0, 1000000, 250, 4, PartSuperType.ENGINE,
            List.of(
                    new Rectangle2D.Double(0, 0, 1.0, 0.25),
                    new Rectangle2D.Double(0.05, 0.25, 0.9, 0.75) // Vereinfacht ohne Line2D für pure Shapes
            )),

    ION_DRIVE("Ionen-Antrieb", 2000, Color.decode("#00008B"), 100, 0, 3000, 2500, 5, PartSuperType.ENGINE,
            List.of(
                    new Rectangle2D.Double(0, 0, 1.0, 0.7),
                    new Ellipse2D.Double(0.1, 0.6, 0.8, 0.33)
            ));

    public final String name;
    public final int cost;
    public final Color color;
    public final double mass;          // Leermasse in kg
    public final double fuelCapacity;  // Treibstoffmasse in kg
    public final double thrust;        // Schub in Newton
    public final int specificImpulse;  // ISP in Sekunden (Effizienz)
    public final int requiredLevel;
    public final PartSuperType superType;
    public final List<Shape> Shapes;

    PartType(String n, int c, Color cl, double m, double fc, double t, int isp, int reqLevel, PartSuperType type, List<Shape> shapes) {
        name = n;
        cost = c;
        color = cl;
        mass = m;
        fuelCapacity = fc;
        thrust = t;
        specificImpulse = isp;
        requiredLevel = reqLevel;
        superType = type;
        this.Shapes = shapes;
    }

    public Color getDisplayColor(int playerLevel) {
        if (playerLevel < this.requiredLevel) {
            return Color.GRAY;
        }
        return this.color;
    }


// --- Hilfsmethoden zur Erstellung der relativen Shapes ---

    private static List<Shape> createCockpitShapes() {
        Path2D path = new Path2D.Double();
        path.moveTo(0, 1.0);
        path.lineTo(1.0, 1.0);
        path.lineTo(0.5, 0);
        path.closePath();
        return List.of(path);
    }

    private static List<Shape> createTankShapes() {
        return List.of(new Rectangle2D.Double(0, 0, 1.0, 1.0));
    }

    private static List<Shape> createEngineShapes(double nozzleInset) {
        return List.of(
                new Rectangle2D.Double(0, 0, 1.0, 0.5),
                createNozzlePath(0.5, nozzleInset)
        );
    }

    private static Path2D createNozzlePath(double startY, double inset) {
        Path2D path = new Path2D.Double();
        path.moveTo(0, startY);
        path.lineTo(1.0, startY);
        path.lineTo(1.0 - inset, 1.0);
        path.lineTo(inset, 1.0);
        path.closePath();
        return path;
    }
}