package dungeonexplorer.map;

import dungeonexplorer.util.Constants;
import java.util.*;

/**
 * Generates side-scrolling platformer levels with a dense cave system.
 * The map is FILLED with solid rock, then corridors are carved out,
 * creating interconnected horizontal tunnels at multiple heights.
 *
 * Layout: corridors (3 tiles high) separated by 1-tile floors.
 * Vertical gaps in the floors allow the player to climb between levels.
 * Internal obstacles (rocks, small platforms) add variety.
 */
public class LevelGenerator {
    private final int rows, cols;
    private final Random rng;

    private static final int CORRIDOR_HEIGHT = 3;
    private static final int FLOOR_THICKNESS = 1;
    private static final int SECTION_HEIGHT = CORRIDOR_HEIGHT + FLOOR_THICKNESS; // 4

    public LevelGenerator(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.rng = new Random();
    }

    public int[][] generate(int level, int enemyCount, int diamondCount) {
        int[][] map = new int[rows][cols];

        // ============================================================
        // STEP 1: Fill EVERYTHING with solid rock
        // ============================================================
        for (int[] row : map) Arrays.fill(row, Constants.TILE_SOLID);

        // ============================================================
        // STEP 2: Determine corridor positions
        // Each corridor: 3 tiles high empty space
        // Separated by 1-tile solid floors
        // ============================================================
        List<Integer> corridorTops = new ArrayList<>();
        int y = 1; // Start 1 row below top (row 0 = ceiling)
        while (y + CORRIDOR_HEIGHT <= rows - 2) { // Leave 2 rows for ground
            corridorTops.add(y);
            y += SECTION_HEIGHT;
        }

        if (corridorTops.isEmpty()) return map;

        // ============================================================
        // STEP 3: Carve main corridors (full width tunnels)
        // ============================================================
        for (int top : corridorTops) {
            for (int r = top; r < Math.min(top + CORRIDOR_HEIGHT, rows - 2); r++) {
                for (int c = 1; c < cols - 1; c++) {
                    map[r][c] = Constants.TILE_EMPTY;
                }
            }
        }

        // ============================================================
        // STEP 4: Create vertical connections between corridors
        // Gaps in the floors allow climbing between levels
        // ============================================================
        for (int i = 0; i < corridorTops.size() - 1; i++) {
            int floorRow = corridorTops.get(i) + CORRIDOR_HEIGHT;
            if (floorRow >= rows - 2) continue;

            // Create well-distributed gaps across the width
            int gapCount = 3 + cols / 15 + level / 3;
            int sectionW = Math.max(1, (cols - 6) / gapCount);

            for (int g = 0; g < gapCount; g++) {
                int baseCol = 3 + g * sectionW;
                int gapCol = baseCol + rng.nextInt(Math.max(1, sectionW - 4));
                gapCol = Math.max(2, Math.min(gapCol, cols - 6));
                int gapWidth = 3 + rng.nextInt(3); // 3-5 tiles wide for comfortable jumping

                for (int c = gapCol; c < Math.min(gapCol + gapWidth, cols - 1); c++) {
                    map[floorRow][c] = Constants.TILE_EMPTY;
                }
            }
        }

        // ============================================================
        // STEP 5: Add internal corridor features for variety
        // ============================================================
        addCorridorFeatures(map, corridorTops, level);

        // ============================================================
        // STEP 6: Add some extra small alcoves / side rooms
        // ============================================================
        addAlcoves(map, corridorTops, level);

        // ============================================================
        // STEP 7: Player spawn (bottom corridor, left side)
        // ============================================================
        int spawnIdx = corridorTops.size() - 1;
        int spawnTop = corridorTops.get(spawnIdx);
        int spawnRow = spawnTop + 1;
        int spawnCol = 2;
        // Clear spawn area
        clearArea(map, spawnTop, 1, CORRIDOR_HEIGHT, 4);
        map[spawnRow][spawnCol] = Constants.TILE_PLAYER_SPAWN;

        // ============================================================
        // STEP 8: Exit (top corridor, right side)
        // ============================================================
        int exitTop = corridorTops.get(0);
        int exitRow = exitTop + 1;
        int exitCol = cols - 3;
        // Clear exit area
        clearArea(map, exitTop, cols - 5, CORRIDOR_HEIGHT, 4);
        map[exitRow][exitCol] = Constants.TILE_EXIT;

        // ============================================================
        // STEP 9: Place gold bars in corridors
        // ============================================================
        placeGoldBars(map, level, corridorTops);

        // ============================================================
        // STEP 10: Place diamonds (rarer, harder to reach)
        // ============================================================
        placeDiamonds(map, diamondCount, corridorTops);

        // ============================================================
        // STEP 11: Place enemy spawns distributed across corridors
        // ============================================================
        placeEnemySpawns(map, enemyCount, corridorTops);

        return map;
    }

