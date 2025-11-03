package de.bildung.moon;

import java.util.ArrayList;
import java.util.List;

/**
 * Datenklasse zur Speicherung von abgetrennten Stufen, die
 * ihre eigene Physik (Geschwindigkeit, Höhe) haben.
 */
public class DetachedStage {
    public List<RocketPart> parts = new ArrayList<>();
    public double velY;
    public double altitude;

    public DetachedStage(List<RocketPart> parts, double startVelY, double startAltitude) {
        this.parts.addAll(parts);
        this.velY = startVelY;
        this.altitude = startAltitude;
    }
}
