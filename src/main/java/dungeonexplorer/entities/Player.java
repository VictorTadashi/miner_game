package dungeonexplorer.entities;

import dungeonexplorer.map.GameMap;
import dungeonexplorer.util.Constants;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * The player character - a Cave Miner rendered from a sprite image.
 * The sprite already includes the pickaxe, so it is not drawn separately.
 * Falls back to procedural rendering if the sprite image is not found.
 */
public class Player extends Entity {
    private int lives;
    private int score;
    private boolean alive;
    private int deathAnimTimer;

    // Attack
    private boolean attacking;
    private int attackTimer;
    private int attackCooldown;

    // Invulnerability after taking damage
    private int invulnTimer;

    // Input state (held keys)
    private boolean moveLeft, moveRight, jumpPressed;

    // Sprite images (shared across all Player instances)
    private static BufferedImage spriteRight;
    private static BufferedImage spriteLeft;
    private static BufferedImage[] walkRight;  // walk animation frames (right-facing)
    private static BufferedImage[] walkLeft;   // walk animation frames (left-facing)
    private static boolean spriteLoaded = false;

    // Walk animation state (per instance)
    private int walkAnimTimer = 0;
    private int walkFrame = 0;

    public Player(double spawnX, double spawnY) {
        super(spawnX, spawnY, Constants.PLAYER_WIDTH, Constants.PLAYER_HEIGHT);
        this.lives = Constants.PLAYER_LIVES;
        this.score = 0;
        this.alive = true;
        this.deathAnimTimer = 0;
        this.attacking = false;
        this.attackTimer = 0;
        this.attackCooldown = 0;
        this.invulnTimer = 0;

        // Load sprite on first instantiation
        if (!spriteLoaded) {
            loadSprite();
        }
    }

