package dungeonexplorer.map;

import dungeonexplorer.util.Constants;
import java.util.*;

/**
 * Generates side-scrolling platformer levels with a dense cave system.
 * The map is FILLED with solid rock, then segmented corridors are carved out,
 * creating tight interconnected tunnels at multiple heights.
 *
 * Layout: corridors (3 tiles high) separated by 1-tile floors.
 * Corridors are broken into segments with walls between them.
 * Passages between segments are 2 tiles high to ensure player access.
 *
 * Spawn room: a clean rectangular area at the bottom-left corner with
 * an open corridor leading right into the rest of the map.
 */
public class LevelGenerator {
    private final int rows, cols;
    private final Random rng;

    // Marks tiles that belong to inter-segment passages (protected from features)
    private boolean[][] passageZones;

    private static final int CORRIDOR_HEIGHT = 3;
    private static final int FLOOR_THICKNESS = 1;
    private static final int SECTION_HEIGHT = CORRIDOR_HEIGHT + FLOOR_THICKNESS; // 4

    // Spawn room dimensions (clean rectangular area)
    private static final int SPAWN_ROOM_WIDTH = 10;
    private static final int EXIT_ROOM_WIDTH = 10;

    public LevelGenerator(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.rng = new Random();
    }

    public int[][] generate(int level, int enemyCount, int diamondCount) {
        int[][] map = new int[rows][cols];

        // Initialize passage zones tracking
        passageZones = new boolean[rows][cols];

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

        // Pre-calculate spawn and exit positions (used throughout generation)
        int spawnCorridorIdx = corridorTops.size() - 1;
        int spawnTop = corridorTops.get(spawnCorridorIdx);
        int spawnRow = spawnTop + CORRIDOR_HEIGHT - 1; // Bottom of corridor (standing on floor)
        int spawnCol = 3;

        int exitCorridorIdx = 0;
        int exitTop = corridorTops.get(exitCorridorIdx);
        int exitRow = exitTop + CORRIDOR_HEIGHT - 1; // Bottom of corridor (on floor)
        int exitCol = cols - 4;

        // ============================================================
        // STEP 3: Carve segmented corridors with 2-tile-high passages
        // Each corridor is broken into segments with solid walls
        // between them. Each wall has a 2-tile-high passage.
        // ============================================================
        for (int top : corridorTops) {
            int c = 1;
            while (c < cols - 1) {
                // Determine segment length (6-14 tiles wide) — shorter = more walls = tighter
                int segmentLen = 6 + rng.nextInt(9);
                segmentLen = Math.min(segmentLen, cols - 1 - c);

                // Carve this segment
                for (int r = top; r < Math.min(top + CORRIDOR_HEIGHT, rows - 2); r++) {
                    for (int sc = c; sc < c + segmentLen && sc < cols - 1; sc++) {
                        map[r][sc] = Constants.TILE_EMPTY;
                    }
                }

                c += segmentLen;

                // Add a wall/divider between segments (1-3 tiles wide)
                // Carve a 2-tile-high passage for player access
                if (c < cols - 3) {
                    int wallWidth = 1 + rng.nextInt(3);

                    // Choose 2 consecutive rows for the passage
                    int passageRow1, passageRow2;
                    if (rng.nextBoolean()) {
                        // Passage at top of corridor
                        passageRow1 = top;
                        passageRow2 = top + 1;
                    } else {
                        // Passage at bottom of corridor
                        passageRow1 = top + CORRIDOR_HEIGHT - 2;
                        passageRow2 = top + CORRIDOR_HEIGHT - 1;
                    }

                    // Carve both rows across the wall width
                    for (int wc = c; wc < Math.min(c + wallWidth, cols - 1); wc++) {
                        if (passageRow1 >= 0 && passageRow1 < rows - 1) {
                            map[passageRow1][wc] = Constants.TILE_EMPTY;
                        }
                        if (passageRow2 >= 0 && passageRow2 < rows - 1) {
                            map[passageRow2][wc] = Constants.TILE_EMPTY;
                        }
                    }

                    // Mark passage zones (passage tiles + 1-tile margin on each side)
                    int marginLeft = Math.max(1, c - 1);
                    int marginRight = Math.min(cols - 2, c + wallWidth);
                    for (int mc = marginLeft; mc <= marginRight; mc++) {
                        if (passageRow1 >= 0 && passageRow1 < rows) {
                            passageZones[passageRow1][mc] = true;
                        }
                        if (passageRow2 >= 0 && passageRow2 < rows) {
                            passageZones[passageRow2][mc] = true;
                        }
                    }

                    c += wallWidth;
                }
            }
        }

        // ============================================================
        // STEP 4: Create vertical connections between corridors
        // Fewer, narrower gaps for tighter navigation
        // ============================================================
        for (int i = 0; i < corridorTops.size() - 1; i++) {
            int floorRow = corridorTops.get(i) + CORRIDOR_HEIGHT;
            if (floorRow >= rows - 2) continue;

            int gapCount = 2 + cols / 25 + level / 4;
            int sectionW = Math.max(1, (cols - 6) / gapCount);

            for (int g = 0; g < gapCount; g++) {
                int baseCol = 3 + g * sectionW;
                int gapCol = baseCol + rng.nextInt(Math.max(1, sectionW - 4));
                gapCol = Math.max(2, Math.min(gapCol, cols - 6));
                int gapWidth = 2 + rng.nextInt(2); // 2-3 tiles wide

                for (int gc = gapCol; gc < Math.min(gapCol + gapWidth, cols - 1); gc++) {
                    map[floorRow][gc] = Constants.TILE_EMPTY;
                }
            }
        }

        // ============================================================
        // STEP 5: Add internal corridor features for variety
        // Protected passage zones + minimum feature sizes
        // Skip spawn room area and exit room area
        // ============================================================
        addCorridorFeatures(map, corridorTops, level);

        // ============================================================
        // STEP 6: Ensure connectivity (player-height-aware BFS)
        // Uses coordinates only — no tile markers placed yet
        // ============================================================
        // Temporarily clear spawn and exit areas for connectivity check
        clearArea(map, spawnTop, 1, CORRIDOR_HEIGHT, SPAWN_ROOM_WIDTH);
        clearArea(map, exitTop, cols - EXIT_ROOM_WIDTH - 1, CORRIDOR_HEIGHT, EXIT_ROOM_WIDTH);
        ensureConnectivity(map, spawnRow, spawnCol, exitRow, exitCol, corridorTops);

        // ============================================================
        // STEP 7: Remove stray isolated solid tiles (cleanup)
        // ============================================================
        removeStrayTiles(map);

        // ============================================================
        // STEP 8: Create spawn room (bottom-left, clean rectangle)
        // This runs AFTER all map modifications to prevent overwriting
        // ============================================================
        createSpawnRoom(map, spawnTop, spawnRow, spawnCol);

        // ============================================================
        // STEP 9: Create exit room (top-right, clean rectangle)
        // ============================================================
        createExitRoom(map, exitTop, exitRow, exitCol);

        // ============================================================
        // STEP 10: Place gold bars in corridors
        // ============================================================
        placeGoldBars(map, level, corridorTops);

        // ============================================================
        // STEP 11: Place diamonds (rarer, harder to reach)
        // ============================================================
        placeDiamonds(map, diamondCount, corridorTops);

        // ============================================================
        // STEP 12: Place enemy spawns distributed across corridors
        // ============================================================
        placeEnemySpawns(map, enemyCount, corridorTops);

        return map;
    }

