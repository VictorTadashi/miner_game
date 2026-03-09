package dungeonexplorer.entities;

import dungeonexplorer.map.GameMap;
import dungeonexplorer.util.Constants;
import java.awt.*;

/**
 * The player character - a full-body Cave Miner with pickaxe for side-scrolling platformer.
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

        // Draw pickaxe attack effect
        if (attacking) {
            drawPickaxeSwing(g, sx, sy);
        }
    }

    private void drawMinerBody(Graphics2D g, int sx, int sy) {
        int w = width;
        int h = height;
        boolean facingRight = (facing == Constants.FACING_RIGHT);
        int flipX = facingRight ? 0 : w;
        int flipDir = facingRight ? 1 : -1;

        // === LEGS (bottom) ===
        int legY = sy + h - 8;
        g.setColor(new Color(60, 50, 40)); // Brown boots
        int legAnim = 0;
        if (Math.abs(velX) > 0.5 && onGround) {
            legAnim = (int) (3 * Math.sin(animTimer * 0.4));
        }
        // Left leg
        g.fillRoundRect(sx + 3, legY + legAnim, 7, 8, 2, 2);
        // Right leg
        g.fillRoundRect(sx + w - 10, legY - legAnim, 7, 8, 2, 2);

        // === BODY (overalls) ===
        g.setColor(Constants.COLOR_PLAYER_DARK);
        g.fillRoundRect(sx + 2, sy + 12, w - 4, h - 18, 4, 4);

        // Overall straps
        g.setColor(new Color(50, 75, 120));
        g.drawLine(sx + 6, sy + 12, sx + 6, sy + 18);
        g.drawLine(sx + w - 6, sy + 12, sx + w - 6, sy + 18);

        // Belt
        g.setColor(new Color(120, 90, 50));
        g.fillRect(sx + 3, sy + 17, w - 6, 3);
        // Belt buckle
        g.setColor(new Color(200, 180, 60));
        g.fillRect(sx + w / 2 - 2, sy + 17, 4, 3);

        // === HEAD ===
        g.setColor(Constants.COLOR_SKIN);
        g.fillOval(sx + 3, sy + 2, w - 6, 13);

        // === HELMET ===
        g.setColor(Constants.COLOR_HELMET);
        g.fillArc(sx + 1, sy - 1, w - 2, 11, 0, 180);
        // Brim
        g.fillRoundRect(sx, sy + 4, w, 3, 2, 2);

        // Darker helmet edge
        g.setColor(new Color(170, 150, 30));
        g.drawArc(sx + 1, sy - 1, w - 2, 11, 0, 180);

        // === LAMP on helmet ===
        int lampX = facingRight ? sx + w - 8 : sx + 2;
        g.setColor(new Color(180, 180, 180));
        g.fillRoundRect(lampX, sy, 6, 4, 2, 2);
        // Lamp glow
        int flickAlpha = 200 + (int) (55 * Math.sin(animTimer * 0.15));
        g.setColor(new Color(255, 255, 180, Math.max(0, Math.min(255, flickAlpha))));
        g.fillOval(lampX + 1, sy + 1, 4, 2);

        // Helmet light beam
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

        // === ARM holding pickaxe (only when not attacking) ===
        if (!attacking) {
            drawPickaxeIdle(g, sx, sy, w, h, facingRight);
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

    private void drawPickaxeIdle(Graphics2D g, int sx, int sy, int w, int h, boolean facingRight) {
        // Arm
        g.setColor(Constants.COLOR_SKIN);
        int armX = facingRight ? sx + w - 3 : sx - 3;
        g.fillRoundRect(armX, sy + 14, 6, 10, 2, 2);

        // Pickaxe handle
        g.setColor(Constants.COLOR_PICKAXE_HANDLE);
        int handleX = facingRight ? sx + w + 1 : sx - 10;
        g.fillRect(handleX, sy + 10, 3, 18);

        // Pickaxe head
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

    private void drawPickaxeSwing(Graphics2D g, int sx, int sy) {
        boolean facingRight = (facing == Constants.FACING_RIGHT);
        float swingProgress = 1.0f - (attackTimer / (float) Constants.PLAYER_ATTACK_DURATION);
        double angle = -Math.PI / 3 + swingProgress * Math.PI * 0.8;

        int pivotX = facingRight ? sx + width : sx;
        int pivotY = sy + 10;

        int handleLen = 22;
        int endX = pivotX + (int) (Math.cos(angle) * handleLen * (facingRight ? 1 : -1));
        int endY = pivotY + (int) (Math.sin(angle) * handleLen);

        // Swing trail effect
        if (swingProgress > 0.2f && swingProgress < 0.8f) {
            g.setColor(new Color(200, 200, 220, 60));
            g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(pivotX, pivotY, endX, endY);
        }

        // Handle
        g.setColor(Constants.COLOR_PICKAXE_HANDLE);
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(pivotX, pivotY, endX, endY);

        // Pickaxe head at end
        g.setColor(Constants.COLOR_PICKAXE_HEAD);
        int headSize = 8;
        double perpAngle = angle + Math.PI / 2;
        int hx1 = endX + (int) (Math.cos(perpAngle) * headSize * (facingRight ? 1 : -1));
        int hy1 = endY + (int) (Math.sin(perpAngle) * headSize);
        int hx2 = endX - (int) (Math.cos(perpAngle) * headSize / 2 * (facingRight ? 1 : -1));
        int hy2 = endY - (int) (Math.sin(perpAngle) * headSize / 2);

        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(hx1, hy1, hx2, hy2);

        // Reset stroke
        g.setStroke(new BasicStroke(1));

        // Spark on impact (middle of swing)
        if (swingProgress > 0.3f && swingProgress < 0.6f) {
            g.setColor(new Color(255, 220, 100, 180));
            g.fillOval(endX - 3, endY - 3, 6, 6);
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
