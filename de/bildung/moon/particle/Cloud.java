package de.bildung.moon.particle;

import java.awt.Color;
import java.awt.Graphics2D;

public class Cloud extends Particle {
    double scale, parallaxFactor;

    public Cloud(int panelWidth, int panelHeight) {
        super(Math.random() * (panelWidth + 400) - 200, Math.random() * panelHeight * 5 - panelHeight * 2);
        this.velX = (Math.random() - 0.5) * 40;
        this.scale = (Math.random() * 60 + 40) * 2;
        this.parallaxFactor = Math.random() * 0.3 + 0.2; // Further away
        this.color = new Color(255, 255, 255, 150);
    }
    
    @Override
    public void update(double deltaTime) {
        super.update(deltaTime);
        // Diese Logik wurde in die PhysicsEngine verschoben,
        // da nur diese die Panel-Breite kennt.
    }

    @Override
    public void draw(Graphics2D g2d, double cameraY, double altitude) {
        double screenY = this.y - cameraY * this.parallaxFactor;
        g2d.setColor(color);
        g2d.fillOval((int) x, (int) screenY, (int) scale, (int) (scale * 0.6));
    }
}

