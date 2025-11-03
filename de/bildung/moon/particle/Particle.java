package de.bildung.moon.particle;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Abstrakte Basisklasse für alle Partikel im Spiel.
 */
public abstract class Particle {
    public double x, y, velX, velY;
    public int lifetime;
    public Color color;

    public Particle(double x, double y) {
        this.x = x;
        this.y = y;
        this.lifetime = Integer.MAX_VALUE; // Standardmäßig unendlich
    }

    public void update(double deltaTime) {
        x += velX * deltaTime;
        y += velY * deltaTime;
        lifetime--;
    }

    public abstract void draw(Graphics2D g2d, double cameraY, double altitude);

    public boolean isDead() {
        return lifetime <= 0;
    }
}
