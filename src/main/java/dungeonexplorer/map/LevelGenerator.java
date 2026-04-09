package dungeonexplorer.map;

import dungeonexplorer.util.Constants;
import java.util.*;

/**
 * Generates Hollow Knight-style cave levels with wide open corridors.
 *
 * Layout philosophy:
 * - Large open corridors (4-6 tiles high) stacked vertically
 * - Solid floors (2 tiles thick) separate corridors
 * - Wide vertical shafts (3-6 tiles) connect corridors
 * - Corridors are clean and open — no random blocks scattered inside
 * - Climbing platforms near shafts for upward traversal
 * - Optional thin ledge platforms in tall corridors for exploration
 * - Spawn at top-left, exit at bottom-right
 */
public class LevelGenerator {
    private final int rows, cols;
    private final Random rng;

    private static final int CORRIDOR_MIN_HEIGHT = 4;
    private static final int CORRIDOR_MAX_HEIGHT = 6;
    private static final int FLOOR_THICKNESS = 2;

    private static final int SPAWN_ROOM_WIDTH = 10;
    private static final int EXIT_ROOM_WIDTH = 10;

    public LevelGenerator(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.rng = new Random();
    }

    public int[][] generate(int level, int enemyCount, int diamondCount) {
        int[][] map = new int[rows][cols];

        // ============================================================
        // STEP 1: Fill everything with solid rock
        // ============================================================
        for (int[] row : map) Arrays.fill(row, Constants.TILE_SOLID);

        // ============================================================
        // STEP 2: Determine corridor layout (variable heights 4-6)
        // ============================================================
        List<int[]> corridors = new ArrayList<>(); // Each entry: {topRow, height}
        int y = 1; // Row 0 = solid ceiling
        while (y + CORRIDOR_MIN_HEIGHT <= rows - 1) {
            int h = CORRIDOR_MIN_HEIGHT + rng.nextInt(CORRIDOR_MAX_HEIGHT - CORRIDOR_MIN_HEIGHT + 1);
            h = Math.min(h, rows - 1 - y);
            if (h < CORRIDOR_MIN_HEIGHT) break;
            corridors.add(new int[]{y, h});
            y += h + FLOOR_THICKNESS;
        }
        if (corridors.isEmpty()) return map;

        // ============================================================
        // STEP 3: Carve fully open corridors (no internal walls/blocks)
        // ============================================================
        for (int[] corridor : corridors) {
            int top = corridor[0];
            int h = corridor[1];
            for (int r = top; r < top + h && r < rows; r++) {
                for (int c = 1; c < cols - 1; c++) {
                    map[r][c] = Constants.TILE_EMPTY;
                }
            }
        }

        // ============================================================
        // STEP 4: Create vertical shafts between corridors
        // Wide openings with climbing platforms for traversal
        // ============================================================
        createVerticalShafts(map, corridors);

        // ============================================================
        // STEP 4.5: Place ladders in one shaft per floor boundary
        // ============================================================
        placeLadders(map, corridors);

        // ============================================================
        // STEP 5: Add thin ledge platforms in tall corridors
        // ============================================================
        addLedgePlatforms(map, corridors, level);

        // ============================================================
        // STEP 6: Determine spawn/exit positions
        // Spawn: top-left (first corridor)
        // Exit: bottom-right (last corridor)
        // ============================================================
        int spawnTop = corridors.get(0)[0];
        int spawnH = corridors.get(0)[1];
        int spawnRow = spawnTop + spawnH - 1;
        int spawnCol = 3;

        int exitIdx = corridors.size() - 1;
        int exitTop = corridors.get(exitIdx)[0];
        int exitH = corridors.get(exitIdx)[1];
        int exitRow = exitTop + exitH - 1;
        int exitCol = cols - 4;

        // ============================================================
        // STEP 7: Ensure spawn can reach exit
        // ============================================================
        ensureConnectivity(map, spawnRow, spawnCol, exitRow, exitCol, corridors);

        // ============================================================
        // STEP 8: Create clean spawn room (top-left)
        // ============================================================
        createSpawnRoom(map, spawnTop, spawnH, spawnRow, spawnCol);

        // ============================================================
        // STEP 9: Create clean exit room (bottom-right)
        // ============================================================
        createExitRoom(map, exitTop, exitH, exitRow, exitCol);

        // ============================================================
        // STEP 10: Remove stray isolated solid tiles
        // ============================================================
        removeStrayTiles(map);

        // ============================================================
        // STEP 11: Place gold bars
        // ============================================================
        placeGoldBars(map, level, corridors);

        // ============================================================
        // STEP 12: Place diamonds
        // ============================================================
        placeDiamonds(map, diamondCount, corridors);

        // ============================================================
        // STEP 13: Place enemy spawns
        // ============================================================
        placeEnemySpawns(map, enemyCount, corridors);

        return map;
    }

