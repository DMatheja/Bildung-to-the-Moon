package de.bildung.moon.model;

import static de.bildung.moon.model.GameConstants.*;
import de.bildung.moon.particle.Particle;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import static de.bildung.moon.model.GameConstants.*;

/**
 * Das "Model" der Anwendung.
 * Diese Klasse enthält ALLE Spieldaten (Zustand, Spielerdaten, Physikwerte).
 * Sie enthält keine Logik, sondern dient nur als Datencontainer.
 */
public class GameModel {

    // --- Spielzustand ---
    public GameState currentState = GameState.BUILDING;

    // --- Raketen- und Spielerdaten ---
    public final RocketPart[][] grid = new RocketPart[GRID_WIDTH][GRID_HEIGHT];
    public final List<RocketPart> rocket = new ArrayList<>();
    public final List<Stage> stages = new ArrayList<>();
    
    public int playerMoney = STARTING_MONEY;
    public PartType selectedPartType = null;
    public boolean autoDetachEnabled = true;
    public boolean isOutOfFuel = false;


    public Point mousePos = new Point(0, 0);

    // --- Launch Animation & Physik-Zustand ---
    public long lastUpdateTime;
    public double timeScale = 1.0;
    public double timeSinceOutOfFuel = -1.0;
    public double cameraWorldY = 0;
    public double rocketVelY = 0;
    public double altitude = 0;
    public double maxAltitude = 0;
    public double gForce = 0;
    public double dynamicPressure = 0;
    public double maxDynamicPressure = 0;
    public double currentTotalMass = 0;
    public double missionTime = 0;

    // --- Progression & Highscores ---
    public double highScoreAltitude = 0;
    public int currentLevel = 1;
    // --- Visuelle Effekte ---
    public final List<Particle> particles = new ArrayList<>();
    public boolean particlesInitialized = false;
    public final List<DetachedStage> detachedStages = new ArrayList<>();
    
    // Konstruktor könnte hier Standardwerte initialisieren,
    // aber das meiste wird direkt bei der Deklaration erledigt.
    public GameModel() {
        // Initialisierungen, falls nötig
    }
}
