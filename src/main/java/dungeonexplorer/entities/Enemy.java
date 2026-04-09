package dungeonexplorer.entities;

import dungeonexplorer.map.GameMap;
import dungeonexplorer.util.Constants;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * Cave monster enemy for side-scrolling platformer.
 * Rendered from a sprite image with procedural fallback.
 * Has HP, knockback, patrol AI, and distinct visual types.
 */
public class Enemy extends Entity {
    private final int enemyIndex;
    private final Color color;
    private final double patrolSpeed;
    private int hp;
    private int invulnTimer;
    private double knockbackVelX;
    private boolean dead;
    private int deathTimer;
    private int eyeAnimOffset;
    private final double spawnX, spawnY;

    // Patrol state
    private boolean patrolling;
    private double chaseRange;

    // (Jump AI removed — monsters only patrol side-to-side)

    // Sprite images (shared across all Enemy instances)
    private static BufferedImage spriteRight;
    private static BufferedImage spriteLeft;
    private static boolean spriteLoaded = false;

    // Sprite draw dimensions (much larger than player for intimidating look)
    private static final int SPRITE_W = 72;
    private static final int SPRITE_H = 72;

    public Enemy(double spawnX, double spawnY, int enemyIndex, double patrolSpeed) {
        super(spawnX, spawnY, Constants.ENEMY_WIDTH, Constants.ENEMY_HEIGHT);
        this.enemyIndex = enemyIndex;
        this.patrolSpeed = patrolSpeed;
        this.color = Constants.ENEMY_COLORS[enemyIndex % Constants.ENEMY_COLORS.length];
        this.hp = Constants.ENEMY_HP;
        this.invulnTimer = 0;
        this.knockbackVelX = 0;
        this.dead = false;
        this.deathTimer = 0;
        this.eyeAnimOffset = enemyIndex * 37;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.patrolling = true;
        this.chaseRange = 200 + enemyIndex * 30;
        this.facing = (enemyIndex % 2 == 0) ? Constants.FACING_RIGHT : Constants.FACING_LEFT;

        // Load sprite on first instantiation
        if (!spriteLoaded) {
            loadSprite();
        }
    }

    /**
     * Load the monster sprite image.
     * Creates right-facing and left-facing (flipped) versions.
     */
    private static void loadSprite() {
        spriteLoaded = true;
        try {
            BufferedImage raw = null;

            // Try classpath first
            InputStream stream = Enemy.class.getResourceAsStream("/tiles/monster_01.png");
            if (stream != null) {
                raw = ImageIO.read(stream);
                stream.close();
            } else {
                // Try file fallback
                File file = new File("tiles/monster_01.png");
                if (file.exists()) {
                    raw = ImageIO.read(file);
                }
            }

            if (raw != null) {
                // Scale to sprite draw size with nearest-neighbor (pixel art)
                spriteRight = new BufferedImage(SPRITE_W, SPRITE_H, BufferedImage.TYPE_INT_ARGB);
                Graphics2D sg = spriteRight.createGraphics();
                sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                sg.drawImage(raw, 0, 0, SPRITE_W, SPRITE_H, null);
                sg.dispose();

                // Create flipped (left-facing) version
                spriteLeft = new BufferedImage(SPRITE_W, SPRITE_H, BufferedImage.TYPE_INT_ARGB);
                sg = spriteLeft.createGraphics();
                sg.drawImage(spriteRight, SPRITE_W, 0, -SPRITE_W, SPRITE_H, null);
                sg.dispose();

                System.out.println("[Enemy] Monster sprite loaded.");
            } else {
                System.out.println("[Enemy] Monster sprite not found, using procedural rendering.");
            }
        } catch (Exception e) {
            System.out.println("[Enemy] Sprite load error: " + e.getMessage());
            spriteRight = null;
            spriteLeft = null;
        }
    }