    // ================================================================
    // Vertical Shafts — wide openings connecting corridors
    // ================================================================

    /**
     * Create wide vertical shafts connecting adjacent corridors.
     * Each shaft cuts through the solid floor between corridors.
     * Includes climbing platforms so the player can traverse upward.
     *
     * The player's max jump height is ~4.5 tiles, so for corridors
     * taller than 4, intermediate stepping platforms are placed.
     */
    private void createVerticalShafts(int[][] map, List<int[]> corridors) {
        for (int i = 0; i < corridors.size() - 1; i++) {
            int upperTop = corridors.get(i)[0];
            int upperH = corridors.get(i)[1];
            int lowerTop = corridors.get(i + 1)[0];
            int lowerH = corridors.get(i + 1)[1];

            int floorStart = upperTop + upperH;      // First solid row of floor
            int floorEnd = lowerTop - 1;              // Last solid row of floor

            // 2-4 shafts per floor connection
            int shaftCount = 2 + rng.nextInt(3);
            int sectionWidth = Math.max(8, (cols - 8) / shaftCount);

            for (int s = 0; s < shaftCount; s++) {
                int baseCol = 3 + s * sectionWidth;
                int jitter = Math.max(0, sectionWidth - 8);
                int shaftCol = baseCol + (jitter > 0 ? rng.nextInt(jitter) : 0);
                shaftCol = Math.max(2, Math.min(shaftCol, cols - 9));
                int shaftWidth = 3 + rng.nextInt(4); // 3-6 tiles wide
                shaftWidth = Math.min(shaftWidth, cols - 2 - shaftCol);

                // Carve through the floor (open both rows)
                for (int r = floorStart; r <= floorEnd && r < rows; r++) {
                    for (int c = shaftCol; c < Math.min(shaftCol + shaftWidth, cols - 1); c++) {
                        map[r][c] = Constants.TILE_EMPTY;
                    }
                }

                // === Climbing platforms for upward traversal ===
                // Platform A: inside the shaft at the lower floor row
                // This lets the player jump from the lower corridor into the shaft
                if (shaftWidth >= 3) {
                    int platWidth = Math.min(3, shaftWidth - 1);
                    int platStart = shaftCol + (shaftWidth - platWidth) / 2;
                    for (int c = platStart; c < platStart + platWidth && c < cols - 1; c++) {
                        if (floorEnd >= 0 && floorEnd < rows) {
                            map[floorEnd][c] = Constants.TILE_SOLID;
                        }
                    }
                }

                // Platform B: mid-height in the lower corridor, near the shaft
                // Needed when the lower corridor is tall (>=5 tiles) so the
                // player can reach platform A in two jumps instead of one
                if (lowerH >= 5) {
                    int midRow = lowerTop + 2; // Near the ceiling of the lower corridor
                    int midWidth = Math.min(3, shaftWidth);
                    int midStart = shaftCol;
                    // Alternate side to avoid blocking the shaft center
                    if (s % 2 == 1) {
                        midStart = Math.max(1, shaftCol - 3);
                    } else {
                        midStart = Math.min(cols - midWidth - 1, shaftCol + shaftWidth);
                    }
                    for (int c = midStart; c < midStart + midWidth && c < cols - 1; c++) {
                        if (c > 0 && midRow >= 0 && midRow < rows
                            && map[midRow][c] == Constants.TILE_EMPTY) {
                            map[midRow][c] = Constants.TILE_SOLID;
                        }
                    }
                }
            }
        }
    }

    // ================================================================
    // Ladders — one ladder per floor boundary, placed in an existing shaft
    // ================================================================

