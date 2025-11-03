package de.bildung.moon.particle;

import java.awt.Color;
import java.awt.Graphics2D;

public class Star extends Particle {
    double parallaxFactor;
    int size;

    public Star(int panelWidth, int panelHeight) {
        super(Math.random() * panelWidth, (Math.random() * panelHeight * 20) - (panelHeight * 10));
        this.parallaxFactor = 0.001; // Far: close to 0
        this.size = (int) (Math.random() * 2 + 3); // Bigger stars
    }

    @Override
    public void draw(Graphics2D g2d, double cameraY, double altitude) {
        if (altitude < 20000) return; // Visible earlier
        float alpha = (float) (Math.min(1.0, (altitude - 20000) / 40000.0) * (Math.random() * 0.5 + 0.5)); // Faster fade-in with twinkle
        g2d.setColor(new Color(1f, 1f, 1f, alpha));
        double screenY = this.y - cameraY * this.parallaxFactor;
        g2d.fillRect((int) x, (int) screenY, size, size);
    }
}
