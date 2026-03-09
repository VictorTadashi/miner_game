package dungeonexplorer.ui;

import dungeonexplorer.map.GameMap;
import dungeonexplorer.util.Constants;
import java.awt.*;
import java.util.Random;

/**
 * Renders the cave platformer map with camera offset.
 * Solid tiles have detailed rock textures. Empty tiles show a
 * distant cave wall background, giving depth to the cave system.
 */
public class MapRenderer {
    private int glowTimer = 0;
    private int[][] rockNoise;
    private final Random rng = new Random(42);
    private boolean initialized = false;

    public MapRenderer() {}

    public void init(int rows, int cols) {
        rockNoise = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                rockNoise[r][c] = rng.nextInt(30) - 15;
            }
        }
        initialized = true;
    }

    public void update() {
        glowTimer++;
    }

    public void render(Graphics2D g, GameMap map, double camX, double camY, int vpWidth, int vpHeight) {
        if (!initialized) {
            init(map.getRows(), map.getCols());
        }

        int ts = Constants.TILE_SIZE;
        int[][] tiles = map.getTiles();

        // Calculate visible tile range
        int startCol = Math.max(0, (int) (camX / ts));
        int endCol = Math.min(map.getCols() - 1, (int) ((camX + vpWidth) / ts) + 1);
        int startRow = Math.max(0, (int) (camY / ts));
        int endRow = Math.min(map.getRows() - 1, (int) ((camY + vpHeight) / ts) + 1);

        // Fill entire viewport with deep background first
        g.setColor(Constants.COLOR_BG);
        g.fillRect(0, 0, vpWidth, vpHeight);

        // Draw visible tiles
        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                int screenX = (int) (c * ts - camX);
                int screenY = (int) (r * ts - camY);
                int tile = tiles[r][c];

                switch (tile) {
                    case Constants.TILE_SOLID:
                        renderSolidTile(g, screenX, screenY, r, c, map);
                        break;
                    case Constants.TILE_GOLD_BAR:
                        renderCaveBackground(g, screenX, screenY, r, c);
                        renderGoldBar(g, screenX, screenY);
                        break;
                    case Constants.TILE_DIAMOND:
                        renderCaveBackground(g, screenX, screenY, r, c);
                        renderDiamond(g, screenX, screenY);
                        break;
                    case Constants.TILE_EXIT:
                        renderCaveBackground(g, screenX, screenY, r, c);
                        renderExit(g, screenX, screenY, map.isExitOpen());
                        break;
                    default: // EMPTY, spawns, etc.
                        renderCaveBackground(g, screenX, screenY, r, c);
                        break;
                }
            }
        }
    }

    /**
     * Render a distant cave wall behind empty tiles to give depth.
     */
    private void renderCaveBackground(Graphics2D g, int x, int y, int r, int c) {
        int s = Constants.TILE_SIZE;
        int noise = rockNoise[r][c];

        // Distant cave wall (darker than foreground rock)
        int bgR = 28 + noise / 4;
        int bgG = 22 + noise / 5;
        int bgB = 18 + noise / 6;
        g.setColor(new Color(clamp(bgR), clamp(bgG), clamp(bgB)));
        g.fillRect(x, y, s, s);

        // Subtle rock texture in background
        g.setColor(new Color(clamp(bgR + 6), clamp(bgG + 4), clamp(bgB + 3)));
        int px = (r * 5 + c * 9) % (s - 4) + 2;
        int py = (r * 9 + c * 3) % (s - 4) + 2;
        g.fillOval(x + px, y + py, 3, 2);

        // Occasional darker spot (deep shadow)
        if ((r * 7 + c * 11) % 13 == 0) {
            g.setColor(new Color(clamp(bgR - 8), clamp(bgG - 6), clamp(bgB - 5)));
            int dx = (r * 3 + c * 7) % (s - 8) + 4;
            int dy = (r * 11 + c * 5) % (s - 8) + 4;
            g.fillOval(x + dx, y + dy, 5, 4);
        }

        // Stalactite hints hanging from ceiling (if solid above)
        // This is checked in the foreground rendering instead
    }

    private void renderSolidTile(Graphics2D g, int x, int y, int r, int c, GameMap map) {
        int s = Constants.TILE_SIZE;
        int noise = rockNoise[r][c];

        // Base rock color with variation
        int baseR = Constants.COLOR_WALL.getRed() + noise;
        int baseG = Constants.COLOR_WALL.getGreen() + noise / 2;
        int baseB = Constants.COLOR_WALL.getBlue() + noise / 3;
        g.setColor(new Color(clamp(baseR), clamp(baseG), clamp(baseB)));
        g.fillRect(x, y, s, s);

        // Rock texture: cracks and lines
        g.setColor(Constants.COLOR_WALL_HIGHLIGHT);
        int crackSeed = (r * 31 + c * 17) % 7;
        switch (crackSeed) {
            case 0:
                g.drawLine(x + 4, y + 8, x + s - 6, y + s / 2);
                g.drawLine(x + 8, y + s - 8, x + s / 2, y + s - 3);
                break;
            case 1:
                g.drawLine(x + s / 2, y + 3, x + s - 4, y + s / 2 + 2);
                break;
            case 2:
                g.drawLine(x + 3, y + s / 3, x + s - 5, y + s / 3 + 2);
                g.drawLine(x + s / 3, y + s * 2 / 3, x + s - 3, y + s * 2 / 3 - 1);
                break;
            case 3:
                g.drawLine(x + s / 2, y + 2, x + s / 2 + 3, y + s - 4);
                break;
            default:
                g.drawLine(x + 5, y + s / 2, x + s - 5, y + s / 2 + noise % 3);
                break;
        }

        // Small speckles
        g.setColor(new Color(clamp(baseR + 15), clamp(baseG + 10), clamp(baseB + 8)));
        int speckX = (r * 7 + c * 13) % (s - 6) + 3;
        int speckY = (r * 11 + c * 5) % (s - 6) + 3;
        g.fillOval(x + speckX, y + speckY, 3, 2);

        // Edge detection for visual borders
        boolean emptyAbove = r > 0 && !map.isSolid(r - 1, c);
        boolean emptyBelow = r < map.getRows() - 1 && !map.isSolid(r + 1, c);
        boolean emptyLeft = c > 0 && !map.isSolid(r, c - 1);
        boolean emptyRight = c < map.getCols() - 1 && !map.isSolid(r, c + 1);

        // Top surface edge (grass/moss hint on exposed surfaces)
        if (emptyAbove) {
            // Darker top line
            g.setColor(new Color(55, 75, 40));
            g.fillRect(x, y, s, 2);
            // Small grass/moss tufts
            g.setColor(new Color(65, 90, 45));
            for (int i = 0; i < s; i += 5) {
                int grassH = 1 + (r * 3 + c * 5 + i) % 3;
                g.drawLine(x + i, y, x + i, y - grassH);
            }
        }

        // Bottom edge (darker shadow)
        if (emptyBelow) {
            g.setColor(new Color(40, 30, 20));
            g.fillRect(x, y + s - 2, s, 2);
        }

        // Side shadows
        g.setColor(new Color(30, 22, 15));
        if (emptyLeft) {
            g.drawLine(x, y, x, y + s - 1);
            g.drawLine(x + 1, y, x + 1, y + s - 1);
        }
        if (emptyRight) {
            g.drawLine(x + s - 1, y, x + s - 1, y + s - 1);
            g.drawLine(x + s - 2, y, x + s - 2, y + s - 1);
        }

        // Occasional mineral vein (gold/copper in the rock)
        if ((r + c) % 11 == 0) {
            g.setColor(new Color(120, 100, 50, 80));
            g.drawLine(x + 2, y + s / 3, x + s - 2, y + s / 3 + 4);
        }
        // Occasional crystal embedded in rock
        if ((r * 7 + c * 13) % 31 == 0) {
            g.setColor(new Color(100, 180, 200, 60));
            int cx = x + (r * 3 + c * 7) % (s - 8) + 4;
            int cy = y + (r * 7 + c * 3) % (s - 8) + 4;
            g.fillOval(cx, cy, 4, 3);
        }
    }

    private void renderGoldBar(Graphics2D g, int x, int y) {
        int s = Constants.TILE_SIZE;
        int cx = x + s / 2;
        int cy = y + s / 2;

        // Glow
        float glowIntensity = 0.3f + 0.2f * (float) Math.sin(glowTimer * 0.05 + x * 0.01);
        int alpha = (int) (glowIntensity * 80);
        g.setColor(new Color(255, 200, 50, Math.max(0, Math.min(255, alpha))));
        g.fillOval(cx - 8, cy - 6, 16, 12);

        // Gold bar shape
        g.setColor(Constants.COLOR_GOLD);
        g.fillRoundRect(cx - 6, cy - 3, 12, 7, 2, 2);

        // Darker edge
        g.setColor(new Color(200, 150, 30));
        g.drawRoundRect(cx - 6, cy - 3, 12, 7, 2, 2);

        // Shine highlight
        g.setColor(new Color(255, 240, 150));
        g.fillRect(cx - 4, cy - 2, 4, 2);
    }

    private void renderDiamond(Graphics2D g, int x, int y) {
        int s = Constants.TILE_SIZE;
        int cx = x + s / 2;
        int cy = y + s / 2;

        float pulse = 0.7f + 0.3f * (float) Math.sin(glowTimer * 0.08);
        int size = (int) (7 * pulse);

        int glowAlpha = (int) (40 * pulse);
        g.setColor(new Color(100, 220, 255, Math.max(0, Math.min(255, glowAlpha))));
        g.fillOval(cx - size - 3, cy - size - 3, (size + 3) * 2, (size + 3) * 2);

        g.setColor(Constants.COLOR_DIAMOND);
        int[] xPts = {cx, cx + size, cx, cx - size};
        int[] yPts = {cy - size, cy, cy + size / 2, cy};
        g.fillPolygon(xPts, yPts, 4);

        g.setColor(new Color(180, 240, 255));
        int[] xF = {cx, cx + size / 2, cx, cx - size / 2};
        int[] yF = {cy - size, cy - size / 3, cy, cy - size / 3};
        g.fillPolygon(xF, yF, 4);

        g.setColor(new Color(220, 250, 255));
        g.fillOval(cx - 2, cy - 2, 4, 3);
    }

    private void renderExit(Graphics2D g, int x, int y, boolean open) {
        int s = Constants.TILE_SIZE;
        int cx = x + s / 2;

        if (open) {
            float pulse = 0.6f + 0.4f * (float) Math.sin(glowTimer * 0.1);

            g.setColor(new Color(255, 255, 200, (int) (35 * pulse)));
            g.fillOval(x - 3, y - 3, s + 6, s + 6);

            g.setColor(new Color(140, 140, 150));
            g.fillRect(x + 3, y + 1, s - 6, s - 2);

            g.setColor(new Color(200, 195, 170));
            g.fillRect(x + 6, y + 4, s - 12, s - 8);

            g.setColor(new Color(100, 100, 110));
            g.drawLine(x + 6, y, x + 6, y + 4);
            g.drawLine(x + s - 6, y, x + s - 6, y + 4);

            g.setColor(new Color(100, 255, 100));
            int[] arrowX = {cx, cx - 4, cx + 4};
            int[] arrowY = {y + 8, y + 14, y + 14};
            g.fillPolygon(arrowX, arrowY, 3);

            g.setColor(Color.WHITE);
            int sparkX = cx + (int) (5 * Math.sin(glowTimer * 0.15));
            int sparkY = y + s / 2 + (int) (4 * Math.cos(glowTimer * 0.12));
            g.fillOval(sparkX - 1, sparkY - 1, 3, 3);
        } else {
            g.setColor(new Color(90, 80, 70));
            g.fillRect(x + 3, y + 1, s - 6, s - 2);

            g.setColor(new Color(120, 90, 60));
            g.fillRect(x + 6, y + 4, s - 12, s - 8);

            g.setColor(new Color(80, 65, 45));
            for (int i = 0; i < 3; i++) {
                int bx = x + 9 + i * (s - 18) / 3;
                g.drawLine(bx, y + 4, bx, y + s - 4);
            }

            g.setColor(new Color(160, 130, 60));
            g.fillOval(cx - 3, y + s / 2 - 1, 6, 6);
            g.setColor(new Color(110, 90, 40));
            g.fillRect(cx - 1, y + s / 2 + 3, 2, 3);
        }
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