    /**
     * For each pair of adjacent corridors, find the first column where
     * the entire floor section is TILE_EMPTY (a shaft was carved there)
     * and mark it as TILE_LADDER, extending 2 tiles into each corridor.
     */
    private void placeLadders(int[][] map, List<int[]> corridors) {
        for (int i = 0; i < corridors.size() - 1; i++) {
            int upperTop = corridors.get(i)[0];
            int upperH   = corridors.get(i)[1];
            int lowerTop = corridors.get(i + 1)[0];

            int floorStart = upperTop + upperH; // first solid row of floor
            int floorEnd   = lowerTop - 1;      // last solid row of floor

            // Find first column fully open through the floor section
            int ladderCol = -1;
            for (int c = 2; c < cols - 2; c++) {
                boolean allEmpty = true;
                for (int r = floorStart; r <= floorEnd; r++) {
                    if (map[r][c] != Constants.TILE_EMPTY) {
                        allEmpty = false;
                        break;
                    }
                }
                if (allEmpty) {
                    ladderCol = c;
                    break;
                }
            }

            if (ladderCol == -1) continue;

            // Ladder starts at the last air row of the upper corridor (walkable floor level)
            // and ends at the ground row of the lower corridor.
            // Floor rows (floorStart..floorEnd) are force-cleared to TILE_LADDER so the
            // shaft is always passable even if a platform tile was placed there.
            int ladderTop    = floorStart - 1;
            int ladderBottom = lowerTop + corridors.get(i + 1)[1] - 1;

            for (int r = ladderTop; r <= ladderBottom && r < rows; r++) {
                if (r >= floorStart && r <= floorEnd) {
                    // Force the two floor-barrier rows to become ladder
                    map[r][ladderCol] = Constants.TILE_LADDER;
                } else if (map[r][ladderCol] == Constants.TILE_EMPTY) {
                    map[r][ladderCol] = Constants.TILE_LADDER;
                }
            }
        }
    }

    // ================================================================
    // Ledge Platforms — thin platforms in tall corridors for variety
    // ================================================================

    /**
     * Add thin platforms (1 tile high, 3-5 tiles wide) inside tall corridors.
     * These are placed at mid-height for exploration and gold placement.
     * Only placed in corridors with height >= 5.
     */
    private void addLedgePlatforms(int[][] map, List<int[]> corridors, int level) {
        for (int idx = 0; idx < corridors.size(); idx++) {
            int top = corridors.get(idx)[0];
            int h = corridors.get(idx)[1];

            // Only add ledges in tall corridors
            if (h < 5) continue;

            // Determine safe column ranges (skip spawn/exit rooms)
            int safeLeft = (idx == 0) ? SPAWN_ROOM_WIDTH + 3 : 4;
            int safeRight = (idx == corridors.size() - 1) ? cols - EXIT_ROOM_WIDTH - 3 : cols - 4;
            if (safeRight - safeLeft < 8) continue;

            // Platform row at mid-height
            int platRow = top + h / 2;

            // 1-3 ledges per corridor
            int platCount = 1 + rng.nextInt(2 + level / 3);
            platCount = Math.min(platCount, 4);

            for (int p = 0; p < platCount; p++) {
                int col = safeLeft + rng.nextInt(Math.max(1, safeRight - safeLeft - 5));
                int w = 3 + rng.nextInt(3); // 3-5 tiles wide

                // Verify area is clear
                boolean canPlace = true;
                for (int c = col; c < Math.min(col + w, safeRight); c++) {
                    if (c >= cols - 1 || map[platRow][c] != Constants.TILE_EMPTY) {
                        canPlace = false;
                        break;
                    }
                }

                if (canPlace && w >= 3) {
                    int actualWidth = Math.min(w, safeRight - col);
                    for (int c = col; c < col + actualWidth && c < cols - 1; c++) {
                        map[platRow][c] = Constants.TILE_SOLID;
                    }
                }
            }
        }
    }

    // ================================================================
    // Spawn Room — clean area at top-left
    // ================================================================

    private void createSpawnRoom(int[][] map, int spawnTop, int spawnH,
                                  int spawnRow, int spawnCol) {
        // Clear the spawn room area
        for (int r = spawnTop; r < spawnTop + spawnH && r < rows; r++) {
            for (int c = 1; c < Math.min(1 + SPAWN_ROOM_WIDTH, cols - 1); c++) {
                map[r][c] = Constants.TILE_EMPTY;
            }
        }

        // Ensure solid floor exists below
        int floorRow = spawnTop + spawnH;
        if (floorRow < rows) {
            for (int c = 0; c <= Math.min(SPAWN_ROOM_WIDTH + 2, cols - 1); c++) {
                map[floorRow][c] = Constants.TILE_SOLID;
            }
        }

        // Ensure corridor opening connects to the rest of the map
        int openCol = 1 + SPAWN_ROOM_WIDTH;
        for (int r = spawnTop; r < spawnTop + spawnH && r < rows; r++) {
            if (openCol < cols - 1) map[r][openCol] = Constants.TILE_EMPTY;
            if (openCol + 1 < cols - 1) map[r][openCol + 1] = Constants.TILE_EMPTY;
        }

        // Place player spawn marker
        map[spawnRow][spawnCol] = Constants.TILE_PLAYER_SPAWN;
    }