    @Override
    public void update(GameMap map) {
        if (dead) {
            deathTimer++;
            return;
        }

        eyeAnimOffset++;

        // Invulnerability
        if (invulnTimer > 0) invulnTimer--;

        // Apply knockback
        if (Math.abs(knockbackVelX) > 0.3) {
            velX = knockbackVelX;
            knockbackVelX *= Constants.ENEMY_KNOCKBACK_DECAY;
        } else {
            knockbackVelX = 0;
            // Patrol behavior: walk in facing direction until hitting wall or edge
            velX = patrolSpeed * facing;
        }

        // Physics (gravity, collision)
        super.update(map);

        // Check if we need to turn around or jump
        checkTurnAroundOrJump(map);
    }

    /**
     * Patrol AI: walk side-to-side, turn around at walls and edges.
     * Monsters do NOT jump — they only walk.
     */
    private void checkTurnAroundOrJump(GameMap map) {
        if (Math.abs(knockbackVelX) > 0.5) return;

        int ts = Constants.TILE_SIZE;
        boolean wallAhead = false;
        boolean edgeAhead = false;

        if (facing == Constants.FACING_RIGHT) {
            int col = (int) ((x + width + 2) / ts);
            int row = (int) ((y + height / 2) / ts);
            if (map.isSolid(row, col)) {
                wallAhead = true;
            }
            if (onGround) {
                int groundCheckCol = (int) ((x + width + 4) / ts);
                int groundRow = (int) ((y + height + 2) / ts);
                if (!map.isSolid(groundRow, groundCheckCol)) {
                    edgeAhead = true;
                }
            }
        } else {
            int col = (int) ((x - 2) / ts);
            int row = (int) ((y + height / 2) / ts);
            if (map.isSolid(row, col)) {
                wallAhead = true;
            }
            if (onGround) {
                int groundCheckCol = (int) ((x - 4) / ts);
                int groundRow = (int) ((y + height + 2) / ts);
                if (!map.isSolid(groundRow, groundCheckCol)) {
                    edgeAhead = true;
                }
            }
        }

        // Turn around at walls or edges (no jumping)
        if (wallAhead || edgeAhead) {
            facing = -facing;
        }
    }

    /** Called when player attacks this enemy. Returns true if enemy dies. */
    public boolean takeHit(int fromFacing) {
        if (invulnTimer > 0 || dead) return false;

        hp--;
        invulnTimer = Constants.ENEMY_INVULN_FRAMES;

        // Knockback away from attack direction
        knockbackVelX = Constants.PLAYER_ATTACK_KNOCKBACK * fromFacing;
        velY = -3; // Small upward bump

        if (hp <= 0) {
            dead = true;
            deathTimer = 0;
            velY = -6;
            return true;
        }
        return false;
    }

    public void respawn() {
        setPosition(spawnX, spawnY);
        hp = Constants.ENEMY_HP;
        dead = false;
        deathTimer = 0;
        invulnTimer = 0;
        knockbackVelX = 0;
        facing = (enemyIndex % 2 == 0) ? Constants.FACING_RIGHT : Constants.FACING_LEFT;
    }

    public boolean isDeathAnimComplete() { return dead && deathTimer >= 40; }
    public boolean isDead() { return dead; }
    public int getHp() { return hp; }

