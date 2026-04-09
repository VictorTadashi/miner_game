package dungeonexplorer.entities;

import dungeonexplorer.map.GameMap;
import dungeonexplorer.util.Constants;

import java.awt.*;

/**
 * Base class for all entities with physics (gravity, velocity, AABB collision).
 */
public abstract class Entity {
    protected double x, y;
    protected double velX, velY;
    protected int width, height;
    protected int facing;
    protected boolean onGround;
    protected boolean skipGravity = false;
    protected int animFrame;
    protected int animTimer;

    public Entity(double x, double y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.velX = 0;
        this.velY = 0;
        this.facing = Constants.FACING_RIGHT;
        this.onGround = false;
    }

    public void update(GameMap map) {
        // Gravity (suppressed when on a ladder)
        if (!skipGravity) {
            velY += Constants.GRAVITY;
            if (velY > Constants.MAX_FALL_SPEED) velY = Constants.MAX_FALL_SPEED;
        }

        // Horizontal movement
        x += velX;
        resolveHorizontal(map);

        // Vertical movement
        y += velY;
        resolveVertical(map);

        // Animation
        animTimer++;
        if (animTimer >= 8) {
            animTimer = 0;
            animFrame = (animFrame + 1) % 4;
        }
    }

    private void resolveHorizontal(GameMap map) {
        int ts = Constants.TILE_SIZE;
        int topRow = (int) (y / ts);
        int botRow = (int) ((y + height - 1) / ts);

        if (velX > 0) {
            int col = (int) ((x + width - 1) / ts);
            for (int r = topRow; r <= botRow; r++) {
                if (map.isSolid(r, col)) {
                    x = col * ts - width;
                    velX = 0;
                    break;
                }
            }
        } else if (velX < 0) {
            int col = (int) (x / ts);
            for (int r = topRow; r <= botRow; r++) {
                if (map.isSolid(r, col)) {
                    x = (col + 1) * ts;
                    velX = 0;
                    break;
                }
            }
        }

        if (x < 0) { x = 0; velX = 0; }
        double maxX = map.getCols() * ts - width;
        if (x > maxX) { x = maxX; velX = 0; }
    }

    private void resolveVertical(GameMap map) {
        int ts = Constants.TILE_SIZE;
        onGround = false;
        int leftCol = (int) (x / ts);
        int rightCol = (int) ((x + width - 1) / ts);

        if (velY > 0) {
            int row = (int) ((y + height - 1) / ts);
            for (int c = leftCol; c <= rightCol; c++) {
                if (map.isSolid(row, c)) {
                    y = row * ts - height;
                    velY = 0;
                    onGround = true;
                    break;
                }
            }
        } else if (velY < 0) {
            int row = (int) (y / ts);
            for (int c = leftCol; c <= rightCol; c++) {
                if (map.isSolid(row, c)) {
                    y = (row + 1) * ts;
                    velY = 0;
                    break;
                }
            }
        }

        if (y < 0) { y = 0; velY = 0; }
        double maxY = map.getRows() * ts - height;
        if (y > maxY) { y = maxY; velY = 0; onGround = true; }
    }

    public boolean overlaps(Entity other) {
        return x < other.x + other.width && x + width > other.x &&
               y < other.y + other.height && y + height > other.y;
    }

    public double getCenterX() { return x + width / 2.0; }
    public double getCenterY() { return y + height / 2.0; }

    public abstract void render(Graphics2D g, double camX, double camY);

    public double getX() { return x; }
    public double getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getFacing() { return facing; }
    public boolean isOnGround() { return onGround; }

    public void setPosition(double x, double y) {
        this.x = x; this.y = y; velX = 0; velY = 0;
    }
}