    // ================================================================
    // Exit Room — clean area at bottom-right
    // ================================================================

    private void createExitRoom(int[][] map, int exitTop, int exitH,
                                 int exitRow, int exitCol) {
        int exitLeft = cols - EXIT_ROOM_WIDTH - 1;

        // Clear exit room
        for (int r = exitTop; r < exitTop + exitH && r < rows; r++) {
            for (int c = Math.max(1, exitLeft); c < cols - 1; c++) {
                map[r][c] = Constants.TILE_EMPTY;
            }
        }

        // Ensure solid floor below
        int floorRow = exitTop + exitH;
        if (floorRow < rows) {
            for (int c = Math.max(0, exitLeft - 1); c < cols; c++) {
                map[floorRow][c] = Constants.TILE_SOLID;
            }
        }

        // Ensure corridor opening connects
        int openCol = exitLeft - 1;
        for (int r = exitTop; r < exitTop + exitH && r < rows; r++) {
            if (openCol >= 1) map[r][openCol] = Constants.TILE_EMPTY;
            if (openCol - 1 >= 1) map[r][openCol - 1] = Constants.TILE_EMPTY;
        }

        // Place exit marker
        map[exitRow][exitCol] = Constants.TILE_EXIT;
    }

    // ================================================================
    // Connectivity — ensure spawn can reach exit
    // ================================================================

    /**
     * Ensure the player can navigate from spawn to exit via BFS.
     * If not reachable, add extra vertical shafts until connected.
     */
    private void ensureConnectivity(int[][] map, int spawnRow, int spawnCol,
                                     int exitRow, int exitCol, List<int[]> corridors) {
        // Phase 1: Try adding random extra shafts
        int attempt = 0;
        while (!isReachable(map, spawnRow, spawnCol, exitRow, exitCol) && attempt < 25) {
            attempt++;
            if (corridors.size() < 2) break;

            int ci = rng.nextInt(corridors.size() - 1);
            int upperTop = corridors.get(ci)[0];
            int upperH = corridors.get(ci)[1];
            int lowerTop = corridors.get(ci + 1)[0];
            int floorStart = upperTop + upperH;
            int floorEnd = lowerTop - 1;

            int gapCol = 3 + rng.nextInt(Math.max(1, cols - 10));
            int gapWidth = 4 + rng.nextInt(3); // 4-6 tiles wide

            for (int r = floorStart; r <= floorEnd && r < rows; r++) {
                for (int c = gapCol; c < Math.min(gapCol + gapWidth, cols - 1); c++) {
                    if (c > 0) map[r][c] = Constants.TILE_EMPTY;
                }
            }
        }

        // Phase 2: If still unreachable, punch through ALL floors at center
        if (!isReachable(map, spawnRow, spawnCol, exitRow, exitCol)) {
            for (int i = 0; i < corridors.size() - 1; i++) {
                int upperTop = corridors.get(i)[0];
                int upperH = corridors.get(i)[1];
                int lowerTop = corridors.get(i + 1)[0];
                int floorStart = upperTop + upperH;
                int floorEnd = lowerTop - 1;

                int center = cols / 2;
                for (int r = floorStart; r <= floorEnd && r < rows; r++) {
                    for (int c = center - 3; c <= center + 3 && c < cols - 1; c++) {
                        if (c > 0) map[r][c] = Constants.TILE_EMPTY;
                    }
                }
            }
        }
    }

