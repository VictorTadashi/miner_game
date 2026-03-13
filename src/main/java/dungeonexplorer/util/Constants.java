package dungeonexplorer.util;

import java.awt.*;

public final class Constants {
    private Constants() {}

    // Tile size in pixels
    public static final int TILE_SIZE = 32;

    // Viewport (fixed window size)
    public static final int VIEWPORT_WIDTH = 512;   // 16 tiles * 32
    public static final int VIEWPORT_HEIGHT = 384;   // 12 tiles * 32

    // HUD
    public static final int HUD_HEIGHT = 50;

    // Game speed
    public static final int FPS = 60;
    public static final long FRAME_TIME_NS = 1_000_000_000L / FPS;

    // Physics
    public static final double GRAVITY = 0.55;
    public static final double MAX_FALL_SPEED = 11.0;
    public static final double JUMP_FORCE = -12.5;
    public static final double PLAYER_WALK_SPEED = 3.5;
    public static final double FRICTION = 0.8;

    // Player
    public static final int PLAYER_WIDTH = 22;
    public static final int PLAYER_HEIGHT = 30;
    public static final int SPRITE_DRAW_WIDTH = 32;
    public static final int SPRITE_DRAW_HEIGHT = 40;
    public static final int WALK_ANIM_INTERVAL = 20;  // frames between walk sprite swap (~0.33s at 60FPS)
    public static final int PLAYER_LIVES = 3;
    public static final int PLAYER_ATTACK_COOLDOWN = 25; // frames
    public static final int PLAYER_ATTACK_DURATION = 12; // frames attack anim lasts
    public static final double PLAYER_ATTACK_RANGE = 36;
    public static final double PLAYER_ATTACK_KNOCKBACK = 7.0;
    public static final int PLAYER_INVULN_FRAMES = 60;

    // Enemy
    public static final int ENEMY_HP = 3;
    public static final int ENEMY_WIDTH = 24;
    public static final int ENEMY_HEIGHT = 26;
    public static final int ENEMY_INVULN_FRAMES = 20;
    public static final double ENEMY_KNOCKBACK_DECAY = 0.85;

    // Scoring
    public static final int GOLD_BAR_SCORE = 10;
    public static final int DIAMOND_SCORE = 50;
    public static final int ENEMY_KILL_SCORE = 200;

    // Colors - Cave theme
    public static final Color COLOR_WALL = new Color(75, 55, 40);
    public static final Color COLOR_WALL_HIGHLIGHT = new Color(100, 78, 55);
    public static final Color COLOR_FLOOR = new Color(35, 28, 22);
    public static final Color COLOR_BG = new Color(18, 14, 10);
    public static final Color COLOR_GOLD = new Color(255, 200, 50);
    public static final Color COLOR_DIAMOND = new Color(100, 220, 255);
    public static final Color COLOR_PLAYER = new Color(220, 160, 60);
    public static final Color COLOR_PLAYER_DARK = new Color(60, 90, 140);
    public static final Color COLOR_HELMET = new Color(200, 180, 40);
    public static final Color COLOR_HELMET_LIGHT = new Color(255, 255, 180);
    public static final Color COLOR_SKIN = new Color(230, 185, 140);
    public static final Color COLOR_PICKAXE_HANDLE = new Color(140, 100, 50);
    public static final Color COLOR_PICKAXE_HEAD = new Color(170, 170, 180);
    public static final Color COLOR_EXIT_DOOR = new Color(180, 180, 180);
    public static final Color COLOR_EXIT_GLOW = new Color(255, 255, 200, 80);
    public static final Color COLOR_HUD_BG = new Color(25, 18, 12);
    public static final Color COLOR_TEXT = new Color(220, 210, 190);

    // Monster colors
    public static final Color[] ENEMY_COLORS = {
        new Color(180, 50, 50),    // Blood Crawler
        new Color(60, 180, 60),    // Toxic Slime
        new Color(140, 60, 180),   // Acid Bat
        new Color(160, 120, 60),   // Rock Golem
        new Color(180, 80, 80),    // Cave Spider
        new Color(80, 160, 80),    // Moss Beast
        new Color(160, 80, 160),   // Shadow Lurker
    };

    // Facing
    public static final int FACING_RIGHT = 1;
    public static final int FACING_LEFT = -1;

    // Tile types
    public static final int TILE_EMPTY = 0;
    public static final int TILE_SOLID = 1;
    public static final int TILE_PLATFORM = 2;  // pass-through from below
    public static final int TILE_GOLD_BAR = 3;
    public static final int TILE_DIAMOND = 4;
    public static final int TILE_EXIT = 5;
    public static final int TILE_PLAYER_SPAWN = 6;
    public static final int TILE_ENEMY_SPAWN = 7;
}
