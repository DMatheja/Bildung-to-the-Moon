package de.bildung.moon.model;

import java.awt.Color;
import java.util.List;

/**
 * Vereinfachtes Enum für Part-Kategorien. Die konkreten Bauteil-Prototypen
 * werden als `RocketPart`-Objekte unten in dieser Datei angelegt.
 */
public enum PartType {
    COCKPIT,
    FUEL_TANK,
    ENGINE;

    // Liste aller verfügbaren Bauteil-Prototypen (ersetzt die bisherigen vielen Enum-Einträge)
    public static final List<RocketPart> PARTS = List.of(
        new RocketPart(COCKPIT, "Generic Cockpit", 200, Color.CYAN, 2000, 0, 0, 0, RocketPart.ShapeKind.TRIANGLE),
        new RocketPart(COCKPIT, "Apollo Command Module", 300, Color.MAGENTA, 5560, 0, 0, 0, RocketPart.ShapeKind.TRIANGLE),
        new RocketPart(COCKPIT, "Mercury Capsule", 250, Color.PINK, 1460, 0, 0, 0, RocketPart.ShapeKind.TRIANGLE),
        new RocketPart(COCKPIT, "Wostok Capsule", 220, Color.RED, 2460, 0, 0, 0, RocketPart.ShapeKind.TRIANGLE),
        new RocketPart(COCKPIT, "Sojus Capsule", 270, Color.ORANGE, 2900, 0, 0, 0, RocketPart.ShapeKind.TRIANGLE),
        new RocketPart(COCKPIT, "ESA ATV Control Module", 320, Color.YELLOW, 4500, 0, 0, 0, RocketPart.ShapeKind.TRIANGLE),

        new RocketPart(FUEL_TANK, "Fuel Tank (generic small)", 100, Color.LIGHT_GRAY, 500, 20, 0, 0, RocketPart.ShapeKind.SQUARE),
        new RocketPart(FUEL_TANK, "Saturn V First Stage Tank", 400, Color.DARK_GRAY, 130000, 80, 0, 0, RocketPart.ShapeKind.SQUARE),
        new RocketPart(FUEL_TANK, "Delta IV Common Booster Core", 350, Color.GRAY, 26000, 70, 0, 0, RocketPart.ShapeKind.SQUARE),
        new RocketPart(FUEL_TANK, "Proton UR-500 Tanks", 300, Color.BLACK, 30000, 60, 0, 0, RocketPart.ShapeKind.SQUARE),
        new RocketPart(FUEL_TANK, "Ariane 5 EPC Main Stage Tank", 450, Color.BLUE, 21000, 90, 0, 0, RocketPart.ShapeKind.SQUARE),

        new RocketPart(ENGINE, "A-4 Engine", 150, Color.decode("#C0C0C0"), 1500, 0, 245, 239, RocketPart.ShapeKind.ENGINE),
        new RocketPart(ENGINE, "F-1 Engine", 500, Color.DARK_GRAY, 8400, 0, 7770, 304, RocketPart.ShapeKind.ENGINE),
        new RocketPart(ENGINE, "RD-180 Engine", 450, Color.GRAY, 5480, 0, 4164, 337, RocketPart.ShapeKind.ENGINE),
        new RocketPart(ENGINE, "RD-170 Engine", 700, Color.BLACK, 9750, 0, 7904, 337, RocketPart.ShapeKind.ENGINE),
        new RocketPart(ENGINE, "Vulcain 2 Engine", 350, Color.BLUE, 1800, 0, 1340, 429, RocketPart.ShapeKind.ENGINE),
        new RocketPart(ENGINE, "RL-10 Engine", 120, Color.GREEN, 280, 0, 110, 465, RocketPart.ShapeKind.ENGINE),
        new RocketPart(ENGINE, "T1 Engine", 600, Color.ORANGE, 6000, 0, 20000, 500, RocketPart.ShapeKind.ENGINE)
    );
}