    /**
     * Create a clean spawn room at bottom-left corner of the map.
     * The room is a clear rectangle with a corridor leading right.
     * Player spawn is placed at the bottom of the room (standing position).
     */
    private void createSpawnRoom(int[][] map, int spawnTop, int spawnRow, int spawnCol) {
        // Clear the entire spawn room area (SPAWN_ROOM_WIDTH tiles wide, full corridor height)
        for (int r = spawnTop; r < Math.min(spawnTop + CORRIDOR_HEIGHT, rows - 1); r++) {
            for (int c = 1; c < Math.min(1 + SPAWN_ROOM_WIDTH, cols - 1); c++) {
                map[r][c] = Constants.TILE_EMPTY;
            }
        }

        // Ensure solid floor exists below the spawn room
        int floorRow = spawnTop + CORRIDOR_HEIGHT;
        if (floorRow < rows) {
            for (int c = 0; c < Math.min(1 + SPAWN_ROOM_WIDTH + 1, cols); c++) {
                map[floorRow][c] = Constants.TILE_SOLID;
            }
        }

        // Ensure solid ceiling exists above the spawn room
        if (spawnTop > 0) {
            for (int c = 0; c < Math.min(1 + SPAWN_ROOM_WIDTH + 1, cols); c++) {
                // Only set ceiling if it's not part of a corridor above
                if (map[spawnTop - 1][c] == Constants.TILE_SOLID) {
                    // Keep it solid (it's already a ceiling/floor separator)
                }
            }
        }

        // Ensure the corridor opening on the right side connects to the rest of the map
        // Clear 2 tiles wide at the right edge of the spawn room for smooth transition
        int openingCol = 1 + SPAWN_ROOM_WIDTH;
        for (int r = spawnTop; r < Math.min(spawnTop + CORRIDOR_HEIGHT, rows - 1); r++) {
            if (openingCol < cols - 1) {
                map[r][openingCol] = Constants.TILE_EMPTY;
            }
            if (openingCol + 1 < cols - 1) {
                map[r][openingCol + 1] = Constants.TILE_EMPTY;
            }
        }

        // Place player spawn marker
        map[spawnRow][spawnCol] = Constants.TILE_PLAYER_SPAWN;
    }

