package de.bildung.moon.model;

import java.awt.Color;
import java.awt.Graphics2D;

public enum PartType {

    // ==========================================
    // COCKPITS
    // ==========================================

    // Level 1: Basics
    BASIC_COCKPIT("Basis Cockpit", 200, Color.CYAN, 10, 0, 0, 0, 1, PartSuperType.COCKPIT) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            drawCockpit(g2d, x, y, size);
        }
    },
    // Level 2: Apollo (Upgrade für den Orbit)
    APOLLO_COCKPIT("Apollo Kommandomodul", 300, Color.MAGENTA, 6, 0, 0, 0, 2, PartSuperType.COCKPIT) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            drawCockpit(g2d, x, y, size);
        }
    },
    // Level 3: Mercury (Leichtbau für weite Reisen)
    MERCURY_COCKPIT("Mercury Kapsel", 400, Color.GREEN, 4, 50, 0, 0, 3, PartSuperType.COCKPIT) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            drawCockpit(g2d, x, y, size);
        }
    },

    // ==========================================
    // TREIBSTOFFTANKS
    // ==========================================

    // Level 1: Kleiner Tank
    BASIC_FUEL_TANK("Treibstofftank (Klein)", 100, Color.LIGHT_GRAY, 20, 200, 0, 0, 1, PartSuperType.FUEL_TANK) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            drawTank(g2d, x, y, size);
        }
    },

    // Level 2: Proton (Verschoben von Lvl 3 auf 2)
    // Dieser Tank ist effizienter und passt gut zum T1 Triebwerk, um den Orbit zu erreichen.
    PROTON_FUEL_TANK("Proton UR-500 Tank", 500, Color.LIGHT_GRAY.brighter(), 25, 400, 0, 0, 3, PartSuperType.FUEL_TANK) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            drawTank(g2d, x, y, size);
        }
    },

    // Level 4: Saturn V Tank (Verschoben von Lvl 2 auf 4)
    // Kommt jetzt zusammen mit dem Behemoth Triebwerk für Schwerlast-Raketen.
    // Preis angepasst (teurer).
    SATURN_FUEL_TANK("Saturn V Stufe-1 Tank", 1000, Color.LIGHT_GRAY.darker(), 50, 700, 0, 0, 4, PartSuperType.FUEL_TANK) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            drawTank(g2d, x, y, size);
        }
    },

    // ==========================================
    // TRIEBWERKE
    // ==========================================

    // Level 1: Einstieg
    A4_ENGINE("A-4 Triebwerk", 150, Color.decode("#C0C0C0"), 20, 0, 8000, 100, 1, PartSuperType.ENGINE) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            drawEngine(g2d, x, y, size);
        }
    },

    // Level 2: Der Allrounder
    ENGINE_T1("T1 Mehrzweck-Triebwerk", 750, Color.ORANGE, 50, 0, 30000, 200, 2, PartSuperType.ENGINE) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            drawEngine(g2d, x, y, size);
        }
    },

    // Level 3: Vakuum-Spezialist (Verschoben auf 3)
    VACUUM_ENGINE("J-2 Voyager Vakuum-Triebwerk", 1500, Color.decode("#4682B4"), 50, 0, 15000, 350, 3, PartSuperType.ENGINE) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            int nozzleInset = (int)(size * 0.05);
            g2d.fillRect(x, y, size, size / 3);
            g2d.fillPolygon(
                    new int[]{x, x + size, x + size - nozzleInset, x + nozzleInset},
                    new int[]{y + size / 3, y + size / 3, y + size, y + size},
                    4
            );
        }
    },

    // Level 4: Heavy Lifter (Verschoben auf 4)
    // Zusammen mit dem Saturn Tank das ultimative Start-Paket.
    HEAVY_LIFTER_ENGINE("F-1 Behemoth", 2000, Color.decode("#2F4F4F"), 150, 0, 1000000, 250, 4, PartSuperType.ENGINE) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            g2d.fillRect(x, y, size, size / 4);
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect(x + 2, y + size / 4, size - 4, size - (size/4));
            g2d.setColor(Color.BLACK);
            g2d.drawLine(x + size/2, y + size/4, x + size/2, y + size);
        }
    },

    // Level 5: High-Tech Endgame (Verschoben auf 5)
    ION_DRIVE("Ionen-Antrieb", 2000, Color.decode("#00008B"), 100, 0, 3000, 2500, 5, PartSuperType.ENGINE) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            int nozzleInset = (int)(size * 0.1);
            g2d.fillRect(x, y, size, (int)(size * 0.7));
            g2d.setColor(new Color(100, 200, 255));
            g2d.fillOval(x + nozzleInset, y + (int)(size * 0.6), size - 2 * nozzleInset, size / 3);
        }
    };

    public final String name;
    public final int cost;
    public final Color color;
    public final double mass;          // Leermasse in kg
    public final double fuelCapacity;  // Treibstoffmasse in kg
    public final double thrust;        // Schub in Newton
    public final int specificImpulse;  // ISP in Sekunden (Effizienz)
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

    public Color getDisplayColor(int playerLevel) {
        if (playerLevel < this.requiredLevel) {
            return Color.GRAY;
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
        g2d.fillRect(x, y, size, size);
    }
}