    @Override
    public void render(Graphics2D g, double camX, double camY) {
        int sx = (int) (x - camX);
        int sy = (int) (y - camY);

        if (dead) {
            // Death poof effect
            float progress = Math.min(deathTimer / 40.0f, 1.0f);
            int alpha = (int) (200 * (1.0f - progress));
            if (alpha > 0) {
                if (spriteRight != null) {
                    // Fade out the sprite
                    Composite old = g.getComposite();
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                   Math.max(0f, alpha / 255f)));
                    boolean facingRight = (facing == Constants.FACING_RIGHT);
                    BufferedImage sprite = facingRight ? spriteRight : spriteLeft;
                    int offsetX = sx + (width - SPRITE_W) / 2;
                    int offsetY = sy + (height - SPRITE_H);
                    int expand = (int) (progress * 8);
                    g.drawImage(sprite, offsetX - expand, offsetY - expand,
                                SPRITE_W + expand * 2, SPRITE_H + expand * 2, null);
                    g.setComposite(old);
                } else {
                    g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                               Math.max(0, Math.min(255, alpha))));
                    int expand = (int) (progress * 12);
                    g.fillOval(sx - expand, sy - expand, width + expand * 2, height + expand * 2);
                }
                // Death particles
                g.setColor(new Color(255, 100, 50, Math.max(0, Math.min(255, alpha))));
                for (int i = 0; i < 4; i++) {
                    double angle = i * Math.PI / 2 + progress * 2;
                    int expand = (int) (progress * 12);
                    int px = sx + width / 2 + (int) (Math.cos(angle) * (8 + expand));
                    int py = sy + height / 2 + (int) (Math.sin(angle) * (8 + expand));
                    g.fillOval(px - 2, py - 2, 4, 4);
                }
            }
            return;
        }

        // Invulnerability blink
        if (invulnTimer > 0 && (invulnTimer / 3) % 2 == 0) {
            return;
        }

        drawMonsterBody(g, sx, sy);
    }

    /**
     * Draw the monster using the sprite image.
     * Falls back to procedural rendering if sprite is not available.
     */
    private void drawMonsterBody(Graphics2D g, int sx, int sy) {
        if (spriteRight != null) {
            // === SPRITE-BASED RENDERING ===
            boolean facingRight = (facing == Constants.FACING_RIGHT);
            BufferedImage sprite = facingRight ? spriteRight : spriteLeft;

            // Center sprite over hitbox horizontally, align feet at bottom
            int offsetX = sx + (width - SPRITE_W) / 2;
            int offsetY = sy + (height - SPRITE_H);

            g.drawImage(sprite, offsetX, offsetY, null);

            // HP indicator (small dots above head)
            for (int i = 0; i < hp; i++) {
                g.setColor(new Color(255, 50, 50));
                g.fillOval(sx + width / 2 - (hp * 3) + i * 6, sy - 8, 4, 4);
            }
        } else {
            // === PROCEDURAL FALLBACK ===
            drawMonsterBodyProcedural(g, sx, sy);
        }
    }

    /**
     * Procedural monster rendering (used only if sprite is not found).
     */
    private void drawMonsterBodyProcedural(Graphics2D g, int sx, int sy) {
        int w = width;
        int h = height;
        boolean facingRight = (facing == Constants.FACING_RIGHT);

        Color darkBody = new Color(
            Math.max(0, color.getRed() - 40),
            Math.max(0, color.getGreen() - 40),
            Math.max(0, color.getBlue() - 40));

        // Shadow
        g.setColor(new Color(0, 0, 0, 40));
        g.fillOval(sx + 2, sy + h - 4, w - 4, 6);

        // === BODY ===
        g.setColor(color);
        g.fillRoundRect(sx + 2, sy + 4, w - 4, h - 6, 6, 6);

        // Head (wider)
        g.fillOval(sx, sy, w, h / 2 + 4);

        // === HORNS / FEATURES per type ===
        g.setColor(darkBody);
        switch (enemyIndex % 4) {
            case 0: // Blood Crawler - two horns
                g.fillOval(sx + 2, sy - 3, 5, 7);
                g.fillOval(sx + w - 7, sy - 3, 5, 7);
                break;
            case 1: // Toxic Slime - drips
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 150));
                int dripY = sy + h - 2 + (int) (3 * Math.sin(eyeAnimOffset * 0.08));
                g.fillOval(sx + 5, dripY, 4, 5);
                g.fillOval(sx + w - 8, dripY - 1, 3, 4);
                g.setColor(darkBody);
                g.fillOval(sx + 4, sy - 1, 4, 4);
                g.fillOval(sx + w / 2 - 1, sy - 2, 4, 4);
                g.fillOval(sx + w - 8, sy - 1, 4, 4);
                break;
            case 2: // Acid Bat - wing ears
                int[] wxL = {sx + 2, sx - 4, sx + 5};
                int[] wyL = {sy + 3, sy - 2, sy + 1};
                g.fillPolygon(wxL, wyL, 3);
                int[] wxR = {sx + w - 2, sx + w + 4, sx + w - 5};
                int[] wyR = {sy + 3, sy - 2, sy + 1};
                g.fillPolygon(wxR, wyR, 3);
                break;
            case 3: // Rock Golem - flat head
                g.fillRect(sx + 3, sy, w - 6, 3);
                g.setColor(new Color(80, 60, 30));
                g.drawLine(sx + w / 2 - 2, sy, sx + w / 2, sy + 5);
                break;
        }

        // === LEGS ===
        g.setColor(darkBody);
        int legAnim = (int) (2 * Math.sin(eyeAnimOffset * 0.15));
        g.fillRoundRect(sx + 4, sy + h - 5 + legAnim, 6, 5, 2, 2);
        g.fillRoundRect(sx + w - 10, sy + h - 5 - legAnim, 6, 5, 2, 2);

        // === CLAWS ===
        g.setColor(darkBody);
        if (facingRight) {
            int[] cx = {sx + w - 1, sx + w + 3, sx + w - 2};
            int[] cy = {sy + h / 2 - 2, sy + h / 2 + 1, sy + h / 2 + 3};
            g.fillPolygon(cx, cy, 3);
        } else {
            int[] cx = {sx + 1, sx - 3, sx + 2};
            int[] cy = {sy + h / 2 - 2, sy + h / 2 + 1, sy + h / 2 + 3};
            g.fillPolygon(cx, cy, 3);
        }

        // === EYES ===
        Color eyeColor;
        switch (enemyIndex % 4) {
            case 0: eyeColor = new Color(255, 200, 50); break;
            case 1: eyeColor = new Color(255, 100, 100); break;
            case 2: eyeColor = new Color(200, 50, 255); break;
            case 3: eyeColor = new Color(255, 150, 50); break;
            default: eyeColor = Color.YELLOW; break;
        }

        int eyeY = sy + h / 3;
        int cx = sx + w / 2;

        // Eye glow
        g.setColor(new Color(eyeColor.getRed(), eyeColor.getGreen(), eyeColor.getBlue(), 50));
        g.fillOval(cx - 8, eyeY - 1, 8, 6);
        g.fillOval(cx, eyeY - 1, 8, 6);

        g.setColor(eyeColor);
        g.fillOval(cx - 6, eyeY, 5, 4);
        g.fillOval(cx + 1, eyeY, 5, 4);

        // Pupils
        g.setColor(new Color(20, 5, 5));
        int pdx = facingRight ? 1 : -1;
        g.fillRect(cx - 4 + pdx, eyeY + 1, 2, 2);
        g.fillRect(cx + 3 + pdx, eyeY + 1, 2, 2);

        // === MOUTH ===
        g.setColor(new Color(40, 10, 10));
        int mouthY = sy + h / 2 + 2;
        g.fillArc(cx - 5, mouthY, 10, 6, 180, 180);
        // Teeth
        g.setColor(new Color(240, 230, 210));
        for (int i = 0; i < 3; i++) {
            g.fillRect(cx - 4 + i * 4, mouthY, 2, 2);
        }

        // HP indicator (small dots above head)
        for (int i = 0; i < hp; i++) {
            g.setColor(new Color(255, 50, 50));
            g.fillOval(sx + w / 2 - (hp * 3) + i * 6, sy - 6, 4, 4);
        }
    }

    // Getters
    public int getEnemyIndex() { return enemyIndex; }
    public Color getColor() { return color; }
}
