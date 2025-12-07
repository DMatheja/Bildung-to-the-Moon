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
    // Level 2: Starke Engine (erst später verfügbar!)
    ENGINE_T1("T1 Engine", 500, Color.ORANGE, 100, 0, 50000, 100, 2) {
        @Override
        public void renderShape(Graphics2D g2d, int x, int y, int size) {
            drawEngine(g2d, x, y, size);
        }
    };

    public final String name;
    public final int cost;
    public final Color color;
    public final double mass;
    public final double fuelCapacity;
    public final double thrust;
    public final int specificImpulse;
    public final int requiredLevel; // NEU: Das benötigte Level

    PartType(String n, int c, Color cl, double m, double fc, double t, int isp, int reqLevel) {
        name = n;
        cost = c;
        color = cl;
        mass = m;
        fuelCapacity = fc;
        thrust = t;
        specificImpulse = isp;
        requiredLevel = reqLevel; // Standardmäßig Level 1
    }

    // Abstrakte Methode, die jedes Enum-Element implementieren muss
    public abstract void renderShape(Graphics2D g2d, int x, int y, int size);

    // Hilfsmethode für Engines, um Code-Duplizierung zu vermeiden
    protected void drawEngine(Graphics2D g2d, int x, int y, int size) {
        int nozzleInset = (int)(size * 0.2); // Ersetzt die fixen '20' Pixel für Skalierbarkeit
        
        // Oberer Teil (Rechteck)
        g2d.fillRect(x, y, size, size / 2);
        
        // Unterer Teil (Trapez/Düse)
        g2d.fillPolygon(
            new int[]{x, x + size, x + size - nozzleInset, x + nozzleInset}, 
            new int[]{y + size / 2, y + size / 2, y + size, y + size}, 
            4
        );
    }
}