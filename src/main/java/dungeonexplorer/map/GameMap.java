package dungeonexplorer.map;

import dungeonexplorer.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the current platformer map state and provides tile query methods.
 * Uses LevelGenerator instead of MazeGenerator for side-scrolling levels.
 */
public class GameMap {
    private int[][] tiles;
    private final int rows;
    private final int cols;
    private int totalGold;
    private int collectedGold;
    private int playerSpawnRow, playerSpawnCol;
    private int exitRow, exitCol;
    private boolean exitOpen;

    // Store all enemy spawn positions
    private final List<int[]> enemySpawns = new ArrayList<>();

    public GameMap(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
    }

    public void loadLevel(int level, int enemyCount, int diamondCount) {
        LevelGenerator generator = new LevelGenerator(rows, cols);
        this.tiles = generator.generate(level, enemyCount, diamondCount);
        this.collectedGold = 0;
        this.totalGold = 0;
        this.exitOpen = false;
        this.enemySpawns.clear();

        // Scan for special tiles
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                switch (tiles[r][c]) {
                    case Constants.TILE_GOLD_BAR:
                        totalGold++;
                        break;
                    case Constants.TILE_DIAMOND:
                        totalGold++;
                        break;
                    case Constants.TILE_PLAYER_SPAWN:
                        playerSpawnRow = r;
                        playerSpawnCol = c;
                        tiles[r][c] = Constants.TILE_EMPTY; // clear spawn marker
                        break;
                    case Constants.TILE_EXIT:
                        exitRow = r;
                        exitCol = c;
                        break;
                    case Constants.TILE_ENEMY_SPAWN:
                        enemySpawns.add(new int[]{r, c});
                        tiles[r][c] = Constants.TILE_EMPTY; // clear spawn marker
                        break;
                }
            }
        }
    }

    /** Check if a tile is solid (blocks entity movement). */
    public boolean isSolid(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return true; // Out of bounds treated as solid
        }
        return tiles[row][col] == Constants.TILE_SOLID;
    }

    public int getTile(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return Constants.TILE_SOLID;
        }
        return tiles[row][col];
    }

    public void setTile(int row, int col, int type) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            tiles[row][col] = type;
        }
    }

    public void collectGold() {
        collectedGold++;
        if (collectedGold >= totalGold) {
            exitOpen = true;
        }
    }

    public boolean isExitOpen() {
        return exitOpen;
    }

    /** Get pixel width of entire map. */
    public int getPixelWidth() { return cols * Constants.TILE_SIZE; }
    /** Get pixel height of entire map. */
    public int getPixelHeight() { return rows * Constants.TILE_SIZE; }

    // Getters
    public int getTotalGold() { return totalGold; }
    public int getCollectedGold() { return collectedGold; }
    public int getPlayerSpawnRow() { return playerSpawnRow; }
    public int getPlayerSpawnCol() { return playerSpawnCol; }
    public int getExitRow() { return exitRow; }
    public int getExitCol() { return exitCol; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int[][] getTiles() { return tiles; }
    public List<int[]> getEnemySpawns() { return enemySpawns; }
}