    /**
     * Add obstacles and platforms inside corridors for variety.
     */
    private void addCorridorFeatures(int[][] map, List<Integer> corridorTops, int level) {
        for (int idx = 0; idx < corridorTops.size(); idx++) {
            int top = corridorTops.get(idx);
            int bottom = top + CORRIDOR_HEIGHT - 1;

            // Safe zones: don't place obstacles near spawn (last corridor left)
            // or exit (first corridor right)
            int safeLeft = (idx == corridorTops.size() - 1) ? 6 : 2;
            int safeRight = (idx == 0) ? cols - 7 : cols - 2;

            // --- Floor rocks (small bumps to jump over) ---
            int rockCount = cols / 10 + level / 2;
            for (int o = 0; o < rockCount; o++) {
                if (safeRight - safeLeft <= 2) continue;
                int c = safeLeft + rng.nextInt(safeRight - safeLeft);
                if (c >= 1 && c < cols - 1 && map[bottom][c] == Constants.TILE_EMPTY) {
                    // Check there's solid floor below
                    if (bottom + 1 < rows && map[bottom + 1][c] == Constants.TILE_SOLID) {
                        map[bottom][c] = Constants.TILE_SOLID;
                        // Sometimes 2 tiles wide
                        if (rng.nextFloat() < 0.3 && c + 1 < safeRight
                            && map[bottom][c + 1] == Constants.TILE_EMPTY) {
                            map[bottom][c + 1] = Constants.TILE_SOLID;
                        }
                    }
                }
            }

            // --- Mid-height platforms (small ledges for extra navigation) ---
            if (CORRIDOR_HEIGHT >= 3) {
                int midRow = top + 1;
                int platCount = cols / 18 + rng.nextInt(3);
                for (int p = 0; p < platCount; p++) {
                    if (safeRight - safeLeft <= 5) continue;
                    int pCol = safeLeft + rng.nextInt(safeRight - safeLeft - 3);
                    int pWidth = 2 + rng.nextInt(3);
                    for (int c = pCol; c < Math.min(pCol + pWidth, safeRight); c++) {
                        if (map[midRow][c] == Constants.TILE_EMPTY) {
                            map[midRow][c] = Constants.TILE_SOLID;
                        }
                    }
                }
            }

            // --- Partial walls (vertical dividers that don't fully block) ---
            int wallCount = cols / 25 + rng.nextInt(2);
            for (int w = 0; w < wallCount; w++) {
                if (safeRight - safeLeft <= 8) continue;
                int wCol = safeLeft + 3 + rng.nextInt(safeRight - safeLeft - 6);
                // Wall from floor, 1-2 tiles high, leaving top open
                int wallH = 1 + rng.nextInt(2);
                for (int r = bottom; r > bottom - wallH && r >= top; r--) {
                    if (map[r][wCol] == Constants.TILE_EMPTY) {
                        map[r][wCol] = Constants.TILE_SOLID;
                    }
                }
            }
        }
    }

    /**
     * Add small alcoves branching off corridors for hidden treasures.
     */
    private void addAlcoves(int[][] map, List<Integer> corridorTops, int level) {
        int alcoveCount = corridorTops.size() / 2 + level / 2;
        for (int a = 0; a < alcoveCount; a++) {
            int ci = rng.nextInt(corridorTops.size());
            int top = corridorTops.get(ci);

            // Try carving a small alcove above or below the corridor
            boolean above = rng.nextBoolean();
            int alcoveRow = above ? top - 1 : top + CORRIDOR_HEIGHT;
            if (alcoveRow < 1 || alcoveRow >= rows - 2) continue;

            int alcoveCol = 4 + rng.nextInt(cols - 8);
            int alcoveW = 3 + rng.nextInt(4);

            // Only carve if the space is solid
            boolean canCarve = true;
            for (int c = alcoveCol; c < Math.min(alcoveCol + alcoveW, cols - 1); c++) {
                if (map[alcoveRow][c] != Constants.TILE_SOLID) {
                    canCarve = false;
                    break;
                }
            }

            if (canCarve) {
                for (int c = alcoveCol; c < Math.min(alcoveCol + alcoveW, cols - 1); c++) {
                    map[alcoveRow][c] = Constants.TILE_EMPTY;
                }
            }
        }
    }