    /**
     * Create a clean exit room at top-right corner of the map.
     */
    private void createExitRoom(int[][] map, int exitTop, int exitRow, int exitCol) {
        int exitLeft = cols - EXIT_ROOM_WIDTH - 1;

        // Clear the exit room
        for (int r = exitTop; r < Math.min(exitTop + CORRIDOR_HEIGHT, rows - 1); r++) {
            for (int c = Math.max(1, exitLeft); c < cols - 1; c++) {
                map[r][c] = Constants.TILE_EMPTY;
            }
        }

        // Ensure solid floor exists below the exit room
        int floorRow = exitTop + CORRIDOR_HEIGHT;
        if (floorRow < rows) {
            for (int c = Math.max(0, exitLeft - 1); c < cols; c++) {
                map[floorRow][c] = Constants.TILE_SOLID;
            }
        }

        // Ensure the corridor opening on the left side connects to the rest of the map
        int openingCol = exitLeft - 1;
        for (int r = exitTop; r < Math.min(exitTop + CORRIDOR_HEIGHT, rows - 1); r++) {
            if (openingCol >= 1) {
                map[r][openingCol] = Constants.TILE_EMPTY;
            }
            if (openingCol - 1 >= 1) {
                map[r][openingCol - 1] = Constants.TILE_EMPTY;
            }
        }

        // Place exit marker (at bottom of corridor, on the floor)
        map[exitRow][exitCol] = Constants.TILE_EXIT;
    }

