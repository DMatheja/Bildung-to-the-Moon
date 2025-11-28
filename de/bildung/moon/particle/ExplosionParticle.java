package de.bildung.moon.particle;

import java.awt.Color;
import java.awt.Graphics2D;
import static de.bildung.moon.model.GameConstants.SEA_LEVEL_GRAVITY;

public class ExplosionParticle extends Particle {
    float alpha = 1.0f;
    int initialLifetime;

    public ExplosionParticle(double x, double y) {
        super(x, y);
        this.velX = (Math.random() - 0.5) * 300;
        this.velY = (Math.random() - 0.5) * 300;
        this.lifetime = (int) (Math.random() * 80 + 40);
        this.initialLifetime = this.lifetime;
        this.color = Math.random() > 0.5 ? Color.ORANGE : Color.YELLOW;
    }

    @Override
    public void update(double deltaTime) {
        x += velX * deltaTime;
        y += velY * deltaTime;
        velY += SEA_LEVEL_GRAVITY * 10 * deltaTime; // Schwerkraft auf Partikel
        lifetime--;
        alpha = (float) lifetime / initialLifetime;
    }

    @Override
    public void draw(Graphics2D g2d, double cameraY, double altitude) {
        g2d.setColor(new Color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, Math.max(0, alpha)));
        g2d.fillRect((int) x, (int) y, 8, 8);
    }
}
