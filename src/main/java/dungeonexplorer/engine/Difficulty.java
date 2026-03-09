package dungeonexplorer.engine;

/**
 * Game difficulty settings - wide platformer maps.
 */
public enum Difficulty {
    EASY(
        "FACIL", "Mapa curto, poucos monstros lentos",
        60, 18, 1.5, 3, 4
    ),
    MEDIUM(
        "MEDIO", "Mapa medio, monstros normais",
        80, 22, 2.0, 5, 5
    ),
    HARD(
        "DIFICIL", "Mapa longo, muitos monstros rapidos",
        100, 26, 2.5, 7, 6
    );

    public final String displayName;
    public final String description;
    public final int mapCols;
    public final int mapRows;
    public final double enemySpeed;
    public final int enemyCount;
    public final int diamondCount;

    Difficulty(String displayName, String description,
              int mapCols, int mapRows,
              double enemySpeed, int enemyCount, int diamondCount) {
        this.displayName = displayName;
        this.description = description;
        this.mapCols = mapCols;
        this.mapRows = mapRows;
        this.enemySpeed = enemySpeed;
        this.enemyCount = enemyCount;
        this.diamondCount = diamondCount;
    }

    public int windowWidth() { return 800; }
    public int windowHeight() { return 608 + 50; } // viewport + HUD
}