    /**
     * Load a single sprite image from classpath or file fallback.
     * Returns the raw BufferedImage, or null if not found.
     */
    private static BufferedImage loadImage(String filename) {
        try {
            InputStream stream = Player.class.getResourceAsStream("/tiles/" + filename);
            if (stream != null) {
                BufferedImage img = ImageIO.read(stream);
                stream.close();
                return img;
            }
            File file = new File("tiles/" + filename);
            if (file.exists()) {
                return ImageIO.read(file);
            }
        } catch (Exception e) {
            System.out.println("[Player] Error loading " + filename + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Scale a raw image to the game sprite size and create a flipped version.
     * Returns {right, left} or null if raw is null.
     */
    private static BufferedImage[] scaleAndFlip(BufferedImage raw) {
        if (raw == null) return null;
        int sw = Constants.SPRITE_DRAW_WIDTH;
        int sh = Constants.SPRITE_DRAW_HEIGHT;

        BufferedImage right = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = right.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        sg.drawImage(raw, 0, 0, sw, sh, null);
        sg.dispose();

        BufferedImage left = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
        sg = left.createGraphics();
        sg.drawImage(right, sw, 0, -sw, sh, null);
        sg.dispose();

        return new BufferedImage[]{right, left};
    }

    /**
     * Load all miner sprite images: idle + walking frames.
     * Creates horizontally flipped versions for left-facing direction.
     * Falls back to procedural rendering if images are not found.
     */
    private static void loadSprite() {
        spriteLoaded = true;
        try {
            // Load idle sprite
            BufferedImage[] idle = scaleAndFlip(loadImage("minerador.png"));
            if (idle != null) {
                spriteRight = idle[0];
                spriteLeft = idle[1];
                System.out.println("[Player] Idle sprite loaded.");
            } else {
                System.out.println("[Player] Idle sprite not found, using procedural rendering.");
            }

            // Load walk animation frames
            BufferedImage[] walk1 = scaleAndFlip(loadImage("minerador_walk1.png"));
            BufferedImage[] walk2 = scaleAndFlip(loadImage("minerador_walk2.png"));

            if (walk1 != null && walk2 != null) {
                walkRight = new BufferedImage[]{walk1[0], walk2[0]};
                walkLeft = new BufferedImage[]{walk1[1], walk2[1]};
                System.out.println("[Player] Walk sprites loaded (2 frames).");
            } else {
                System.out.println("[Player] Walk sprites not found, walking will use idle sprite.");
            }
        } catch (Exception e) {
            System.out.println("[Player] Sprite load error: " + e.getMessage());
            spriteRight = null;
            spriteLeft = null;
            walkRight = null;
            walkLeft = null;
        }
    }

    /** Get the sprite image for external use (e.g., title screen). */
    public static BufferedImage getSpriteRight() {
        if (!spriteLoaded) loadSprite();
        return spriteRight;
    }

    @Override
    public void update(GameMap map) {
        if (!alive) {
            deathAnimTimer++;
            velY += Constants.GRAVITY;
            return;
        }

        // Horizontal movement from held keys
        if (moveLeft) {
            velX = -Constants.PLAYER_WALK_SPEED;
            facing = Constants.FACING_LEFT;
        } else if (moveRight) {
            velX = Constants.PLAYER_WALK_SPEED;
            facing = Constants.FACING_RIGHT;
        } else {
            velX *= Constants.FRICTION;
            if (Math.abs(velX) < 0.3) velX = 0;
        }

        // Jump
        if (jumpPressed && onGround) {
            velY = Constants.JUMP_FORCE;
            onGround = false;
        }

        // Physics (gravity, collision)
        super.update(map);

        // Walk animation timer
        if (Math.abs(velX) > 0.5 && onGround) {
            walkAnimTimer++;
            if (walkAnimTimer >= Constants.WALK_ANIM_INTERVAL) {
                walkAnimTimer = 0;
                walkFrame = (walkFrame + 1) % 2;
            }
        } else {
            walkAnimTimer = 0;
            walkFrame = 0;
        }

        // Attack timer
        if (attacking) {
            attackTimer--;
            if (attackTimer <= 0) {
                attacking = false;
            }
        }
        if (attackCooldown > 0) attackCooldown--;

        // Invulnerability timer
        if (invulnTimer > 0) invulnTimer--;
    }

    public void startAttack() {
        if (!attacking && attackCooldown <= 0 && alive) {
            attacking = true;
            attackTimer = Constants.PLAYER_ATTACK_DURATION;
            attackCooldown = Constants.PLAYER_ATTACK_COOLDOWN;
        }
    }

    /** Get the attack hitbox rectangle for collision with enemies. */
    public Rectangle getAttackHitbox() {
        if (!attacking) return null;
        int ax, ay;
        int aw = (int) Constants.PLAYER_ATTACK_RANGE;
        int ah = height;
        if (facing == Constants.FACING_RIGHT) {
            ax = (int) (x + width);
            ay = (int) y;
        } else {
            ax = (int) (x - aw);
            ay = (int) y;
        }
        return new Rectangle(ax, ay, aw, ah);
    }

    public void takeDamage() {
        if (invulnTimer > 0 || !alive) return;
        lives--;
        invulnTimer = Constants.PLAYER_INVULN_FRAMES;
        if (lives <= 0) {
            die();
        }
    }

    public void die() {
        alive = false;
        deathAnimTimer = 0;
        velY = Constants.JUMP_FORCE * 0.6;
    }

    public void respawn(double spawnX, double spawnY) {
        setPosition(spawnX, spawnY);
        alive = true;
        deathAnimTimer = 0;
        attacking = false;
        attackTimer = 0;
        attackCooldown = 0;
        invulnTimer = Constants.PLAYER_INVULN_FRAMES;
    }

    public void addScore(int points) { score += points; }

    // Input setters
    public void setMoveLeft(boolean v) { this.moveLeft = v; }
    public void setMoveRight(boolean v) { this.moveRight = v; }
    public void setJumpPressed(boolean v) { this.jumpPressed = v; }

    @Override
    public void render(Graphics2D g, double camX, double camY) {
        int sx = (int) (x - camX);
        int sy = (int) (y - camY);

        if (!alive) {
            // Death animation - miner flies up and fades
            float deathProgress = Math.min(deathAnimTimer / 60.0f, 1.0f);
            int alpha = (int) (255 * (1.0f - deathProgress));
            if (alpha > 0) {
                Composite old = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255f));
                drawMinerBody(g, sx, sy);
                g.setComposite(old);
            }
            return;
        }

        // Invulnerability blink effect
        if (invulnTimer > 0 && (invulnTimer / 4) % 2 == 0) {
            return; // Skip rendering for blink
        }

        drawMinerBody(g, sx, sy);

        // Draw attack visual effect (no separate pickaxe — it's in the sprite)
        if (attacking) {
            drawAttackEffect(g, sx, sy);
        }
    }

    /**
     * Draw the miner body using the sprite image.
     * Sprites are pre-aligned by center of mass so frame swaps are smooth.
     * Falls back to procedural rendering if sprite is not available.
     */
    private void drawMinerBody(Graphics2D g, int sx, int sy) {
        if (spriteRight != null) {
            // === SPRITE-BASED RENDERING ===
            boolean facingRight = (facing == Constants.FACING_RIGHT);
            boolean isWalking = Math.abs(velX) > 0.5 && onGround;

            // Select appropriate sprite: walking frames or idle
            BufferedImage sprite;
            if (isWalking && walkRight != null) {
                sprite = facingRight ? walkRight[walkFrame] : walkLeft[walkFrame];
            } else {
                sprite = facingRight ? spriteRight : spriteLeft;
            }

            int drawW = Constants.SPRITE_DRAW_WIDTH;
            int drawH = Constants.SPRITE_DRAW_HEIGHT;

            // Center sprite over hitbox horizontally, align feet at bottom
            int offsetX = sx + (width - drawW) / 2;
            int offsetY = sy + (height - drawH);

            g.drawImage(sprite, offsetX, offsetY, null);

            // Helmet light beam effect (adds atmosphere)
            int lampX = facingRight ? offsetX + drawW - 8 : offsetX + 2;
            int lampY = offsetY + 3;
            drawHelmetBeam(g, lampX + 3, lampY, facingRight);
        } else {
            // === PROCEDURAL FALLBACK ===
            drawMinerBodyProcedural(g, sx, sy);
        }
    }

    /**
     * Attack visual effect — swing trail and spark.
     * The pickaxe is part of the sprite, so we only draw effects.
     */
    private void drawAttackEffect(Graphics2D g, int sx, int sy) {
        boolean facingRight = (facing == Constants.FACING_RIGHT);
        float swingProgress = 1.0f - (attackTimer / (float) Constants.PLAYER_ATTACK_DURATION);
        double angle = -Math.PI / 3 + swingProgress * Math.PI * 0.8;

        int pivotX = facingRight ? sx + width + 4 : sx - 4;
        int pivotY = sy + 10;

        int swingLen = 20;
        int endX = pivotX + (int) (Math.cos(angle) * swingLen * (facingRight ? 1 : -1));
        int endY = pivotY + (int) (Math.sin(angle) * swingLen);

        // Swing arc trail
        if (swingProgress > 0.1f && swingProgress < 0.9f) {
            g.setColor(new Color(200, 200, 220, 50));
            g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(pivotX, pivotY, endX, endY);
        }

        // Swing motion line
        if (swingProgress > 0.2f && swingProgress < 0.8f) {
            g.setColor(new Color(255, 255, 255, 40));
            g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(pivotX, pivotY, endX, endY);
        }

        g.setStroke(new BasicStroke(1));

        // Spark on impact (middle of swing)
        if (swingProgress > 0.3f && swingProgress < 0.6f) {
            g.setColor(new Color(255, 220, 100, 200));
            g.fillOval(endX - 4, endY - 4, 8, 8);
            // Extra spark particles
            g.setColor(new Color(255, 255, 180, 150));
            g.fillOval(endX - 2, endY - 7, 4, 4);
            g.fillOval(endX + 3, endY - 2, 3, 3);
        }
    }

    private void drawHelmetBeam(Graphics2D g, int lx, int ly, boolean facingRight) {
        int beamLen = 28;
        int beamW = 14;
        int flickAlpha = 18 + (int) (10 * Math.sin(animTimer * 0.12));

        g.setColor(new Color(255, 255, 150, Math.max(0, Math.min(255, flickAlpha))));
        if (facingRight) {
            int[] xp = {lx, lx + beamLen, lx + beamLen, lx};
            int[] yp = {ly - 2, ly - beamW / 2, ly + beamW / 2, ly + 4};
            g.fillPolygon(xp, yp, 4);
        } else {
            int[] xp = {lx, lx - beamLen, lx - beamLen, lx};
            int[] yp = {ly - 2, ly - beamW / 2, ly + beamW / 2, ly + 4};
            g.fillPolygon(xp, yp, 4);
        }
    }

    // ================================================================
    // PROCEDURAL FALLBACK (used only if sprite image is not found)
    // ================================================================

    private void drawMinerBodyProcedural(Graphics2D g, int sx, int sy) {
        int w = width;
        int h = height;
        boolean facingRight = (facing == Constants.FACING_RIGHT);

        // === LEGS ===
        int legY = sy + h - 8;
        g.setColor(new Color(60, 50, 40));
        int legAnim = 0;
        if (Math.abs(velX) > 0.5 && onGround) {
            legAnim = (int) (3 * Math.sin(animTimer * 0.4));
        }
        g.fillRoundRect(sx + 3, legY + legAnim, 7, 8, 2, 2);
        g.fillRoundRect(sx + w - 10, legY - legAnim, 7, 8, 2, 2);

        // === BODY ===
        g.setColor(Constants.COLOR_PLAYER_DARK);
        g.fillRoundRect(sx + 2, sy + 12, w - 4, h - 18, 4, 4);

        g.setColor(new Color(50, 75, 120));
        g.drawLine(sx + 6, sy + 12, sx + 6, sy + 18);
        g.drawLine(sx + w - 6, sy + 12, sx + w - 6, sy + 18);

        g.setColor(new Color(120, 90, 50));
        g.fillRect(sx + 3, sy + 17, w - 6, 3);
        g.setColor(new Color(200, 180, 60));
        g.fillRect(sx + w / 2 - 2, sy + 17, 4, 3);

        // === HEAD ===
        g.setColor(Constants.COLOR_SKIN);
        g.fillOval(sx + 3, sy + 2, w - 6, 13);

        // === HELMET ===
        g.setColor(Constants.COLOR_HELMET);
        g.fillArc(sx + 1, sy - 1, w - 2, 11, 0, 180);
        g.fillRoundRect(sx, sy + 4, w, 3, 2, 2);
        g.setColor(new Color(170, 150, 30));
        g.drawArc(sx + 1, sy - 1, w - 2, 11, 0, 180);

        int lampX = facingRight ? sx + w - 8 : sx + 2;
        g.setColor(new Color(180, 180, 180));
        g.fillRoundRect(lampX, sy, 6, 4, 2, 2);
        int flickAlpha = 200 + (int) (55 * Math.sin(animTimer * 0.15));
        g.setColor(new Color(255, 255, 180, Math.max(0, Math.min(255, flickAlpha))));
        g.fillOval(lampX + 1, sy + 1, 4, 2);

        drawHelmetBeam(g, lampX + 3, sy + 1, facingRight);

        // === EYES ===
        int eyeY = sy + 6;
        g.setColor(Color.WHITE);
        if (facingRight) {
            g.fillOval(sx + 10, eyeY, 4, 4);
            g.fillOval(sx + 15, eyeY, 4, 4);
            g.setColor(new Color(40, 30, 20));
            g.fillOval(sx + 12, eyeY + 1, 2, 2);
            g.fillOval(sx + 17, eyeY + 1, 2, 2);
        } else {
            g.fillOval(sx + 3, eyeY, 4, 4);
            g.fillOval(sx + 8, eyeY, 4, 4);
            g.setColor(new Color(40, 30, 20));
            g.fillOval(sx + 4, eyeY + 1, 2, 2);
            g.fillOval(sx + 9, eyeY + 1, 2, 2);
        }

        // === ARM + PICKAXE (procedural fallback only) ===
        if (!attacking) {
            g.setColor(Constants.COLOR_SKIN);
            int armX = facingRight ? sx + w - 3 : sx - 3;
            g.fillRoundRect(armX, sy + 14, 6, 10, 2, 2);

            g.setColor(Constants.COLOR_PICKAXE_HANDLE);
            int handleX = facingRight ? sx + w + 1 : sx - 10;
            g.fillRect(handleX, sy + 10, 3, 18);

            g.setColor(Constants.COLOR_PICKAXE_HEAD);
            if (facingRight) {
                int[] px = {handleX + 1, handleX + 10, handleX + 1};
                int[] py = {sy + 8, sy + 10, sy + 12};
                g.fillPolygon(px, py, 3);
            } else {
                int[] px = {handleX + 2, handleX - 7, handleX + 2};
                int[] py = {sy + 8, sy + 10, sy + 12};
                g.fillPolygon(px, py, 3);
            }
        }
    }

    /** Draw a small miner helmet icon for HUD lives display. */
    public static void drawMiniHelmet(Graphics2D g, int x, int y) {
        g.setColor(Constants.COLOR_HELMET);
        g.fillArc(x, y, 16, 12, 0, 180);
        g.fillRect(x - 1, y + 6, 18, 3);
        g.setColor(new Color(255, 255, 180));
        g.fillOval(x + 6, y + 1, 4, 3);
    }

    public boolean isDeathAnimComplete() { return !alive && deathAnimTimer >= 60; }
    public boolean isInvulnerable() { return invulnTimer > 0; }
    public boolean isAttacking() { return attacking; }

    // Getters
    public int getLives() { return lives; }
    public int getScore() { return score; }
    public boolean isAlive() { return alive; }
    public void setLives(int lives) { this.lives = lives; }
    public void setScore(int score) { this.score = score; }
}
