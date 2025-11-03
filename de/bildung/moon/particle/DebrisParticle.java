package de.bildung.moon.particle;

import java.awt.Color;
import java.awt.Graphics2D;

public class DebrisParticle extends Particle {
    
    public DebrisParticle(double rocketWorldX, double cameraY, int panelHeight) {
        super(rocketWorldX + (Math.random() - 0.5) * 800, cameraY + panelHeight + Math.random() * 50);
        this.velY = -(Math.random() * 100 + 50);
        this.lifetime = (int) (Math.random() * 100 + 50);
        this.color = new Color(200, 200, 200, 100);
    }

    @Override
    public void update(double deltaTime) {
        y += velY * deltaTime * 15;
        lifetime--;
    }

    @Override
    public void draw(Graphics2D g2d, double cameraY, double altitude) {
        g2d.setColor(color);
        g2d.fillRect((int) x, (int) y, 3, 3);
    }
}

