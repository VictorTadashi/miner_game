package dungeonexplorer.ui;

import dungeonexplorer.engine.Difficulty;
import dungeonexplorer.entities.Player;
import dungeonexplorer.map.GameMap;
import dungeonexplorer.util.Constants;
import java.awt.*;

/**
 * Heads-Up Display showing score, lives, level, and gold bar progress.
 */
public class HUD {
    private final Font mainFont;
    private final Font smallFont;

    public HUD() {
        mainFont = new Font("Monospaced", Font.BOLD, 16);
        smallFont = new Font("Monospaced", Font.PLAIN, 12);
    }

    public void render(Graphics2D g, Player player, GameMap map, int level,
                       int windowWidth, Difficulty difficulty) {
        if (player == null || map == null) return;

        // Background
        g.setColor(Constants.COLOR_HUD_BG);
        g.fillRect(0, 0, windowWidth, Constants.HUD_HEIGHT);

        // Bottom border
        g.setColor(new Color(100, 75, 45));
        g.drawLine(0, Constants.HUD_HEIGHT - 1, windowWidth, Constants.HUD_HEIGHT - 1);

        g.setFont(mainFont);

        // Score
        g.setColor(Constants.COLOR_TEXT);
        g.drawString("SCORE", 10, 18);
        g.setColor(Constants.COLOR_GOLD);
        g.drawString(String.format("%06d", player.getScore()), 10, 38);

        // Lives (helmet icons)
        g.setColor(Constants.COLOR_TEXT);
        g.drawString("LIVES", 160, 18);
        for (int i = 0; i < player.getLives(); i++) {
            Player.drawMiniHelmet(g, 160 + i * 22, 24);
        }

        // Level
        g.setColor(Constants.COLOR_TEXT);
        g.drawString("DEPTH", 300, 18);
        g.setColor(new Color(200, 180, 140));
        g.drawString(String.valueOf(level), 300, 38);

        // Gold bar progress
        g.setColor(Constants.COLOR_TEXT);
        g.drawString("GOLD", 400, 18);
        int collected = map.getCollectedGold();
        int total = map.getTotalGold();
        Color progressColor = map.isExitOpen() ? new Color(100, 255, 100) : Constants.COLOR_GOLD;
        g.setColor(progressColor);
        g.drawString(collected + "/" + total, 400, 38);

        // Progress bar
        int barX = 460;
        int barY = 28;
        int barW = 120;
        int barH = 10;
        g.setColor(new Color(50, 40, 30));
        g.fillRect(barX, barY, barW, barH);
        if (total > 0) {
            int fillW = (int) ((double) collected / total * barW);
            g.setColor(progressColor);
            g.fillRect(barX, barY, fillW, barH);
        }
        g.setColor(new Color(80, 65, 45));
        g.drawRect(barX, barY, barW, barH);

        // Exit status
        if (map.isExitOpen()) {
            g.setFont(smallFont);
            g.setColor(new Color(100, 255, 100));
            String exitText = ">> EXIT OPEN! <<";
            int textWidth = g.getFontMetrics().stringWidth(exitText);
            g.drawString(exitText, windowWidth - textWidth - 10, 20);
        }

        // Difficulty indicator
        g.setFont(smallFont);
        g.setColor(new Color(150, 140, 120));
        g.drawString(difficulty.displayName, windowWidth - 80, 42);
    }
}