    /**
     * Ensure spawn can reach exit via player-height-aware flood-fill.
     * If not reachable, add extra vertical gaps and clear horizontal blockages.
     */
    private void ensureConnectivity(int[][] map, int spawnRow, int spawnCol,
                                     int exitRow, int exitCol, List<Integer> corridorTops) {
        int maxAttempts = 20;
        int attempt = 0;

        // Phase 1: Try adding random vertical gaps
        while (!isReachable(map, spawnRow, spawnCol, exitRow, exitCol) && attempt < maxAttempts) {
            attempt++;
            if (corridorTops.size() < 2) break;
            int ci = rng.nextInt(corridorTops.size() - 1);
            int floorRow = corridorTops.get(ci) + CORRIDOR_HEIGHT;
            if (floorRow >= rows - 2) continue;

            int gapCol = 4 + rng.nextInt(Math.max(1, cols - 8));
            int gapWidth = 3 + rng.nextInt(2);
            for (int c = gapCol; c < Math.min(gapCol + gapWidth, cols - 1); c++) {
                map[floorRow][c] = Constants.TILE_EMPTY;
            }
        }

        // Phase 2: If still not reachable, punch small 2-tile-high holes
        // through blockages — NOT full corridor clearing (keeps map tight)
        if (!isReachable(map, spawnRow, spawnCol, exitRow, exitCol)) {
            // Punch narrow (3-tile wide) passages at regular intervals in each corridor
            for (int top : corridorTops) {
                int walkRow1 = top;
                int walkRow2 = top + 1;
                if (walkRow2 >= rows - 1) continue;
                // Every 10 tiles, punch a 3-tile-wide hole through any blockage
                for (int c = 5; c < cols - 5; c += 10) {
                    for (int dc = -1; dc <= 1; dc++) {
                        int cc = c + dc;
                        if (cc >= 1 && cc < cols - 1) {
                            if (map[walkRow1][cc] == Constants.TILE_SOLID) {
                                map[walkRow1][cc] = Constants.TILE_EMPTY;
                            }
                            if (map[walkRow2][cc] == Constants.TILE_SOLID) {
                                map[walkRow2][cc] = Constants.TILE_EMPTY;
                            }
                        }
                    }
                }
            }
            // Ensure vertical connections with small gaps
            for (int i = 0; i < corridorTops.size() - 1; i++) {
                int floorRow = corridorTops.get(i) + CORRIDOR_HEIGHT;
                if (floorRow >= rows - 1) continue;
                int center = cols / 2;
                for (int c = center - 1; c <= center + 1 && c < cols - 1; c++) {
                    if (c > 0) map[floorRow][c] = Constants.TILE_EMPTY;
                }
            }
        }
    }

