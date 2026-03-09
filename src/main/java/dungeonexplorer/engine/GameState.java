package dungeonexplorer.engine;

import dungeonexplorer.entities.Enemy;
import dungeonexplorer.entities.Player;
import dungeonexplorer.map.GameMap;
import dungeonexplorer.util.Constants;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages all game state for the side-scrolling platformer:
 * player, enemies, map, camera, level, difficulty, and game phase.
 */
public class GameState {
    public enum State {
        TITLE, DIFFICULTY_SELECT, GET_READY, PLAYING, PAUSED, DYING, GAME_OVER, LEVEL_COMPLETE
    }

    private State state;
    private int level;
    private Difficulty difficulty;
    private int selectedDifficultyIndex;
    private Player player;
    private List<Enemy> enemies;
    private GameMap map;
    private int readyTimer;
    private int dyingTimer;
    private boolean difficultyChanged;

    // Camera position (top-left corner of viewport in world coords)
    private double cameraX, cameraY;

    public GameState() {
        this.state = State.TITLE;
        this.level = 1;
        this.difficulty = Difficulty.MEDIUM;
        this.selectedDifficultyIndex = 1;
        this.enemies = new ArrayList<>();
        this.difficultyChanged = false;
        this.cameraX = 0;
        this.cameraY = 0;
    }

    private void initLevel() {
        map = new GameMap(difficulty.mapRows, difficulty.mapCols);
        map.loadLevel(level, difficulty.enemyCount, (int) difficulty.diamondCount);

        // Create player at spawn (pixel coords)
        double spawnX = map.getPlayerSpawnCol() * Constants.TILE_SIZE;
        double spawnY = map.getPlayerSpawnRow() * Constants.TILE_SIZE;

        if (player == null) {
            player = new Player(spawnX, spawnY);
        } else {
            player.respawn(spawnX, spawnY);
        }

        // Create enemies at their spawn positions
        enemies.clear();
        List<int[]> spawns = map.getEnemySpawns();
        for (int i = 0; i < spawns.size(); i++) {
            int[] sp = spawns.get(i);
            double ex = sp[1] * Constants.TILE_SIZE;
            double ey = sp[0] * Constants.TILE_SIZE;
            enemies.add(new Enemy(ex, ey, i, difficulty.enemySpeed));
        }

        // Initialize camera
        updateCamera();
    }

    public void update() {
        switch (state) {
            case GET_READY:
                readyTimer--;
                if (readyTimer <= 0) {
                    state = State.PLAYING;
                }
                break;

            case PLAYING:
                updatePlaying();
                break;

            case DYING:
                dyingTimer--;
                if (dyingTimer <= 0) {
                    if (player.getLives() <= 0) {
                        state = State.GAME_OVER;
                    } else {
                        // Respawn player
                        double spawnX = map.getPlayerSpawnCol() * Constants.TILE_SIZE;
                        double spawnY = map.getPlayerSpawnRow() * Constants.TILE_SIZE;
                        player.respawn(spawnX, spawnY);
                        // Respawn all enemies
                        for (Enemy enemy : enemies) {
                            enemy.respawn();
                        }
                        state = State.GET_READY;
                        readyTimer = 90;
                    }
                }
                break;

            default:
                break;
        }
    }

    private void updatePlaying() {
        // Update player
        player.update(map);

        // Update enemies
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy enemy = it.next();
            enemy.update(map);

            // Remove dead enemies after death anim
            if (enemy.isDeathAnimComplete()) {
                it.remove();
            }
        }

        // Check player attack hitting enemies
        if (player.isAttacking()) {
            Rectangle attackBox = player.getAttackHitbox();
            if (attackBox != null) {
                for (Enemy enemy : enemies) {
                    if (enemy.isDead()) continue;
                    // Check overlap between attack hitbox and enemy bounds
                    Rectangle enemyBox = new Rectangle(
                        (int) enemy.getX(), (int) enemy.getY(),
                        enemy.getWidth(), enemy.getHeight()
                    );
                    if (attackBox.intersects(enemyBox)) {
                        boolean killed = enemy.takeHit(player.getFacing());
                        if (killed) {
                            player.addScore(Constants.ENEMY_KILL_SCORE);
                        }
                    }
                }
            }
        }

        // Check collectible pickup (gold bars and diamonds)
        checkCollectibles();

        // Check exit
        checkExit();

        // Check enemy contact damage
        checkEnemyContact();

