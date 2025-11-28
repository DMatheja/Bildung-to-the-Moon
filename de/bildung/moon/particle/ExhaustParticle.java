package de.bildung.moon.particle;

import java.awt.Color;
import java.awt.Graphics2D;
import static de.bildung.moon.model.GameConstants.PIXELS_PER_METER;

public class ExhaustParticle extends Particle {
    float alpha = 1.0f;
    int initialLifetime;

    public ExhaustParticle(double x, double y, double parentVelY) {
        super(x, y);
        this.velX = (Math.random() - 0.5) * 50 * PIXELS_PER_METER;
        this.velY = parentVelY * PIXELS_PER_METER + (Math.random() * 150 + 100) * PIXELS_PER_METER;
        this.lifetime = (int) (Math.random() * 40 + 30);
        this.initialLifetime = this.lifetime;
        this.color = Math.random() > 0.3 ? Color.ORANGE : Color.YELLOW;
    }

    @Override
    public void update(double deltaTime) {
        x += velX * deltaTime;
        y += velY * deltaTime;
        lifetime--;
        alpha = (float) lifetime / initialLifetime;
    }

    @Override
    public void draw(Graphics2D g2d, double cameraY, double altitude) {
        g2d.setColor(new Color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, Math.max(0, alpha)));
        g2d.fillRect((int) x, (int) y, 6, 6);
    }
}