    /**
     * Clear an area (ensure it's empty) for spawn/exit placement.
     */
    private void clearArea(int[][] map, int top, int left, int height, int width) {
        for (int r = top; r < Math.min(top + height, rows - 2); r++) {
            for (int c = left; c < Math.min(left + width, cols - 1); c++) {
                if (r > 0 && c > 0) {
                    map[r][c] = Constants.TILE_EMPTY;
                }
            }
        }
    }

    /**
     * Place gold bars on surfaces (empty tile with solid below) inside corridors.
     */
    private void placeGoldBars(int[][] map, int level, List<Integer> corridorTops) {
        int target = 12 + level * 4 + cols / 12;
        int placed = 0;
        int attempts = 0;

        // Place on corridor floors (standing on ground)
        while (placed < target && attempts < target * 20) {
            attempts++;
            int ci = rng.nextInt(corridorTops.size());
            int top = corridorTops.get(ci);
            int c = 3 + rng.nextInt(cols - 6);

            // Find a surface in this corridor
            for (int r = top + CORRIDOR_HEIGHT - 1; r >= top; r--) {
                if (map[r][c] == Constants.TILE_EMPTY &&
                    r + 1 < rows && map[r + 1][c] == Constants.TILE_SOLID) {
                    map[r][c] = Constants.TILE_GOLD_BAR;
                    placed++;
                    break;
                }
            }
        }

        // Place some floating gold (mid-air, requires jumping)
        int floatingTarget = 3 + level * 2;
        attempts = 0;
        while (floatingTarget > 0 && attempts < floatingTarget * 15) {
            attempts++;
            int ci = rng.nextInt(corridorTops.size());
            int top = corridorTops.get(ci);
            int r = top + rng.nextInt(CORRIDOR_HEIGHT);
            int c = 3 + rng.nextInt(cols - 6);

            if (map[r][c] == Constants.TILE_EMPTY) {
                map[r][c] = Constants.TILE_GOLD_BAR;
                floatingTarget--;
                placed++;
            }
        }
    }

    /**
     * Place diamonds in harder-to-reach spots.
     */
    private void placeDiamonds(int[][] map, int count, List<Integer> corridorTops) {
        int placed = 0;
        int attempts = 0;

        while (placed < count && attempts < count * 25) {
            attempts++;
            int ci = rng.nextInt(corridorTops.size());
            int top = corridorTops.get(ci);
            int r = top + rng.nextInt(CORRIDOR_HEIGHT);
            int c = 5 + rng.nextInt(cols - 10);

            if (map[r][c] == Constants.TILE_EMPTY) {
                map[r][c] = Constants.TILE_DIAMOND;
                placed++;
            }
        }
    }

    /**
     * Place enemy spawns distributed across corridors on ground surfaces.
     */
    private void placeEnemySpawns(int[][] map, int count, List<Integer> corridorTops) {
        int placed = 0;
        int sectionW = Math.max(1, (cols - 10) / Math.max(count, 1));

        for (int i = 0; i < count; i++) {
            int ci = i % corridorTops.size();
            int top = corridorTops.get(ci);
            int baseCol = 6 + (i * sectionW) % (cols - 12);

            int attempts = 0;
            while (attempts < 50) {
                attempts++;
                int c = baseCol + rng.nextInt(Math.max(1, sectionW));
                c = Math.max(3, Math.min(c, cols - 4));

                // Find a standing position
                for (int r = top + CORRIDOR_HEIGHT - 1; r >= top; r--) {
                    if (map[r][c] == Constants.TILE_EMPTY &&
                        r + 1 < rows && map[r + 1][c] == Constants.TILE_SOLID) {
                        map[r][c] = Constants.TILE_ENEMY_SPAWN;
                        placed++;
                        break;
                    }
                }
                if (placed > i) break;
            }
        }
    }
}