    /**
     * Player-height-aware BFS: checks 2-tile vertical clearance at each position.
     */
    private boolean isReachable(int[][] map, int startRow, int startCol,
                                 int endRow, int endCol) {
        boolean[][] visited = new boolean[rows][cols];
        Queue<int[]> queue = new LinkedList<>();

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

    // ================================================================
    // Cleanup — remove isolated solid tiles
    // ================================================================

    private void removeStrayTiles(int[][] map) {
        for (int pass = 0; pass < 3; pass++) {
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

    // ================================================================
    // Item Placement
    // ================================================================

    /**
     * Place gold bars on surfaces (empty tile with solid below).
     * Skips spawn room (first corridor, left) and exit room (last corridor, right).
     */
    private void placeGoldBars(int[][] map, int level, List<int[]> corridors) {
        int target = 12 + level * 4 + cols / 12;
        int placed = 0;
        int attempts = 0;

        while (placed < target && attempts < target * 20) {
            attempts++;
            int ci = rng.nextInt(corridors.size());
            int top = corridors.get(ci)[0];
            int h = corridors.get(ci)[1];
            int c = 3 + rng.nextInt(Math.max(1, cols - 6));

            // Skip spawn room (first corridor, left side)
            if (ci == 0 && c <= SPAWN_ROOM_WIDTH + 1) continue;
            // Skip exit room (last corridor, right side)
            if (ci == corridors.size() - 1 && c >= cols - EXIT_ROOM_WIDTH - 2) continue;

            // Find a surface (empty above solid)
            for (int r = top + h - 1; r >= top; r--) {
                if (map[r][c] == Constants.TILE_EMPTY
                    && r + 1 < rows && map[r + 1][c] == Constants.TILE_SOLID) {
                    map[r][c] = Constants.TILE_GOLD_BAR;
                    placed++;
                    break;
                }
            }
        }

        // Floating gold (mid-air, requires jumping)
        int floatTarget = 3 + level * 2;
        attempts = 0;
        while (floatTarget > 0 && attempts < floatTarget * 15) {
            attempts++;
            int ci = rng.nextInt(corridors.size());
            int top = corridors.get(ci)[0];
            int h = corridors.get(ci)[1];
            int r = top + rng.nextInt(h);
            int c = 3 + rng.nextInt(Math.max(1, cols - 6));

            if (ci == 0 && c <= SPAWN_ROOM_WIDTH + 1) continue;
            if (ci == corridors.size() - 1 && c >= cols - EXIT_ROOM_WIDTH - 2) continue;

            if (map[r][c] == Constants.TILE_EMPTY) {
                map[r][c] = Constants.TILE_GOLD_BAR;
                floatTarget--;
                placed++;
            }
        }
    }

    /**
     * Place diamonds in various positions inside corridors.
     */
    private void placeDiamonds(int[][] map, int count, List<int[]> corridors) {
        int placed = 0;
        int attempts = 0;

        while (placed < count && attempts < count * 25) {
            attempts++;
            int ci = rng.nextInt(corridors.size());
            int top = corridors.get(ci)[0];
            int h = corridors.get(ci)[1];
            int r = top + rng.nextInt(h);
            int c = 5 + rng.nextInt(Math.max(1, cols - 10));

            if (map[r][c] == Constants.TILE_EMPTY) {
                map[r][c] = Constants.TILE_DIAMOND;
                placed++;
            }
        }
    }

    /**
     * Place enemy spawns distributed across corridors on ground surfaces.
     * Avoids spawn room and exit room.
     */
    private void placeEnemySpawns(int[][] map, int count, List<int[]> corridors) {
        int placed = 0;
        int sectionW = Math.max(1, (cols - 10) / Math.max(count, 1));

        for (int i = 0; i < count; i++) {
            int ci = i % corridors.size();
            int top = corridors.get(ci)[0];
            int h = corridors.get(ci)[1];
            int baseCol = 6 + (i * sectionW) % (Math.max(1, cols - 12));

            // Skip spawn room (first corridor, left)
            if (ci == 0 && baseCol <= SPAWN_ROOM_WIDTH + 2) {
                baseCol = SPAWN_ROOM_WIDTH + 3;
            }
            // Skip exit room (last corridor, right)
            if (ci == corridors.size() - 1 && baseCol >= cols - EXIT_ROOM_WIDTH - 3) {
                baseCol = Math.max(2, cols - EXIT_ROOM_WIDTH - 5);
            }

            int attempts = 0;
            while (attempts < 100) {
                attempts++;
                int c = baseCol + rng.nextInt(Math.max(1, sectionW));
                c = Math.max(2, Math.min(c, cols - 4));

                // Enforce spawn/exit room avoidance
                if (ci == 0 && c <= SPAWN_ROOM_WIDTH + 1) continue;
                if (ci == corridors.size() - 1 && c >= cols - EXIT_ROOM_WIDTH - 2) continue;

                for (int r = top + h - 1; r >= top; r--) {
                    if (map[r][c] == Constants.TILE_EMPTY
                        && r + 1 < rows && map[r + 1][c] == Constants.TILE_SOLID) {
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
