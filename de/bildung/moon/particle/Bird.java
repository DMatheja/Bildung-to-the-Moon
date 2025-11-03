package de.bildung.moon.particle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public class Bird extends Particle {
    double parallaxFactor;

    public Bird(int panelWidth, int panelHeight) {
        super(Math.random() * panelWidth, Math.random() * panelHeight * 2);
        this.velX = (Math.random() - 0.5) * 150;
        this.parallaxFactor = Math.random() * 0.4 + 0.5; // Nearer
        this.color = Color.DARK_GRAY;
    }

    @Override
    public void draw(Graphics2D g2d, double cameraY, double altitude) {
        double screenY = this.y - cameraY * this.parallaxFactor;
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(3));
        int[] xPoints = {(int) x - 15, (int) x, (int) x + 15};
        int[] yPoints = {(int) screenY, (int) screenY + 10, (int) screenY};
        g2d.drawPolyline(xPoints, yPoints, 3);
    }
}
