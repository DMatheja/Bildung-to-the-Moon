package de.bildung.moon.model;

import java.awt.Color;
import java.awt.Graphics2D;

public enum PartType {
    // Level 1: Basics
    BASIC_COCKPIT("Basic Cockpit", 200, Color.CYAN, 10, 0, 0, 0, 1, PartSuperType.COCKPIT) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            drawCockpit(g2d, x, y, size);
        }
    },
    // Level 2: Apollo Command Module
    APOLLO_COCKPIT("Apollo Command Module", 300, Color.MAGENTA, 8, 0, 0, 0, 2, PartSuperType.COCKPIT) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            drawCockpit(g2d, x, y, size);
        }
    },
    // Level 3: Mercury Capsule
    MERCURY_COCKPIT("Mercury Capsule", 400, Color.GREEN, 12, 50, 0, 0, 3, PartSuperType.COCKPIT) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            drawCockpit(g2d, x, y, size);
        }
    },
    // Level 1: Kleiner Tank
    BASIC_FUEL_TANK("Fuel Tank", 100, Color.LIGHT_GRAY, 10, 200, 0, 0, 1, PartSuperType.FUEL_TANK) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            drawTank(g2d, x, y, size);
        }
    },
    // Level 2: Saturn V First Stage Tank
    SATURN_FUEL_TANK("Saturn V First Stage Tank", 150, Color.LIGHT_GRAY.darker(), 30, 500, 0, 0, 2, PartSuperType.FUEL_TANK) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            drawTank(g2d, x, y, size);
        }
    },
    // Level 3: Proton UR-500 Tanks
    PROTON_FUEL_TANK("Proton UR-500 Tanks", 300, Color.LIGHT_GRAY.brighter(), 5, 150, 0, 0, 3, PartSuperType.FUEL_TANK) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            drawTank(g2d, x, y, size);
        }
    },
    // Level 1: Schwache Engine (V2 Stil)
    A4_ENGINE("A-4 Engine", 250, Color.decode("#C0C0C0"), 80, 0, 8000, 100, 1, PartSuperType.ENGINE) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            drawEngine(g2d, x, y, size);
        }
    },
    // Level 2: Starke Engine
    ENGINE_T1("T1 Engine", 500, Color.ORANGE, 100, 0, 55000, 150, 2, PartSuperType.ENGINE) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            drawEngine(g2d, x, y, size);
        }
    },
    // Level 3: Ionen-Antrieb (Neu)
    // Hoher ISP (400) für Effizienz, aber sehr niedriger Schub (4000), kann kaum das Eigengewicht heben.
    ION_DRIVE("Ion Drive", 2000, Color.decode("#00008B"), 100, 0, 4500, 420, 3, PartSuperType.ENGINE) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            // Spezielle Optik für Ionen-Antrieb: Breiter, flacher Auslass
            int nozzleInset = (int)(size * 0.1);

            // Gehäuse (Dunkel)
            g2d.fillRect(x, y, size, (int)(size * 0.7));

            // Das "Leuchten" des Ionenstrahls (Optionales Detail durch Farbe im Spiel sichtbar)
            g2d.fillOval(x + nozzleInset, y + (int)(size * 0.5), size - 2 * nozzleInset, size / 2);
        }
    };

    public final String name;
    public final int cost;
    public final Color color;
    public final double mass;
    public final double fuelCapacity;
    public final double thrust;
    public final int specificImpulse; // ISP: Maß für die Effizienz
    public final int requiredLevel;
    public final PartSuperType superType;

    PartType(String n, int c, Color cl, double m, double fc, double t, int isp, int reqLevel, PartSuperType type) {
        name = n;
        cost = c;
        color = cl;
        mass = m;
        fuelCapacity = fc;
        thrust = t;
        specificImpulse = isp;
        requiredLevel = reqLevel;
        superType = type;
    }

    /**
     * Gibt die Farbe zurück. Wenn das Spieler-Level zu niedrig ist, wird Grau zurückgegeben.
     */
    public Color getDisplayColor(int playerLevel) {
        if (playerLevel < this.requiredLevel) {
            return Color.GRAY; // Ausgegraut, wenn noch nicht freigeschaltet
        }
        return this.color;
    }

    public abstract void renderShape(Graphics2D g2d, int x, int y, int size);

    protected void drawEngine(Graphics2D g2d, int x, int y, int size) {
        int nozzleInset = (int)(size * 0.2);
        g2d.fillRect(x, y, size, size / 2);
        g2d.fillPolygon(
                new int[]{x, x + size, x + size - nozzleInset, x + nozzleInset},
                new int[]{y + size / 2, y + size / 2, y + size, y + size},
                4
        );
    }

    protected void drawCockpit(Graphics2D g2d, int x, int y, int size){
        g2d.fillPolygon(
                new int[]{x, x + size, x + size / 2},
                new int[]{y + size, y + size, y},
                3
        );
    }

    protected void drawTank(Graphics2D g2d, int x, int y, int size){
        // Quadrat
        g2d.fillRect(x, y, size, size);
    }
}

/*// Liste aller verfügbaren Bauteil-Prototypen (ersetzt die bisherigen vielen Enum-Einträge)
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
    );*/