        // Update camera to follow player
        updateCamera();
    }

    private void checkCollectibles() {
        int ts = Constants.TILE_SIZE;
        double px = player.getX();
        double py = player.getY();
        int pw = player.getWidth();
        int ph = player.getHeight();

        // Check all tiles the player overlaps
        int startCol = (int) (px / ts);
        int endCol = (int) ((px + pw - 1) / ts);
        int startRow = (int) (py / ts);
        int endRow = (int) ((py + ph - 1) / ts);

        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                int tile = map.getTile(r, c);
                if (tile == Constants.TILE_GOLD_BAR) {
                    map.setTile(r, c, Constants.TILE_EMPTY);
                    map.collectGold();
                    player.addScore(Constants.GOLD_BAR_SCORE);
                } else if (tile == Constants.TILE_DIAMOND) {
                    map.setTile(r, c, Constants.TILE_EMPTY);
                    map.collectGold();
                    player.addScore(Constants.DIAMOND_SCORE);
                }
            }
        }
    }

    private void checkExit() {
        if (!map.isExitOpen()) return;

        int ts = Constants.TILE_SIZE;
        // Check if player overlaps the exit tile
        int exitPixelX = map.getExitCol() * ts;
        int exitPixelY = map.getExitRow() * ts;

        if (player.getX() < exitPixelX + ts && player.getX() + player.getWidth() > exitPixelX &&
            player.getY() < exitPixelY + ts && player.getY() + player.getHeight() > exitPixelY) {
            state = State.LEVEL_COMPLETE;
            player.addScore(1000 + level * 500);
        }
    }

    private void checkEnemyContact() {
        if (player.isInvulnerable() || !player.isAlive()) return;

        for (Enemy enemy : enemies) {
            if (enemy.isDead()) continue;
            if (player.overlaps(enemy)) {
                player.takeDamage();
                if (!player.isAlive()) {
                    state = State.DYING;
                    dyingTimer = 70;
                }
                break;
            }
        }
    }

    private void updateCamera() {
        if (player == null || map == null) return;

        int vpW = Constants.VIEWPORT_WIDTH;
        int vpH = Constants.VIEWPORT_HEIGHT;

        // Target camera position: center on player
        double targetX = player.getCenterX() - vpW / 2.0;
        double targetY = player.getCenterY() - vpH / 2.0;

        // Smooth camera follow
        cameraX += (targetX - cameraX) * 0.12;
        cameraY += (targetY - cameraY) * 0.12;

        // Clamp to map bounds
        cameraX = Math.max(0, Math.min(cameraX, map.getPixelWidth() - vpW));
        cameraY = Math.max(0, Math.min(cameraY, map.getPixelHeight() - vpH));
    }

    /** Called from title screen - transitions to difficulty selection */
    public void startGame() {
        state = State.DIFFICULTY_SELECT;
        selectedDifficultyIndex = 1;
    }

    /** Called after selecting difficulty */
    public void selectDifficulty(int index) {
        Difficulty[] values = Difficulty.values();
        this.difficulty = values[Math.max(0, Math.min(index, values.length - 1))];
        this.level = 1;
        this.player = null;
        this.difficultyChanged = true;
        initLevel();
        state = State.GET_READY;
        readyTimer = 90;
    }

    public void restartGame() {
        state = State.DIFFICULTY_SELECT;
        selectedDifficultyIndex = difficulty.ordinal();
    }

    public void nextLevel() {
        level++;
        int score = player.getScore();
        int lives = player.getLives();
        initLevel();
        player.setScore(score);
        player.setLives(lives);
        state = State.GET_READY;
        readyTimer = 90;
    }

    public void moveDifficultySelection(int delta) {
        selectedDifficultyIndex += delta;
        if (selectedDifficultyIndex < 0) selectedDifficultyIndex = Difficulty.values().length - 1;
        if (selectedDifficultyIndex >= Difficulty.values().length) selectedDifficultyIndex = 0;
    }

    public boolean consumeDifficultyChanged() {
        if (difficultyChanged) {
            difficultyChanged = false;
            return true;
        }
        return false;
    }

    // Getters
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public int getLevel() { return level; }
    public Player getPlayer() { return player; }
    public List<Enemy> getEnemies() { return enemies; }
    public GameMap getMap() { return map; }
    public Difficulty getDifficulty() { return difficulty; }
    public int getSelectedDifficultyIndex() { return selectedDifficultyIndex; }
    public double getCameraX() { return cameraX; }
    public double getCameraY() { return cameraY; }
}
