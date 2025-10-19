import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Simple launcher UI for the "Bildung to the Moon" game.
 */
public class Launcher {
    private final JFrame frame;

    public Launcher() {
        frame = new JFrame("Bildung to the Moon - Launcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 220);
        frame.setLocationRelativeTo(null);
        frame.setContentPane(createMainPanel());
        frame.setVisible(true);
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel title = new JLabel("<html><div style='text-align:center'>Bildung to the Moon</div></html>", JLabel.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        panel.add(title, BorderLayout.NORTH);
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(3, 1, 8, 8));
        JButton startBtn = new JButton("Start Game");
        startBtn.addActionListener(this::onStart);
        buttons.add(startBtn);
        JButton settingsBtn = new JButton("Settings");
        settingsBtn.addActionListener(this::onSettings);
        buttons.add(settingsBtn);
        JButton quitBtn = new JButton("Quit");
        quitBtn.addActionListener(e -> onQuit());
        buttons.add(quitBtn);
        panel.add(buttons, BorderLayout.CENTER);
        JLabel hint = new JLabel("Tip: implement your game and replace the placeholder.", JLabel.CENTER);
        hint.setFont(hint.getFont().deriveFont(11f));
        panel.add(hint, BorderLayout.SOUTH);
        return panel;
    }

    private void onStart(ActionEvent e) { SwingUtilities.invokeLater(() -> new GameWindow(frame)); frame.setVisible(false); }
    private void onSettings(ActionEvent e) { JOptionPane.showMessageDialog(frame, "Settings not yet implemented.", "Settings", JOptionPane.INFORMATION_MESSAGE); }
    private void onQuit() { frame.dispose(); System.exit(0); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Launcher::new);
    }

