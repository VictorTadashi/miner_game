package dungeonexplorer.ui;

import dungeonexplorer.map.GameMap;
import dungeonexplorer.util.Constants;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Renders the cave platformer map with camera offset.
 * Uses a single stone texture for all solid tiles (normal color)
 * and a darkened version for the background behind empty tiles.
 */
public class MapRenderer {
    private int glowTimer = 0;
    private int[][] rockNoise;
    private final Random rng = new Random(42);
    private boolean initialized = false;

    // Single tile image for everything
    private BufferedImage tileStone;      // Normal color - for solid tiles (chao)
    private BufferedImage tileStoneDark;   // Darkened - for background (fundo)
    private boolean imageLoaded = false;

    // Decoration overlay (mineral/gems)
    private BufferedImage tileDecoration;      // Normal color - for solid tiles
    private BufferedImage tileDecorationDark;  // Darkened - for background
    private boolean decorationLoaded = false;

    public MapRenderer() {
        loadTileImage();
    }

    /**
     * Load the single tile image. Falls back to procedural rendering if not found.
     */
    private void loadTileImage() {
        try {
            // Try loading from classpath first (resources)
            InputStream stream = getClass().getResourceAsStream("/tiles/chao_pedra.png");

            if (stream != null) {
                tileStone = ImageIO.read(stream);
                stream.close();
            } else {
                // Fallback: try loading from file path
                File file = new File("src/main/resources/tiles/chao_pedra.png");
                if (file.exists()) {
                    tileStone = ImageIO.read(file);
                }
            }

            // Create darkened version for background
            if (tileStone != null) {
                int ts = Constants.TILE_SIZE;
                tileStoneDark = new BufferedImage(ts, ts, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gd = tileStoneDark.createGraphics();
                gd.drawImage(tileStone, 0, 0, null);
                // Dark overlay to make it look like a distant cave wall
                gd.setColor(new Color(0, 0, 0, 150));
                gd.fillRect(0, 0, ts, ts);
                gd.dispose();
                imageLoaded = true;
            }

            if (imageLoaded) {
                System.out.println("[MapRenderer] Tile image loaded successfully.");
            } else {
                System.out.println("[MapRenderer] Tile image not found, using procedural rendering.");
            }

            // Load decoration image (mineral/gems)
            InputStream decoStream = getClass().getResourceAsStream("/tiles/decoracao.png");
            BufferedImage decoRaw = null;
            if (decoStream != null) {
                decoRaw = ImageIO.read(decoStream);
                decoStream.close();
            } else {
                File decoFile = new File("src/main/resources/tiles/decoracao.png");
                if (decoFile.exists()) {
                    decoRaw = ImageIO.read(decoFile);
                }
            }

            if (decoRaw != null) {
                int ts = Constants.TILE_SIZE;
                // Resize to tile size if needed
                tileDecoration = new BufferedImage(ts, ts, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gDeco = tileDecoration.createGraphics();
                gDeco.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                gDeco.drawImage(decoRaw, 0, 0, ts, ts, null);
                gDeco.dispose();

                // Create darkened version for background
                tileDecorationDark = new BufferedImage(ts, ts, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gDecoDark = tileDecorationDark.createGraphics();
                gDecoDark.drawImage(tileDecoration, 0, 0, null);
                gDecoDark.setColor(new Color(0, 0, 0, 150));
                gDecoDark.fillRect(0, 0, ts, ts);
                gDecoDark.dispose();

                decorationLoaded = true;
                System.out.println("[MapRenderer] Decoration image loaded successfully.");
            } else {
                System.out.println("[MapRenderer] Decoration image not found, skipping.");
            }

        } catch (Exception e) {
            System.err.println("[MapRenderer] Error loading tile image: " + e.getMessage());
            imageLoaded = false;
        }
    }

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
     * Render background behind empty tiles using darkened stone texture.
     */
    private void renderCaveBackground(Graphics2D g, int x, int y, int r, int c) {
        int s = Constants.TILE_SIZE;

        if (tileStoneDark != null) {
            // Darkened stone image as background
            g.drawImage(tileStoneDark, x, y, null);
            // Decoration overlay on background (different hash offset so it doesn't always match floor)
            if (decorationLoaded && isDecorationTileBg(r, c)) {
                g.drawImage(tileDecorationDark, x, y, null);
            }
        } else {
            // Fallback: procedural background
            int noise = rockNoise[r][c];
            int bgR = 28 + noise / 4;
            int bgG = 22 + noise / 5;
            int bgB = 18 + noise / 6;
            g.setColor(new Color(clamp(bgR), clamp(bgG), clamp(bgB)));
            g.fillRect(x, y, s, s);

            g.setColor(new Color(clamp(bgR + 6), clamp(bgG + 4), clamp(bgB + 3)));
            int px = (r * 5 + c * 9) % (s - 4) + 2;
            int py = (r * 9 + c * 3) % (s - 4) + 2;
            g.fillOval(x + px, y + py, 3, 2);

            if ((r * 7 + c * 11) % 13 == 0) {
                g.setColor(new Color(clamp(bgR - 8), clamp(bgG - 6), clamp(bgB - 5)));
                int dx = (r * 3 + c * 7) % (s - 8) + 4;
                int dy = (r * 11 + c * 5) % (s - 8) + 4;
                g.fillOval(x + dx, y + dy, 5, 4);
            }
        }
    }

    /**
     * Render solid tile using the stone texture at normal color.
     */
    private void renderSolidTile(Graphics2D g, int x, int y, int r, int c, GameMap map) {
        int s = Constants.TILE_SIZE;

        if (imageLoaded) {
            // Draw stone texture at normal color
            g.drawImage(tileStone, x, y, null);
        } else {
            // Fallback: procedural rock rendering
            renderSolidTileProcedural(g, x, y, r, c);
        }

        // Decoration overlay on ~12% of solid tiles, spaced apart
        if (decorationLoaded && isDecorationTile(r, c)) {
            g.drawImage(tileDecoration, x, y, null);
        }

        // Edge detection for visual borders (drawn over the image)
        boolean emptyAbove = r > 0 && !map.isSolid(r - 1, c);
        boolean emptyBelow = r < map.getRows() - 1 && !map.isSolid(r + 1, c);
        boolean emptyLeft = c > 0 && !map.isSolid(r, c - 1);
        boolean emptyRight = c < map.getCols() - 1 && !map.isSolid(r, c + 1);

        // Top surface edge
        if (emptyAbove) {
            g.setColor(new Color(40, 55, 30, 180));
            g.fillRect(x, y, s, 2);
        }

        // Bottom edge (shadow)
        if (emptyBelow) {
            g.setColor(new Color(20, 15, 10, 150));
            g.fillRect(x, y + s - 2, s, 2);
        }

        // Side shadows
        if (emptyLeft) {
            g.setColor(new Color(15, 10, 5, 120));
            g.drawLine(x, y, x, y + s - 1);
            g.drawLine(x + 1, y, x + 1, y + s - 1);
        }
        if (emptyRight) {
            g.setColor(new Color(15, 10, 5, 120));
            g.drawLine(x + s - 1, y, x + s - 1, y + s - 1);
            g.drawLine(x + s - 2, y, x + s - 2, y + s - 1);
        }
    }

    /**
     * Fallback procedural rock rendering when image is not available.
     */
    private void renderSolidTileProcedural(Graphics2D g, int x, int y, int r, int c) {
        int s = Constants.TILE_SIZE;
        int noise = rockNoise[r][c];

        int baseR = Constants.COLOR_WALL.getRed() + noise;
        int baseG = Constants.COLOR_WALL.getGreen() + noise / 2;
        int baseB = Constants.COLOR_WALL.getBlue() + noise / 3;
        g.setColor(new Color(clamp(baseR), clamp(baseG), clamp(baseB)));
        g.fillRect(x, y, s, s);

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

        g.setColor(new Color(clamp(baseR + 15), clamp(baseG + 10), clamp(baseB + 8)));
        int speckX = (r * 7 + c * 13) % (s - 6) + 3;
        int speckY = (r * 11 + c * 5) % (s - 6) + 3;
        g.fillOval(x + speckX, y + speckY, 3, 2);
    }

    private void renderGoldBar(Graphics2D g, int x, int y) {
        int s = Constants.TILE_SIZE;
        int cx = x + s / 2;
        int cy = y + s / 2;

        float glowIntensity = 0.3f + 0.2f * (float) Math.sin(glowTimer * 0.05 + x * 0.01);
        int alpha = (int) (glowIntensity * 80);
        g.setColor(new Color(255, 200, 50, Math.max(0, Math.min(255, alpha))));
        g.fillOval(cx - 8, cy - 6, 16, 12);

        g.setColor(Constants.COLOR_GOLD);
        g.fillRoundRect(cx - 6, cy - 3, 12, 7, 2, 2);

        g.setColor(new Color(200, 150, 30));
        g.drawRoundRect(cx - 6, cy - 3, 12, 7, 2, 2);

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

    /**
     * Determines if a solid tile should show the decoration overlay (~12%).
     * Uses deterministic hash so it's consistent across frames.
     * Ensures decorations don't cluster (no two adjacent decorations).
     */
    private boolean isDecorationTile(int r, int c) {
        int hash = (r * 31 + c * 17 + 7) % 100;
        if (hash >= 5) return false; // ~5% chance (reduced from 12%)
        // Avoid clustering: check if any neighbor would also be decoration
        int hashUp = ((r - 1) * 31 + c * 17 + 7) % 100;
        int hashLeft = (r * 31 + (c - 1) * 17 + 7) % 100;
        if (hashUp < 5 || hashLeft < 5) return false;
        return true;
    }

    /**
     * Determines if a background tile should show the decoration overlay (~10%).
     * Uses a different hash offset so floor and background decorations don't always align.
     */
    private boolean isDecorationTileBg(int r, int c) {
        int hash = (r * 23 + c * 41 + 13) % 100;
        if (hash >= 3) return false; // ~3% chance (reduced from 10%)
        // Avoid clustering
        int hashUp = ((r - 1) * 23 + c * 41 + 13) % 100;
        int hashLeft = (r * 23 + (c - 1) * 41 + 13) % 100;
        if (hashUp < 3 || hashLeft < 3) return false;
        return true;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
