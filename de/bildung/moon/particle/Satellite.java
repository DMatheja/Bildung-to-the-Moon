package de.bildung.moon.particle;

import java.awt.Color;
import java.awt.Graphics2D;

public class Satellite extends Particle {
    double parallaxFactor;

    public Satellite(int panelWidth, int panelHeight) {
        super(Math.random() * panelWidth, Math.random() * panelHeight);
        this.velX = (Math.random() > 0.5 ? 1 : -1) * 200;
        this.parallaxFactor = 0.02; // Very far: close to 0
        this.color = Color.LIGHT_GRAY;
    }

    @Override
    public void draw(Graphics2D g2d, double cameraY, double altitude) {
        double screenY = this.y - cameraY * this.parallaxFactor;
        g2d.setColor(color);
        g2d.fillRect((int) x - 10, (int) screenY - 3, 20, 6);
        g2d.drawLine((int) x - 25, (int) screenY - 10, (int) x + 25, (int) screenY + 10);
        g2d.drawLine((int) x - 25, (int) screenY + 10, (int) x + 25, (int) screenY - 10);
    }
}
