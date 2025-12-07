package de.bildung.moon.model;

import java.awt.Color;
import java.awt.Graphics2D;

public enum PartType {
    // Level 1: Basics
    COCKPIT("Cockpit", 200, Color.CYAN, 10, 0, 0, 0, 1) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            // Dreieck
            g2d.fillPolygon(
                    new int[]{x, x + size, x + size / 2},
                    new int[]{y + size, y + size, y},
                    3
            );
        }
    },
    // Level 1: Kleiner Tank
    FUEL_TANK("Fuel Tank", 100, Color.LIGHT_GRAY, 10, 200, 0, 0, 1) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            // Quadrat
            g2d.fillRect(x, y, size, size);
        }
    },
    // Level 1: Schwache Engine (V2 Stil)
    A4_ENGINE("A-4 Engine", 250, Color.decode("#C0C0C0"), 100, 0, 8000, 100, 1) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            drawEngine(g2d, x, y, size);
        }
    },
    // Level 2: Starke Engine
    ENGINE_T1("T1 Engine", 500, Color.ORANGE, 100, 0, 50000, 100, 2) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            drawEngine(g2d, x, y, size);
        }
    },
    // Level 3: Ionen-Antrieb (Neu)
    // Hoher ISP (400) für Effizienz, aber sehr niedriger Schub (4000), kann kaum das Eigengewicht heben.
    ION_DRIVE("Ion Drive", 2000, Color.decode("#00008B"), 100, 0, 4000, 420, 3) {
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

    PartType(String n, int c, Color cl, double m, double fc, double t, int isp, int reqLevel) {
        name = n;
        cost = c;
        color = cl;
        mass = m;
        fuelCapacity = fc;
        thrust = t;
        specificImpulse = isp;
        requiredLevel = reqLevel;
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
}