    /**
     * Player-height-aware BFS: a tile (r,c) is only passable if both
     * (r,c) and (r-1,c) are non-solid (player needs 2 tiles of vertical space).
     */
    private boolean isReachable(int[][] map, int startRow, int startCol, int endRow, int endCol) {
        boolean[][] visited = new boolean[rows][cols];
        Queue<int[]> queue = new LinkedList<>();

        // Only start if the start position has 2-tile clearance
        if (startRow >= 1 && map[startRow][startCol] != Constants.TILE_SOLID
            && map[startRow - 1][startCol] != Constants.TILE_SOLID) {
            queue.add(new int[]{startRow, startCol});
            visited[startRow][startCol] = true;
        }

        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            int r = pos[0], c = pos[1];
            if (r == endRow && c == endCol) return true;

            for (int[] d : dirs) {
                int nr = r + d[0], nc = c + d[1];
                if (nr >= 1 && nr < rows && nc >= 0 && nc < cols
                    && !visited[nr][nc]
                    && map[nr][nc] != Constants.TILE_SOLID
                    && map[nr - 1][nc] != Constants.TILE_SOLID) {
                    visited[nr][nc] = true;
                    queue.add(new int[]{nr, nc});
                }
            }
        }
        return false;
    }

    /**
     * Remove isolated solid tiles that appear as stray pixels.
     * A solid tile with 3+ non-solid orthogonal neighbors is removed.
     * Runs multiple passes to handle cascading removals.
     */
    private void removeStrayTiles(int[][] map) {
        for (int pass = 0; pass < 2; pass++) {
            boolean changed = false;
            for (int r = 1; r < rows - 1; r++) {
                for (int c = 1; c < cols - 1; c++) {
                    if (map[r][c] != Constants.TILE_SOLID) continue;
                    int emptyNeighbors = 0;
                    if (map[r - 1][c] != Constants.TILE_SOLID) emptyNeighbors++;
                    if (map[r + 1][c] != Constants.TILE_SOLID) emptyNeighbors++;
                    if (map[r][c - 1] != Constants.TILE_SOLID) emptyNeighbors++;
                    if (map[r][c + 1] != Constants.TILE_SOLID) emptyNeighbors++;
                    if (emptyNeighbors == 4) {
                        map[r][c] = Constants.TILE_EMPTY;
                        changed = true;
                    }
                }
            }
            if (!changed) break;
        }
    }

    /**
     * Add obstacles and platforms inside corridors for variety.
     * Respects passage zones and enforces minimum feature sizes.
     * Skips spawn room area (last corridor, first SPAWN_ROOM_WIDTH+2 cols)
     * and exit room area (first corridor, last EXIT_ROOM_WIDTH+2 cols).
     */
    private void addCorridorFeatures(int[][] map, List<Integer> corridorTops, int level) {
        for (int idx = 0; idx < corridorTops.size(); idx++) {
            int top = corridorTops.get(idx);
            int bottom = top + CORRIDOR_HEIGHT - 1;

            // Safe zones: don't place obstacles near spawn (last corridor left)
            // or exit (first corridor right)
            int safeLeft = (idx == corridorTops.size() - 1) ? SPAWN_ROOM_WIDTH + 3 : 2;
            int safeRight = (idx == 0) ? cols - EXIT_ROOM_WIDTH - 3 : cols - 2;

            // --- Floor rocks (minimum 2 tiles wide, no single-tile bumps) ---
            int rockCount = cols / 5 + level / 2;
            for (int o = 0; o < rockCount; o++) {
                if (safeRight - safeLeft <= 4) continue;
                int c = safeLeft + rng.nextInt(safeRight - safeLeft - 1);

                // Must have room for 2 tiles, not in passage zone
                if (c < 1 || c + 1 >= cols - 1) continue;
                if (passageZones[bottom][c] || passageZones[bottom][c + 1]) continue;

                if (map[bottom][c] == Constants.TILE_EMPTY
                    && map[bottom][c + 1] == Constants.TILE_EMPTY
                    && bottom + 1 < rows
                    && map[bottom + 1][c] == Constants.TILE_SOLID
                    && map[bottom + 1][c + 1] == Constants.TILE_SOLID) {
                    map[bottom][c] = Constants.TILE_SOLID;
                    map[bottom][c + 1] = Constants.TILE_SOLID;
                }
            }

            // --- Mid-height platforms (minimum 3 tiles wide) ---
            // Skip first corridor (idx=0) — platforms near the ceiling create
            // unreachable areas where gold can spawn but player can't reach
            if (CORRIDOR_HEIGHT >= 3 && idx > 0) {
                int midRow = top + 1;
                int platCount = cols / 20 + rng.nextInt(2);
                for (int p = 0; p < platCount; p++) {
                    if (safeRight - safeLeft <= 6) continue;
                    int pCol = safeLeft + rng.nextInt(safeRight - safeLeft - 4);
                    int pWidth = 3 + rng.nextInt(3); // 3-5 tiles wide

                    // Verify all tiles are available and not in passage zones
                    boolean canPlace = true;
                    for (int pc = pCol; pc < Math.min(pCol + pWidth, safeRight); pc++) {
                        if (pc >= cols - 1 || passageZones[midRow][pc]
                            || map[midRow][pc] != Constants.TILE_EMPTY) {
                            canPlace = false;
                            break;
                        }
                    }
                    int actualWidth = Math.min(pWidth, safeRight - pCol);
                    if (canPlace && actualWidth >= 3) {
                        for (int pc = pCol; pc < pCol + actualWidth && pc < cols - 1; pc++) {
                            map[midRow][pc] = Constants.TILE_SOLID;
                        }
                    }
                }
            }

            // --- Partial walls (minimum 2 tiles wide) ---
            int wallCount = cols / 10 + rng.nextInt(3);
            for (int w = 0; w < wallCount; w++) {
                if (safeRight - safeLeft <= 8) continue;
                int wCol = safeLeft + 3 + rng.nextInt(safeRight - safeLeft - 6);

                if (wCol + 1 >= cols - 1) continue;

                boolean blocked = false;
                for (int r = bottom; r > bottom - 2 && r >= top; r--) {
                    if (passageZones[r][wCol] || passageZones[r][wCol + 1]) {
                        blocked = true;
                        break;
                    }
                }
                if (blocked) continue;

                // Wall from floor, 2 tiles high, 2 tiles wide
                for (int r = bottom; r > bottom - 2 && r >= top; r--) {
                    if (map[r][wCol] == Constants.TILE_EMPTY) {
                        map[r][wCol] = Constants.TILE_SOLID;
                    }
                    if (map[r][wCol + 1] == Constants.TILE_EMPTY) {
                        map[r][wCol + 1] = Constants.TILE_SOLID;
                    }
                }
            }
        }
    }

    /**
     * Clear an area (ensure it's empty) for spawn/exit placement.
     */
    private void clearArea(int[][] map, int top, int left, int height, int width) {
        for (int r = top; r < Math.min(top + height, rows - 1); r++) {
            for (int c = left; c < Math.min(left + width, cols - 1); c++) {
                if (r > 0 && c > 0) {
                    map[r][c] = Constants.TILE_EMPTY;
                }
            }
        }
    }

    /**
     * Place gold bars on surfaces (empty tile with solid below) inside corridors.
     * Avoids spawn room and exit room areas.
     */
    private void placeGoldBars(int[][] map, int level, List<Integer> corridorTops) {
        int target = 12 + level * 4 + cols / 12;
        int placed = 0;
        int attempts = 0;

        while (placed < target && attempts < target * 20) {
            attempts++;
            int ci = rng.nextInt(corridorTops.size());
            int top = corridorTops.get(ci);
            int c = 3 + rng.nextInt(cols - 6);

            // Skip first corridor (top of map) — gold there is often unreachable
            if (ci == 0) continue;
            // Skip spawn room area (last corridor, left side)
            if (ci == corridorTops.size() - 1 && c <= SPAWN_ROOM_WIDTH + 1) continue;
            // Skip exit room area (first corridor, right side)
            if (ci == 0 && c >= cols - EXIT_ROOM_WIDTH - 2) continue;

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

            // Skip first corridor (top of map) — gold there is often unreachable
            if (ci == 0) continue;
            // Skip spawn/exit rooms
            if (ci == corridorTops.size() - 1 && c <= SPAWN_ROOM_WIDTH + 1) continue;
            if (ci == 0 && c >= cols - EXIT_ROOM_WIDTH - 2) continue;

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
            // Skip first corridor (top of map) — often unreachable
            if (ci == 0) continue;
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
     * Avoids spawn room area so enemies don't spawn on top of player.
     */
    private void placeEnemySpawns(int[][] map, int count, List<Integer> corridorTops) {
        int placed = 0;
        int sectionW = Math.max(1, (cols - 10) / Math.max(count, 1));

        for (int i = 0; i < count; i++) {
            int ci = i % corridorTops.size();
            int top = corridorTops.get(ci);
            int baseCol = 6 + (i * sectionW) % (cols - 12);

            // Skip spawn room area for enemies in the last corridor
            if (ci == corridorTops.size() - 1 && baseCol <= SPAWN_ROOM_WIDTH + 2) {
                baseCol = SPAWN_ROOM_WIDTH + 3;
            }

            int attempts = 0;
            while (attempts < 100) {
                attempts++;
                int c = baseCol + rng.nextInt(Math.max(1, sectionW));
                c = Math.max(SPAWN_ROOM_WIDTH + 3, Math.min(c, cols - 4));

                // Extra safety: never spawn enemies in spawn room
                if (ci == corridorTops.size() - 1 && c <= SPAWN_ROOM_WIDTH + 1) continue;

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