    private static class GameWindow extends JFrame {
        private final JFrame parent;
        private final GameCanvas gameCanvas;
        GameWindow(JFrame parent) {
            super("Bildung to the Moon - Rocket Builder");
            this.parent = parent;
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            gameCanvas = new GameCanvas();
            add(gameCanvas, BorderLayout.CENTER);
            pack();
            setMinimumSize(new Dimension(1200, 800));
            setLocationRelativeTo(null);
            addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosed(java.awt.event.WindowEvent e) { onReturn(); }
            });
            setVisible(true);
        }
        private void onReturn() {
            gameCanvas.stopGameLoop();
            dispose();
            if (parent != null) { parent.setVisible(true); }
        }
    }

    private static class GameCanvas extends JPanel {
        // --- Game & Physics Constants ---
        private static final int GRID_WIDTH = 5;
        private static final int GRID_HEIGHT = 9;
        private static final int CELL_SIZE = 100;
        private static final int SIDE_PANEL_WIDTH = 350;
        private static final int STARTING_MONEY = 5000;
        private static final double PIXELS_PER_METER = 1.0;

        // --- Earth Constants for Atmosphere Model ---
        private static final double SEA_LEVEL_GRAVITY = 9.81;
        private static final double EARTH_RADIUS = 6371000;
        private static final double SEA_LEVEL_AIR_DENSITY = 1.225;
        private static final double SCALE_HEIGHT = 8500;
        private static final double DRAG_CONSTANT = 0.008;

        private enum GameState { BUILDING, READY_FOR_LAUNCH, COUNTDOWN, LAUNCHING, EXPLODED }
        private GameState currentState = GameState.BUILDING;

        // --- Rocket and Player Data ---
        private final RocketPart[][] grid = new RocketPart[GRID_WIDTH][GRID_HEIGHT];
        private final List<RocketPart> placedParts = new ArrayList<>();
        private final List<Stage> stages = new ArrayList<>();
        private int playerMoney = STARTING_MONEY;
        private PartType selectedPartType = null;
        private boolean autoDetachEnabled = false;
        private boolean isOutOfFuel = false;
        
        // --- UI & Input ---
        private Point mousePos = new Point(0, 0);
        private double shopScrollY = 0;
        private final Rectangle launchButton = new Rectangle();
        private final Rectangle startButton = new Rectangle();
        private final Rectangle backToHangarButton = new Rectangle();
        private final Rectangle detachButton = new Rectangle();
        private final Rectangle autoDetachCheckbox = new Rectangle();
        private final Rectangle selfDestructButton = new Rectangle();

        // --- Launch Animation & Visuals ---
        private Timer gameLoop;
        private long lastUpdateTime;
        private double timeScale = 1.0;
        private double timeSinceOutOfFuel = -1.0;
        private double cameraWorldY = 0;
        private double rocketVelY = 0;
        private double altitude = 0;
        private double maxAltitude = 0;
        private double gForce = 0;
        private double dynamicPressure = 0;
        private double maxDynamicPressure = 0;
        private double currentTotalMass = 0;
        private double missionTime = 0;
        private final List<Particle> particles = new ArrayList<>();
        private boolean particlesInitialized = false;
        private final List<DetachedStage> detachedStages = new ArrayList<>();

        private static class RocketPart {
            PartType type; int gridX, gridY; double worldX, worldY; boolean isDetached = false; double currentFuel;
            RocketPart(PartType type, int gridX, int gridY) {
                this.type = type; this.gridX = gridX; this.gridY = gridY;
                this.worldX = gridX * CELL_SIZE; this.worldY = gridY * CELL_SIZE; this.currentFuel = type.fuelCapacity;
            }
        }
        
        private static class Stage {
            List<RocketPart> stageEngines = new ArrayList<>(); List<RocketPart> parts = new ArrayList<>();
            Stage(List<RocketPart> engines) { this.stageEngines.addAll(engines); }
            Stage() {}
            double getTotalCurrentFuel() { return parts.stream().mapToDouble(p -> p.currentFuel).sum(); }
            boolean hasActiveEngine() { return !stageEngines.isEmpty() && stageEngines.stream().noneMatch(e -> e.isDetached); }
            void consumeFuel(double amount) {
                List<RocketPart> fuelTanks = parts.stream().filter(p -> p.type == PartType.FUEL_TANK && p.currentFuel > 0).collect(Collectors.toList());
                if (fuelTanks.isEmpty()) return;
                double consumptionPerTank = amount / fuelTanks.size();
                for (RocketPart tank : fuelTanks) { tank.currentFuel = Math.max(0, tank.currentFuel - consumptionPerTank); }
            }
        }

        private static class DetachedStage {
            List<RocketPart> parts = new ArrayList<>(); double velY; double altitude;
            DetachedStage(List<RocketPart> parts, double startVelY, double startAltitude) {
                this.parts.addAll(parts); this.velY = startVelY; this.altitude = startAltitude;
            }
        }

        private enum PartType {
            COCKPIT("Cockpit", 200, Color.CYAN, 50, 0, 0, 0),
            FUEL_TANK("Fuel Tank", 100, Color.LIGHT_GRAY, 10, 200, 0, 0),
            A4_ENGINE("A-4 Engine", 250, Color.decode("#C0C0C0"), 120, 0, 8000, 100),
            ENGINE_T1("T1 Engine", 500, Color.ORANGE, 50, 0, 50000, 300);

            final String name; final int cost; final Color color; 
            final double mass; final double fuelCapacity; final double thrust; final int specificImpulse;
            PartType(String n, int c, Color cl, double m, double fc, double t, int isp) { 
                name=n; cost=c; color=cl; mass=m; fuelCapacity=fc; thrust=t; specificImpulse=isp;
            }
        }

        private static abstract class Particle {
            double x, y, velX, velY; int lifetime; Color color;
            Particle(double x, double y) { this.x = x; this.y = y; this.lifetime = Integer.MAX_VALUE; }
            void update(double deltaTime) { x += velX * deltaTime; }
            abstract void draw(Graphics2D g2d, double cameraY, double altitude);
            boolean isDead() { return lifetime <= 0; }
        }

        private static class Cloud extends Particle {
            double scale, parallaxFactor;
            Cloud(int panelWidth, int panelHeight) {
                super(Math.random() * (panelWidth + 400) - 200, Math.random() * panelHeight * 5 - panelHeight * 2);
                this.velX = (Math.random() - 0.5) * 40;
                this.scale = (Math.random() * 60 + 40) * 2;
                this.parallaxFactor = Math.random() * 0.3 + 0.2; // Further away
                this.color = new Color(255, 255, 255, 150);
            }
            @Override
            void draw(Graphics2D g2d, double cameraY, double altitude) {
                double screenY = this.y - cameraY * this.parallaxFactor;
                g2d.setColor(color);
                g2d.fillOval((int)x, (int)screenY, (int)scale, (int)(scale * 0.6));
            }
        }
        
        private static class Star extends Particle {
            double parallaxFactor;
            int size;
            Star(int panelWidth, int panelHeight) {
                super(Math.random() * panelWidth, (Math.random() * panelHeight * 20) - (panelHeight * 10)); 
                this.parallaxFactor = 0.001; // Far: close to 0
                this.size = (int)(Math.random() * 2 + 3); // Bigger stars
            }
            @Override
            void draw(Graphics2D g2d, double cameraY, double altitude) {
                if (altitude < 20000) return; // Visible earlier
                float alpha = (float)(Math.min(1.0, (altitude - 20000) / 40000.0) * (Math.random() * 0.5 + 0.5)); // Faster fade-in with twinkle
                g2d.setColor(new Color(1f, 1f, 1f, alpha));
                double screenY = this.y - cameraY * this.parallaxFactor;
                g2d.fillRect((int)x, (int)screenY, size, size);
            }
        }
        
        private static class DebrisParticle extends Particle {
            DebrisParticle(double rocketWorldX, double cameraY, int panelHeight) {
                super(rocketWorldX + (Math.random() - 0.5) * 800, cameraY + panelHeight + Math.random() * 50);
                this.velY = -(Math.random() * 100 + 50);
                this.lifetime = (int)(Math.random() * 100 + 50);
                this.color = new Color(200, 200, 200, 100);
            }
             @Override void update(double deltaTime) {
                y += velY * deltaTime * 15;
                lifetime--;
            }
            @Override
            void draw(Graphics2D g2d, double cameraY, double altitude) {
                g2d.setColor(color);
                g2d.fillRect((int)x, (int)y, 3, 3);
            }
        }

        private static class Bird extends Particle {
             double parallaxFactor;
             Bird(int panelWidth, int panelHeight) {
                super(Math.random() * panelWidth, Math.random() * panelHeight * 2);
                this.velX = (Math.random() - 0.5) * 150;
                this.parallaxFactor = Math.random() * 0.4 + 0.5; // Nearer
                this.color = Color.DARK_GRAY;
            }
             @Override
            void draw(Graphics2D g2d, double cameraY, double altitude) {
                double screenY = this.y - cameraY * this.parallaxFactor;
                g2d.setColor(color);
                g2d.setStroke(new BasicStroke(3));
                int[] xPoints = {(int)x - 15, (int)x, (int)x + 15};
                int[] yPoints = {(int)screenY, (int)screenY + 10, (int)screenY};
                g2d.drawPolyline(xPoints, yPoints, 3);
            }
        }

        private static class Satellite extends Particle {
            double parallaxFactor;
            Satellite(int panelWidth, int panelHeight) {
                super(Math.random() * panelWidth, Math.random() * panelHeight);
                this.velX = (Math.random() > 0.5 ? 1 : -1) * 200;
                this.parallaxFactor = 0.02; // Very far: close to 0
                this.color = Color.LIGHT_GRAY;
            }
            @Override
            void draw(Graphics2D g2d, double cameraY, double altitude) {
                double screenY = this.y - cameraY * this.parallaxFactor;
                g2d.setColor(color);
                g2d.fillRect((int)x - 10, (int)screenY - 3, 20, 6);
                g2d.drawLine((int)x - 25, (int)screenY - 10, (int)x + 25, (int)screenY + 10);
                g2d.drawLine((int)x - 25, (int)screenY + 10, (int)x + 25, (int)screenY - 10);
            }
        }

        private static class ExplosionParticle extends Particle {
            float alpha = 1.0f;
            int initialLifetime;
            ExplosionParticle(double x, double y) {
                super(x,y);
                this.velX = (Math.random() - 0.5) * 300; this.velY = (Math.random() - 0.5) * 300;
                this.lifetime = (int)(Math.random() * 80 + 40);
                this.initialLifetime = this.lifetime;
                this.color = Math.random() > 0.5 ? Color.ORANGE : Color.YELLOW;
            }
            @Override void update(double deltaTime) { 
                x += velX * deltaTime;
                y += velY * deltaTime;
                velY += SEA_LEVEL_GRAVITY * 10 * deltaTime;
                lifetime--; 
                alpha = (float)lifetime / initialLifetime;
            }
            @Override void draw(Graphics2D g2d, double cameraY, double altitude) {
                 g2d.setColor(new Color(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, Math.max(0, alpha)));
                 g2d.fillRect((int)x, (int)y, 8, 8);
            }
        }

        private static class ExhaustParticle extends Particle {
            float alpha = 1.0f;
            int initialLifetime;
            ExhaustParticle(double x, double y, double parentVelY) {
                super(x,y);
                this.velX = (Math.random() - 0.5) * 50 * PIXELS_PER_METER;
                this.velY = parentVelY * PIXELS_PER_METER + (Math.random() * 150 + 100) * PIXELS_PER_METER;
                this.lifetime = (int)(Math.random() * 40 + 30);
                this.initialLifetime = this.lifetime;
                this.color = Math.random() > 0.3 ? Color.ORANGE : Color.YELLOW;
            }
            @Override void update(double deltaTime) {
                x += velX * deltaTime;
                y += velY * deltaTime;
                lifetime--;
                alpha = (float)lifetime / initialLifetime;
            }
            @Override void draw(Graphics2D g2d, double cameraY, double altitude) {
                g2d.setColor(new Color(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, Math.max(0, alpha)));
                g2d.fillRect((int)x, (int)y, 6, 6);
            }
        }

        public GameCanvas() {
            int panelWidth = SIDE_PANEL_WIDTH + GRID_WIDTH * CELL_SIZE + SIDE_PANEL_WIDTH; 
            int panelHeight = GRID_HEIGHT * CELL_SIZE;
            setPreferredSize(new Dimension(panelWidth, panelHeight)); setBackground(Color.DARK_GRAY);
            setupInputHandlers();
        }
        
        private void resetBackgroundParticles(int w, int h) {
            particles.clear();
            for (int i = 0; i < 50; i++) { particles.add(new Cloud(w, h)); }
            for (int i = 0; i < 20; i++) { particles.add(new Bird(w, h)); }
            for (int i = 0; i < 10; i++) { particles.add(new Satellite(w, h)); }
            for (int i = 0; i < 400; i++) { particles.add(new Star(w, h)); }
        }

        private void setupInputHandlers() { 
            MouseAdapter adapter = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) { mousePos = e.getPoint(); repaint(); }
                @Override public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) { handleMouseClick(e.getPoint()); } 
                    else if (SwingUtilities.isRightMouseButton(e)) { handleRightClick(e.getPoint()); }
                }
                @Override public void mouseWheelMoved(MouseWheelEvent e) {
                    if (currentState == GameState.BUILDING && getWidth() > 0) {
                        if(mousePos.x < SIDE_PANEL_WIDTH){
                            shopScrollY += e.getWheelRotation() * 25;
                            int totalItemHeight = PartType.values().length * 180;
                            int visibleHeight = getHeight() - 140 - 150;
                            int maxScroll = Math.max(0, totalItemHeight - visibleHeight);
                            if (shopScrollY < 0) shopScrollY = 0;
                            if (shopScrollY > maxScroll) shopScrollY = maxScroll;
                            repaint();
                        }
                    }
                }
            };
            addMouseListener(adapter); 
            addMouseMotionListener(adapter);
            addMouseWheelListener(adapter);
        }
        
        public void stopGameLoop() { if (gameLoop != null) gameLoop.stop(); }

        private void handleMouseClick(Point clickPos) {
            int gridStartX = SIDE_PANEL_WIDTH;
            int gridEndX = gridStartX + GRID_WIDTH * CELL_SIZE;

            if (currentState == GameState.BUILDING) {
                if (clickPos.x >= gridStartX && clickPos.x < gridEndX) {
                    int gridX = (clickPos.x - gridStartX) / CELL_SIZE;
                    int gridY = clickPos.y / CELL_SIZE;
                    tryPlacePart(gridX, gridY);
                } else if (clickPos.x < gridStartX) { 
                    selectPartFromShop(clickPos);
                }
                if (launchButton.contains(clickPos) && !placedParts.isEmpty()) { prepareForLaunch(); }
                if (autoDetachCheckbox.contains(clickPos)) { autoDetachEnabled = !autoDetachEnabled; }
            } else if (currentState == GameState.READY_FOR_LAUNCH || currentState == GameState.COUNTDOWN) {
                if(startButton.contains(clickPos) && currentState == GameState.READY_FOR_LAUNCH) { currentState = GameState.COUNTDOWN; }
                if(backToHangarButton.contains(clickPos)) { resetToBuilding(); }
            } else if (currentState == GameState.LAUNCHING) {
                if (detachButton.contains(clickPos) && getActiveStageCount() > 1) { detachStage(); }
                if (selfDestructButton.contains(clickPos) && (rocketVelY > 0 || isOutOfFuel)) { explode(); }
            } else if (currentState == GameState.EXPLODED) {
                if (backToHangarButton.contains(clickPos)) { resetToBuilding(); }
            }
            repaint();
        }

        private void prepareForLaunch() {
            currentState = GameState.READY_FOR_LAUNCH;
            rocketVelY = 0; altitude = 0; maxAltitude = 0; gForce = 0;
            dynamicPressure = 0; maxDynamicPressure = 0;
            missionTime = -3.0; isOutOfFuel = false; timeScale = 1.0; timeSinceOutOfFuel = -1.0;
            detachedStages.clear();
            particles.removeIf(p -> p instanceof ExplosionParticle || p instanceof ExhaustParticle || p instanceof DebrisParticle);
            
            int maxGridY = placedParts.stream().mapToInt(p -> p.gridY).max().orElse(GRID_HEIGHT - 1);
            double yOffset = (GRID_HEIGHT - 1 - maxGridY) * CELL_SIZE;
            int gridStartX = SIDE_PANEL_WIDTH;
            for(RocketPart p : placedParts) {
                p.worldY = p.gridY * CELL_SIZE + yOffset;
                p.worldX = gridStartX + p.gridX * CELL_SIZE;
            }
            currentTotalMass = 0;
            for(RocketPart p : placedParts) { currentTotalMass += p.type.mass + p.currentFuel; }
            lastUpdateTime = System.nanoTime();
            gameLoop = new Timer(16, e -> { updateGame(); repaint(); });
            gameLoop.start();
        }
        
        private void handleRightClick(Point clickPos) { 
            int gridStartX = SIDE_PANEL_WIDTH;
            if (currentState != GameState.BUILDING) return;
            if (clickPos.x >= gridStartX && clickPos.x < gridStartX + GRID_WIDTH * CELL_SIZE) {
                int gridX = (clickPos.x - gridStartX) / CELL_SIZE;
                int gridY = clickPos.y / CELL_SIZE;
                if (gridX < GRID_WIDTH && gridY < GRID_HEIGHT) {
                    RocketPart partToRemove = grid[gridX][gridY];
                    if (partToRemove != null) {
                        int mirroredX = (GRID_WIDTH - 1) - gridX;
                        boolean isCenter = (gridX == mirroredX);

                        if (isCenter) {
                            playerMoney += partToRemove.type.cost;
                            grid[gridX][gridY] = null;
                            placedParts.remove(partToRemove);
                        } else {
                            RocketPart mirroredPart = grid[mirroredX][gridY];

                            playerMoney += partToRemove.type.cost;
                            grid[gridX][gridY] = null;
                            placedParts.remove(partToRemove);

                            if (mirroredPart != null) {
                                playerMoney += mirroredPart.type.cost;
                                grid[mirroredX][gridY] = null;
                                placedParts.remove(mirroredPart);
                            }
                        }
                        recalculateStages(); 
                        repaint();
                    }
                }
            }
        }
        
        private void resetToBuilding() {
            stopGameLoop(); 
            currentState = GameState.BUILDING; 
            detachedStages.clear();
            particlesInitialized = false;
            
            currentTotalMass = 0;
            for (RocketPart part : placedParts) {
                part.currentFuel = part.type.fuelCapacity; 
                part.worldX = SIDE_PANEL_WIDTH + part.gridX * CELL_SIZE;
                part.worldY = part.gridY * CELL_SIZE;
                part.isDetached = false;
            }
            recalculateStages();
        }

        private void selectPartFromShop(Point clickPos) {
            int shopItemHeight = 160; int yOffset = 150;
            for(PartType type : PartType.values()) {
                Rectangle itemBounds = new Rectangle(10, (int)(yOffset - shopScrollY), SIDE_PANEL_WIDTH - 20, shopItemHeight + 20);
                if (itemBounds.contains(clickPos)) { selectedPartType = type; break; }
                yOffset += shopItemHeight + 40;
            }
        }

        private void tryPlacePart(int gridX, int gridY) {
            if (selectedPartType == null || grid[gridX][gridY] != null) return;
        
            int mirroredX = (GRID_WIDTH - 1) - gridX;
            boolean isCenter = (gridX == mirroredX);
        
            if (isCenter) {
                if (playerMoney >= selectedPartType.cost && isPlacementLegal(selectedPartType, gridX, gridY)) {
                    playerMoney -= selectedPartType.cost;
                    RocketPart newPart = new RocketPart(selectedPartType, gridX, gridY);
                    grid[gridX][gridY] = newPart;
                    placedParts.add(newPart);
                    recalculateStages();
                }
            } else {
                if (playerMoney < selectedPartType.cost * 2 || grid[mirroredX][gridY] != null) return;
        
                if (isPlacementLegal(selectedPartType, gridX, gridY) && isPlacementLegal(selectedPartType, mirroredX, gridY)) {
                    playerMoney -= selectedPartType.cost * 2;
        
                    RocketPart part1 = new RocketPart(selectedPartType, gridX, gridY);
                    grid[gridX][gridY] = part1;
                    placedParts.add(part1);
        
                    RocketPart part2 = new RocketPart(selectedPartType, mirroredX, gridY);
                    grid[mirroredX][gridY] = part2;
                    placedParts.add(part2);
        
                    recalculateStages();
                }
            }
        }
        
        private boolean isPlacementLegal(PartType partType, int x, int y) {
            if (placedParts.isEmpty()) {
                // The first part must be a cockpit and placed in the center column
                return partType == PartType.COCKPIT && x == GRID_WIDTH / 2;
            }
            
            boolean isAdjacent = (y > 0 && grid[x][y - 1] != null) || (y < GRID_HEIGHT - 1 && grid[x][y + 1] != null) ||
                                 (x > 0 && grid[x - 1][y] != null) || (x < GRID_WIDTH - 1 && grid[x + 1][y] != null);
            if (!isAdjacent) return false;

            if (partType.thrust > 0) {
                if (y > 0) {
                    for (int i = 0; i < GRID_WIDTH; i++) {
                        if (grid[i][y - 1] != null && grid[i][y - 1].type.thrust > 0) return false;
                    }
                }
                if (y < GRID_HEIGHT - 1) {
                    for (int i = 0; i < GRID_WIDTH; i++) {
                        if (grid[i][y + 1] != null && grid[i][y + 1].type.thrust > 0) return false;
                    }
                }
                for (int i = 0; i < GRID_WIDTH; i++) {
                    if (grid[i][y] != null && grid[i][y].type == PartType.FUEL_TANK) return false;
                }
                
                RocketPart partAbove = (y > 0) ? grid[x][y - 1] : null;
                return partAbove != null && partAbove.type == PartType.FUEL_TANK;
            }

            if (partType == PartType.FUEL_TANK) {
                for (int i = 0; i < GRID_WIDTH; i++) {
                    if (grid[i][y] != null && grid[i][y].type.thrust > 0) return false;
                }
            }

            return true;
        }

        private void recalculateStages() {
            stages.clear();
            List<RocketPart> allEngines = placedParts.stream().filter(p -> p.type.thrust > 0).collect(Collectors.toList());
            List<RocketPart> unassignedParts = new ArrayList<>(placedParts);
            allEngines.stream()
                .collect(Collectors.groupingBy(e -> e.gridY))
                .values().stream()
                .sorted(Comparator.comparingInt(list -> list.get(0).gridY))
                .forEach(engineGroup -> {
                    Stage newStage = new Stage(engineGroup);
                    List<RocketPart> partsToSearch = new ArrayList<>(engineGroup);
                    while (!partsToSearch.isEmpty()) {
                        RocketPart current = partsToSearch.remove(0);
                        if (unassignedParts.contains(current) && !newStage.parts.contains(current)) {
                            newStage.parts.add(current); unassignedParts.remove(current);
                            int x = current.gridX, y = current.gridY;
                            if (y > 0 && grid[x][y-1] != null && grid[x][y-1].type.thrust == 0) partsToSearch.add(grid[x][y-1]);
                            if (x > 0 && grid[x-1][y] != null && grid[x-1][y].type.thrust == 0) partsToSearch.add(grid[x-1][y]);
                            if (x < GRID_WIDTH -1 && grid[x+1][y] != null && grid[x+1][y].type.thrust == 0) partsToSearch.add(grid[x+1][y]);
                        }
                    }
                    stages.add(newStage);
                });
            if (!unassignedParts.isEmpty()) {
                Stage finalStage = new Stage(); 
                finalStage.parts.addAll(unassignedParts);
                stages.add(0, finalStage);
            }
        }
        
        private void startLaunch() {
            currentState = GameState.LAUNCHING; rocketVelY = 0; altitude = 0; maxAltitude = 0; gForce = 0;
        }

        private void explode() {
            currentState = GameState.EXPLODED;
            for (RocketPart p : placedParts) {
                if (!p.isDetached) {
                    double partCenterX = p.worldX + (CELL_SIZE / 2.0);
                    double partCenterY = p.worldY + (CELL_SIZE / 2.0);
                    for (int i = 0; i < 25; i++) {
                        particles.add(new ExplosionParticle(partCenterX, partCenterY));
                    }
                }
            }
        }
        
        private double getAirDensityAt(double alt) {
            if (alt < 0) return SEA_LEVEL_AIR_DENSITY;
            return SEA_LEVEL_AIR_DENSITY * Math.exp(-alt / SCALE_HEIGHT);
        }
        private double getGravityAt(double alt) {
            return SEA_LEVEL_GRAVITY * Math.pow(EARTH_RADIUS / (EARTH_RADIUS + alt), 2);
        }
        
        private void updateGame() {
            long now = System.nanoTime();
            double actualDeltaTime = (now - lastUpdateTime) / 1_000_000_000.0;
            lastUpdateTime = now;

            double deltaTime = actualDeltaTime * timeScale;

            particles.forEach(p -> {p.update(deltaTime); if(p instanceof Cloud){ Cloud c = (Cloud)p; if (c.x > getWidth() + 200) c.x = -200; if (c.x < -200) c.x = getWidth() + 200; }});
            particles.removeIf(Particle::isDead);
            
             if (currentState == GameState.LAUNCHING && rocketVelY < 0 && altitude < 2000) {
                if (Math.random() > 0.7) { 
                    double rocketX = 0; int activeParts = 0;
                    for (RocketPart p : placedParts) {
                        if (!p.isDetached) { rocketX += p.worldX; activeParts++; }
                    }
                    if (activeParts > 0) {
                        rocketX /= activeParts;
                        particles.add(new DebrisParticle(rocketX, cameraWorldY, getHeight()));
                    }
                }
            }

            for(DetachedStage ds : detachedStages) {
                double stageMass = ds.parts.stream().mapToDouble(p -> p.type.mass).sum();
                if(stageMass <= 0) continue;
                double stageGravity = stageMass * getGravityAt(ds.altitude);
                double stageDrag = -DRAG_CONSTANT * getAirDensityAt(ds.altitude) * ds.velY * Math.abs(ds.velY);
                double netForce = stageGravity + stageDrag;
                double acceleration = netForce / stageMass;
                ds.velY += acceleration * deltaTime;
                ds.altitude += -ds.velY * deltaTime;
                for (RocketPart p : ds.parts) { p.worldY += ds.velY * deltaTime * PIXELS_PER_METER; }
            }

            if(currentState == GameState.COUNTDOWN){
                missionTime += deltaTime;
                if(missionTime >= 0){
                    startLaunch();
                }
                return;
            }
            if (currentState != GameState.LAUNCHING) return;
            
            missionTime += deltaTime;
            currentTotalMass = 0; for(RocketPart p : placedParts) { if(!p.isDetached) currentTotalMass += p.type.mass + p.currentFuel; }
            if(currentTotalMass <= 0) { isOutOfFuel = true; return; }

            double totalThrust = 0;
            Stage activeStage = null;
            for(int i = stages.size() - 1; i >= 0; i--) {
                Stage s = stages.get(i);
                if (s.hasActiveEngine()) {
                    if (s.getTotalCurrentFuel() > 0) {
                        activeStage = s; break;
                    } else if (autoDetachEnabled && getActiveStageCount() > 1) {
                        detachStage(s);
                    }
                }
            }

            double currentGravity = getGravityAt(altitude);
            if(activeStage != null) {
                isOutOfFuel = false;
                timeSinceOutOfFuel = -1.0;
                timeScale = 1.0;
                totalThrust = activeStage.stageEngines.stream().mapToDouble(e -> e.type.thrust).sum();
                int isp = activeStage.stageEngines.get(0).type.specificImpulse;
                double fuelConsumption = (totalThrust / (isp * SEA_LEVEL_GRAVITY)) * deltaTime;
                activeStage.consumeFuel(fuelConsumption);
                for(RocketPart engine : activeStage.stageEngines) {
                    for(int i=0; i<10; i++) {
                        particles.add(new ExhaustParticle(engine.worldX + CELL_SIZE/2.0, engine.worldY + CELL_SIZE, rocketVelY));
                    }
                }
            } else {
                if (!isOutOfFuel) {
                    isOutOfFuel = true;
                    timeSinceOutOfFuel = 0.0;
                }
                if(timeSinceOutOfFuel >= 0) {
                    timeSinceOutOfFuel += actualDeltaTime; // Use actual delta time for the timer
                }

                if (timeSinceOutOfFuel > 1.0) {
                    if (altitude < 1000) {
                        timeScale = 1.0; // Safety override near ground
                    } else if (Math.abs(rocketVelY) < 100 && altitude > 80000) {
                        timeScale = 100.0; // Apogee in space
                    } else if (altitude > 100000) {
                        timeScale = 50.0; // High space flight
                    } else if (altitude > 50000) {
                        timeScale = 25.0; // Low space flight
                    } else if (altitude > 20000) {
                        timeScale = 10.0; // Upper atmosphere
                    } else if (altitude > 5000) {
                        timeScale = 5.0; // Mid atmosphere
                    } else {
                        timeScale = 2.0; // Low atmosphere
                    }
                } else {
                    timeScale = 1.0;
                }
            }
            
            double forceThrust = -totalThrust;
            double forceGravity = currentTotalMass * currentGravity;
            double airDensity = getAirDensityAt(altitude);
            double forceDrag = -DRAG_CONSTANT * airDensity * rocketVelY * Math.abs(rocketVelY);
            
            double netForce = forceThrust + forceGravity + forceDrag;
            double acceleration = netForce / currentTotalMass;
            rocketVelY += acceleration * deltaTime;
            gForce = Math.abs(acceleration / SEA_LEVEL_GRAVITY);
            altitude += -rocketVelY * deltaTime;
            
            dynamicPressure = 0.5 * airDensity * rocketVelY * rocketVelY;
            if (dynamicPressure > maxDynamicPressure) {
                maxDynamicPressure = dynamicPressure;
            }
            
            for (RocketPart part : placedParts) {
                if (!part.isDetached) { part.worldY += rocketVelY * deltaTime * PIXELS_PER_METER; }
            }
            
            if (placedParts.stream().allMatch(p -> p.isDetached)) { 
                if (gameLoop.isRunning()) stopGameLoop(); 
            } else {
                maxAltitude = Math.max(altitude, maxAltitude);
                if(altitude < -1.0) { explode(); }
            }
        }
        
        private void detachStage() { 
            for (int i = stages.size() - 1; i >= 0; i--) { Stage stage = stages.get(i); if (stage.hasActiveEngine()) { detachStage(stage); break; } }
        }
        private void detachStage(Stage stageToDetach) { 
            detachedStages.add(new DetachedStage(stageToDetach.parts, rocketVelY, altitude));
            for (RocketPart part : stageToDetach.parts) { part.isDetached = true; }
        }
        private long getActiveStageCount() { 
            return stages.stream().filter(Stage::hasActiveEngine).count();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if (!particlesInitialized && getWidth() > 0 && getHeight() > 0) {
                resetBackgroundParticles(getWidth(), getHeight());
                particlesInitialized = true;
            }

            updateUIRectangles();
            if (currentState == GameState.BUILDING) { drawBuildMode(g2d); }
            else { drawLaunchMode(g2d); }
        }

        private void updateUIRectangles() {
            int w = getWidth(); int h = getHeight();
            int uiStartX = SIDE_PANEL_WIDTH + GRID_WIDTH * CELL_SIZE;

            launchButton.setBounds(uiStartX + 30, h - 80, 290, 60);
            autoDetachCheckbox.setBounds(uiStartX + 30, h - 125, 290, 35);
            
            startButton.setBounds(uiStartX + 30, h - 80, 290, 60);
            backToHangarButton.setBounds(uiStartX + 30, h - 150, 290, 60);
            detachButton.setBounds(15, h - 80, 220, 50);
            selfDestructButton.setBounds(15, h - 140, 220, 50);
        }

        private void drawBuildMode(Graphics2D g2d) {
            drawGrid(g2d); drawPlacedParts(g2d); drawShop(g2d); drawGhostPart(g2d); drawBuildUI(g2d);
        }

        private void drawLaunchMode(Graphics2D g2d) {
            double focusY = 0; int activePartCount = 0;
            for(RocketPart p : placedParts) { if (!p.isDetached) { focusY += p.worldY; activePartCount++; } }
            if (activePartCount > 0) {
                focusY /= activePartCount;
            } else if (!particles.isEmpty()) {
                double particleCenterY = 0;
                for (Particle p : particles) { if(p instanceof ExplosionParticle) particleCenterY += p.y; }
                if(particleCenterY > 0) focusY = particleCenterY / particles.stream().filter(p -> p instanceof ExplosionParticle).count();
            }
            cameraWorldY = focusY - (getHeight() / 2.0);
            
            AffineTransform originalTransform = g2d.getTransform();
            
            // Layer 1: Sky and Parallax Background (Screen Space)
            drawSky(g2d);
            drawParallaxParticles(g2d);
            
            // Layer 2: World Objects (World Space)
            g2d.translate(0, -cameraWorldY); 
            drawGround(g2d);
            drawWorldParticles(g2d);
            drawDetachedStages(g2d);
            if (currentState != GameState.EXPLODED) {
                drawPlacedParts(g2d);
            }
            
            // Layer 3: UI (Screen Space)
            g2d.setTransform(originalTransform); 
            drawLaunchUI(g2d);
        }
        
        private void drawWorldParticles(Graphics2D g2d) {
            for(Particle p : particles) {
                if (!(p instanceof Star || p instanceof Cloud || p instanceof Bird || p instanceof Satellite)) {
                    p.draw(g2d, cameraWorldY, altitude);
                }
            }
        }
        
        private void drawParallaxParticles(Graphics2D g2d) {
            for (Particle p : particles) {
                if (p instanceof Star || p instanceof Cloud || p instanceof Bird || p instanceof Satellite) {
                    p.draw(g2d, cameraWorldY, altitude);
                }
            }
        }

        private void drawDetachedStages(Graphics2D g2d) {
            for(DetachedStage ds : detachedStages) {
                for(RocketPart p : ds.parts) {
                    drawPart(g2d, p.type, (int)p.worldX, (int)p.worldY, 1.0f);
                }
            }
        }

        private void drawSky(Graphics2D g2d) {
            float skyHue = (float)Math.max(0, 200 - altitude/80000) / 360f;
            float brightness = (float)Math.max(0.05, 0.9 - altitude / 120000.0);
            float saturation = (float)Math.max(0.1, 0.7 - altitude / 150000.0);
            Color skyTop = Color.getHSBColor(skyHue, saturation, brightness);
            Color skyBottom = Color.getHSBColor(skyHue, saturation * 0.8f, brightness * 0.6f);
            
            g2d.setPaint(new GradientPaint(0, 0, skyTop, 0, getHeight(), skyBottom));
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        private void drawGround(Graphics2D g2d) {
            g2d.setColor(new Color(34, 139, 34)); 
            g2d.fillRect(-getWidth(), GRID_HEIGHT * CELL_SIZE, getWidth()*3, getHeight()*10);
        }

        private void drawGrid(Graphics2D g2d) { 
            int gridStartX = SIDE_PANEL_WIDTH;
            g2d.setColor(Color.GRAY);
            for (int x = 0; x < GRID_WIDTH; x++) { for (int y = 0; y < GRID_HEIGHT; y++) { g2d.drawRect(gridStartX + x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE); } }
        }

        private void drawPlacedParts(Graphics2D g2d) {
            int gridStartX = (currentState == GameState.BUILDING) ? SIDE_PANEL_WIDTH : 0;
            for (RocketPart part : placedParts) { 
                if ((currentState == GameState.LAUNCHING || currentState == GameState.EXPLODED) && part.isDetached) continue;
                
                double drawX = (currentState == GameState.BUILDING) ? gridStartX + part.gridX * CELL_SIZE : part.worldX;
                double drawY = (currentState == GameState.BUILDING) ? part.gridY * CELL_SIZE : part.worldY;
                
                drawPart(g2d, part.type, (int)drawX, (int)drawY, 1.0f); 
            }
        }

        private void drawShop(Graphics2D g2d) {
            int shopX = 0;
            g2d.setColor(Color.BLACK); g2d.fillRect(shopX, 0, SIDE_PANEL_WIDTH, getHeight());
            
            Shape oldClip = g2d.getClip();
            g2d.clipRect(shopX, 150, SIDE_PANEL_WIDTH, getHeight() - 150);
            
            int yOffset = 150;
            int itemHeight = 160;

            for (PartType type : PartType.values()) {
                int itemY = (int)(yOffset - shopScrollY);

                if (type == selectedPartType) { g2d.setColor(Color.YELLOW); g2d.drawRect(shopX + 10, itemY, SIDE_PANEL_WIDTH - 20, itemHeight + 20); }
                
                drawPart(g2d, type, shopX + 15, itemY + 45, 0.7f);

                g2d.setColor(Color.WHITE);
                int textX = shopX + 120;
                int textY = itemY + 35;
                
                g2d.setFont(new Font("SansSerif", Font.BOLD, 22));
                g2d.drawString(type.name + " ($" + type.cost + ")", textX, textY);
                
                g2d.setFont(new Font("SansSerif", Font.PLAIN, 18));
                textY += 30;
                g2d.drawString(String.format("Mass: %.0f kg", type.mass), textX, textY);
                
                if (type.fuelCapacity > 0) {
                    textY += 25;
                    g2d.drawString(String.format("Fuel: %.0f kg", type.fuelCapacity), textX, textY);
                }
                
                if (type.thrust > 0) {
                    textY += 25;
                    g2d.drawString(String.format("Thrust: %.1f kN", type.thrust / 1000), textX, textY);
                    textY += 25;
                    g2d.drawString(String.format("Isp: %d s", type.specificImpulse), textX, textY);
                }
                
                yOffset += itemHeight + 40;
            }
            g2d.setClip(oldClip);

            g2d.setColor(Color.BLACK);
            g2d.fillRect(shopX, 0, SIDE_PANEL_WIDTH, 150);
            
            g2d.setColor(Color.WHITE); g2d.setFont(new Font("SansSerif", Font.BOLD, 30)); g2d.drawString("Shop", shopX + 120, 50);
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 24)); g2d.drawString("Money: $" + playerMoney, shopX + 20, 90);
            
            double totalDryMass = 0;
            for(RocketPart part : placedParts) totalDryMass += part.type.mass;
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 20));
            g2d.drawString(String.format("Dry Mass: %.0f kg", totalDryMass), shopX + 20, 120);
        }

        private void drawBuildUI(Graphics2D g2d) {
            int uiX = SIDE_PANEL_WIDTH + GRID_WIDTH * CELL_SIZE;
            g2d.setColor(Color.BLACK);
            g2d.fillRect(uiX, 0, SIDE_PANEL_WIDTH, getHeight());

            g2d.setColor(placedParts.isEmpty() ? Color.GRAY : Color.GREEN); g2d.fill(launchButton);
            g2d.setColor(Color.BLACK); g2d.setFont(new Font("SansSerif", Font.BOLD, 30)); g2d.drawString("To Launchpad", launchButton.x + 35, launchButton.y + 45);
            g2d.setColor(Color.WHITE); g2d.draw(autoDetachCheckbox);
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 22)); g2d.drawString("Auto-Detach", autoDetachCheckbox.x + 50, autoDetachCheckbox.y + 26);
            if(autoDetachEnabled) {
                g2d.setColor(Color.GREEN); g2d.setStroke(new BasicStroke(4));
                int checkX = autoDetachCheckbox.x + 15; int checkY = autoDetachCheckbox.y + 18;
                g2d.drawLine(checkX, checkY, checkX + 8, checkY + 8); g2d.drawLine(checkX + 8, checkY + 8, checkX + 20, checkY - 8);
                g2d.setStroke(new BasicStroke(1));
            }
        }
        
        private void drawLaunchUI(Graphics2D g2d) {
            int uiStartX = SIDE_PANEL_WIDTH + GRID_WIDTH * CELL_SIZE;

            // --- Left Panel ---
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
            g2d.drawString("FLIGHT DATA", 15, 30);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
            g2d.drawString(String.format("Velocity: %.1f m/s", -rocketVelY), 15, 60);
            g2d.drawString(String.format("G-Force: %.1f G", gForce), 15, 90);
            g2d.drawString(String.format("Mass: %.0f kg", currentTotalMass), 15, 120);
            
            // --- Right Panel ---
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
            g2d.drawString("TELEMETRY", uiStartX + 15, 30);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
            g2d.drawString(String.format("Altitude: %.0f m", altitude), uiStartX + 15, 60);
            g2d.drawString(String.format("Max Alt: %.0f m", maxAltitude), uiStartX + 15, 90);
            g2d.drawString(String.format("Atm. Press: %.1f %%", getAirDensityAt(altitude)/SEA_LEVEL_AIR_DENSITY * 100), uiStartX + 15, 120);
            g2d.drawString(String.format("Q: %.1f kPa", dynamicPressure/1000), uiStartX + 15, 150);
            g2d.drawString(String.format("Max Q: %.1f kPa", maxDynamicPressure/1000), uiStartX + 15, 180);

            if (timeScale > 1.0) {
                g2d.setColor(Color.CYAN);
                g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
                g2d.drawString(String.format("Time Warp: %.0fx", timeScale), getWidth() / 2 - 80, 100);
            }

            String timeString;
            if (currentState == GameState.READY_FOR_LAUNCH || (currentState == GameState.COUNTDOWN && missionTime < 0)) {
                timeString = String.format("T- %.1f", -missionTime);
            } else {
                timeString = String.format("T+ %.1f", missionTime);
            }
            g2d.setColor(missionTime < 0 ? Color.RED : Color.YELLOW);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 48));
            g2d.drawString(timeString, getWidth() / 2 - 100, 60);
            
            int uiX = uiStartX + 15; int uiY = 220; int stageNum = 1;
             g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
            g2d.drawString("STAGES", uiX, uiY - 5);
            uiY += 30;

            for (int i = stages.size() - 1; i >= 0; i--) {
                Stage stage = stages.get(i); if (!stage.hasActiveEngine()) continue;
                double capacity = stage.parts.stream().mapToDouble(p -> p.type.fuelCapacity).sum();
                if (capacity > 0) {
                    double fuelRatio = stage.getTotalCurrentFuel() / capacity;
                    g2d.setColor(Color.GRAY); g2d.drawRect(uiX, uiY, 290, 30);
                    g2d.setColor(Color.GREEN); g2d.fillRect(uiX + 2, uiY + 2, (int)(286 * fuelRatio), 26);
                    g2d.setColor(Color.WHITE); g2d.setFont(new Font("SansSerif", Font.BOLD, 22)); g2d.drawString("Stage " + stageNum + " Fuel", uiX, uiY - 5); uiY += 50;
                }
                stageNum++;
            }
            if(currentState == GameState.READY_FOR_LAUNCH || currentState == GameState.COUNTDOWN) {
                if(currentState == GameState.READY_FOR_LAUNCH) {
                    g2d.setColor(Color.RED); g2d.fill(startButton);
                    g2d.setColor(Color.WHITE); g2d.setFont(new Font("SansSerif", Font.BOLD, 32)); g2d.drawString("START", startButton.x + 90, startButton.y + 45);
                }
                g2d.setColor(Color.LIGHT_GRAY); g2d.fill(backToHangarButton);
                g2d.setColor(Color.BLACK); g2d.setFont(new Font("SansSerif", Font.BOLD, 24)); g2d.drawString("Back to Hangar", backToHangarButton.x + 50, backToHangarButton.y + 40);
            } else if (currentState == GameState.LAUNCHING) {
                if(getActiveStageCount() > 1) {
                    g2d.setColor(Color.ORANGE); g2d.fill(detachButton);
                    g2d.setColor(Color.BLACK); g2d.setFont(new Font("SansSerif", Font.BOLD, 24)); g2d.drawString("Detach Stage", detachButton.x + 35, detachButton.y + 35);
                }
                if(rocketVelY > 0 || isOutOfFuel) {
                    g2d.setColor(Color.RED); g2d.fill(selfDestructButton);
                    g2d.setColor(Color.WHITE); g2d.setFont(new Font("SansSerif", Font.BOLD, 24)); g2d.drawString("SELF-DESTRUCT", selfDestructButton.x + 15, selfDestructButton.y + 35);
                }
            } else if (currentState == GameState.EXPLODED) {
                g2d.setColor(Color.RED); g2d.setFont(new Font("SansSerif", Font.BOLD, 60));
                g2d.drawString("ROCKET DESTROYED", getWidth()/2 - 350, getHeight()/2);
                g2d.setColor(Color.LIGHT_GRAY); g2d.fill(backToHangarButton);
                g2d.setColor(Color.BLACK); g2d.setFont(new Font("SansSerif", Font.BOLD, 24)); g2d.drawString("Back to Hangar", backToHangarButton.x + 50, backToHangarButton.y + 40);
            }
        }

        private void drawGhostPart(Graphics2D g2d) {
            int gridStartX = SIDE_PANEL_WIDTH;
            if (selectedPartType != null && mousePos.x >= gridStartX && mousePos.x < gridStartX + GRID_WIDTH * CELL_SIZE) {
                int gridX = (mousePos.x - gridStartX) / CELL_SIZE;
                int gridY = mousePos.y / CELL_SIZE; 
                if(gridX >= GRID_WIDTH || gridY >= GRID_HEIGHT) return;
                
                int mirroredX = (GRID_WIDTH - 1) - gridX;
                boolean isCenter = (gridX == mirroredX);

                // Draw original ghost part
                drawSingleGhost(g2d, gridX, gridY);

                // Draw mirrored ghost part if not in center
                if(!isCenter){
                     drawSingleGhost(g2d, mirroredX, gridY);
                }
            }
        }

        private void drawSingleGhost(Graphics2D g2d, int gridX, int gridY) {
            int gridStartX = SIDE_PANEL_WIDTH;
            int x = gridStartX + gridX * CELL_SIZE; 
            int y = gridY * CELL_SIZE;
            Composite old = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            drawPart(g2d, selectedPartType, x, y, 1.0f);
            g2d.setComposite(old);
            if (!isPlacementLegal(selectedPartType, gridX, gridY) || grid[gridX][gridY] != null) {
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(4));
                g2d.drawLine(x, y, x + CELL_SIZE, y + CELL_SIZE);
                g2d.drawLine(x + CELL_SIZE, y, x, y + CELL_SIZE);
                g2d.setStroke(new BasicStroke(1));
            }
        }
        
        private void drawPart(Graphics2D g2d, PartType type, int x, int y, float scale) {
            int size = (int)(CELL_SIZE * scale); int inset = (int)(CELL_SIZE * (1-scale) / 2); x += inset; y += inset;
            g2d.setColor(type.color);
            switch(type) {
                case COCKPIT: g2d.fillPolygon(new int[]{x, x + size, x + size/2}, new int[]{y + size, y + size, y}, 3); break;
                case FUEL_TANK: g2d.fillRect(x, y, size, size); break;
                case A4_ENGINE: case ENGINE_T1:
                    g2d.fillRect(x, y, size, size/2);
                    g2d.fillPolygon(new int[]{x, x + size, x + size - 20, x + 20}, new int[]{y + size/2, y + size/2, y + size, y + size}, 4);
                    break;
            }
        }
    }
}

