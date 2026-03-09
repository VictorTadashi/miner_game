package dungeonexplorer.ui;

import dungeonexplorer.engine.Difficulty;
import dungeonexplorer.util.Constants;
import java.awt.*;

/**
 * Renders overlay screens: title, difficulty select, game over, level complete, pause.
 * Updated for side-scrolling platformer controls.
 */
public class ScreenOverlay {
    private final Font titleFont;
    private final Font subtitleFont;
    private final Font textFont;
    private final Font optionFont;
    private int animTimer = 0;

    public ScreenOverlay() {
        titleFont = new Font("Monospaced", Font.BOLD, 36);
        subtitleFont = new Font("Monospaced", Font.BOLD, 18);
        textFont = new Font("Monospaced", Font.PLAIN, 14);
        optionFont = new Font("Monospaced", Font.BOLD, 20);
    }

    public void update() {
        animTimer++;
    }

    public void renderTitleScreen(Graphics2D g, int width, int height) {
        // Dark cave overlay
        g.setColor(new Color(10, 8, 5, 220));
        g.fillRect(0, 0, width, height);

        int centerX = width / 2;
        int centerY = height / 2;

        // Title with golden glow
        float pulse = 0.7f + 0.3f * (float) Math.sin(animTimer * 0.05);
        int glowAlpha = (int) (50 * pulse);
        g.setColor(new Color(255, 200, 50, glowAlpha));
        g.setFont(titleFont);
        String title = "CAVE MINER";
        int tw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, centerX - tw / 2 - 1, centerY - 60 - 1);
        g.drawString(title, centerX - tw / 2 + 1, centerY - 60 + 1);

        g.setColor(Constants.COLOR_HELMET);
        g.drawString(title, centerX - tw / 2, centerY - 60);

        // Subtitle
        g.setFont(subtitleFont);
        g.setColor(Constants.COLOR_GOLD);
        String sub = "Explore the Cave Depths";
        int sw = g.getFontMetrics().stringWidth(sub);
        g.drawString(sub, centerX - sw / 2, centerY - 20);

        // Instructions for platformer
        g.setFont(textFont);
        g.setColor(Constants.COLOR_TEXT);
        String[] instructions = {
            "Collect all gold bars to unlock the exit",
            "Use your pickaxe to fight cave monsters!",
            "Each monster takes 3 hits to defeat",
            "",
            "A/D or LEFT/RIGHT - Walk",
            "SPACE/W/UP - Jump",
            "Z/J/X - Attack with pickaxe",
            "P - Pause  |  R - Restart",
        };

        int ly = centerY + 15;
        for (String line : instructions) {
            int lw = g.getFontMetrics().stringWidth(line);
            g.drawString(line, centerX - lw / 2, ly);
            ly += 20;
        }

        // Blinking "Press ENTER"
        g.setFont(subtitleFont);
        float blink = (float) Math.sin(animTimer * 0.08);
        if (blink > 0) {
            g.setColor(Constants.COLOR_HELMET);
            String start = "Press ENTER to start";
            int stw = g.getFontMetrics().stringWidth(start);
            g.drawString(start, centerX - stw / 2, ly + 15);
        }

        // Animated full-body miner icon
        drawTitleMiner(g, centerX, centerY - 115);
    }

    private void drawTitleMiner(Graphics2D g, int cx, int baseY) {
        float bounce = (float) Math.sin(animTimer * 0.08) * 4;
        int by = (int) bounce;
        int x = cx - 14;
        int y = baseY + by;

        // Legs
        g.setColor(new Color(60, 50, 40));
        g.fillRoundRect(x + 4, y + 28, 7, 8, 2, 2);
        g.fillRoundRect(x + 17, y + 28, 7, 8, 2, 2);

        // Body (overalls)
        g.setColor(Constants.COLOR_PLAYER_DARK);
        g.fillRoundRect(x + 3, y + 14, 22, 16, 4, 4);

        // Belt
        g.setColor(new Color(120, 90, 50));
        g.fillRect(x + 4, y + 22, 20, 3);

        // Head
        g.setColor(Constants.COLOR_SKIN);
        g.fillOval(x + 5, y + 4, 18, 14);

        // Helmet
        g.setColor(Constants.COLOR_HELMET);
        g.fillArc(x + 3, y, 22, 12, 0, 180);
        g.fillRect(x + 2, y + 5, 24, 3);

        // Lamp
        g.setColor(Constants.COLOR_HELMET_LIGHT);
        g.fillOval(x + 11, y + 1, 6, 4);

        // Light beam
        g.setColor(new Color(255, 255, 180, 50));
        int[] beamX = {x + 13, x + 9, x + 19, x + 15};
        int[] beamY = {y + 1, y - 14, y - 14, y + 1};
        g.fillPolygon(beamX, beamY, 4);

        // Eyes
        g.setColor(Color.WHITE);
        g.fillOval(x + 10, y + 10, 4, 4);
        g.fillOval(x + 16, y + 10, 4, 4);
        g.setColor(new Color(40, 30, 20));
        g.fillOval(x + 11, y + 11, 2, 2);
        g.fillOval(x + 17, y + 11, 2, 2);

        // Pickaxe at side
        g.setColor(Constants.COLOR_PICKAXE_HANDLE);
        g.fillRect(x + 24, y + 10, 3, 18);
        g.setColor(Constants.COLOR_PICKAXE_HEAD);
        int[] px = {x + 25, x + 33, x + 25};
        int[] py = {y + 8, y + 11, y + 14};
        g.fillPolygon(px, py, 3);
    }

    public void renderDifficultySelect(Graphics2D g, int selectedIndex, int width, int height) {
        g.setColor(new Color(10, 8, 5, 230));
        g.fillRect(0, 0, width, height);

        int centerX = width / 2;
        int centerY = height / 2;

        g.setFont(titleFont);
        g.setColor(Constants.COLOR_HELMET);
        String title = "SELECT DIFFICULTY";
        int tw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, centerX - tw / 2, centerY - 100);

        Difficulty[] diffs = Difficulty.values();
        int optionHeight = 70;
        int startY = centerY - 40;

        for (int i = 0; i < diffs.length; i++) {
            Difficulty d = diffs[i];
            int optY = startY + i * optionHeight;
            boolean selected = (i == selectedIndex);

            int boxWidth = 320;
            int boxX = centerX - boxWidth / 2;
            int boxY = optY - 8;
            int boxH = optionHeight - 10;

            if (selected) {
                float pulse2 = 0.6f + 0.4f * (float) Math.sin(animTimer * 0.1);
                int ga = (int) (30 * pulse2);
                g.setColor(new Color(255, 200, 50, ga));
                g.fillRoundRect(boxX - 4, boxY - 4, boxWidth + 8, boxH + 8, 12, 12);

                g.setColor(new Color(80, 60, 30));
                g.fillRoundRect(boxX, boxY, boxWidth, boxH, 8, 8);

                g.setColor(Constants.COLOR_HELMET);
                g.drawRoundRect(boxX, boxY, boxWidth, boxH, 8, 8);
            } else {
                g.setColor(new Color(40, 32, 22));
                g.fillRoundRect(boxX, boxY, boxWidth, boxH, 8, 8);
                g.setColor(new Color(70, 55, 35));
                g.drawRoundRect(boxX, boxY, boxWidth, boxH, 8, 8);
            }

            g.setFont(optionFont);
            g.setColor(selected ? Constants.COLOR_HELMET : Constants.COLOR_TEXT);
            String name = d.displayName;
            int nw = g.getFontMetrics().stringWidth(name);
            g.drawString(name, centerX - nw / 2, optY + 14);

            g.setFont(textFont);
            g.setColor(selected ? new Color(220, 200, 160) : new Color(150, 140, 120));
            String desc = d.description;
            int dw = g.getFontMetrics().stringWidth(desc);
            g.drawString(desc, centerX - dw / 2, optY + 36);

            String stats = d.mapCols + "x" + d.mapRows + " | " +
                           d.enemyCount + " monsters | Speed " + d.enemySpeed;
            int ssw = g.getFontMetrics().stringWidth(stats);
            g.setColor(selected ? new Color(180, 160, 120) : new Color(120, 110, 95));
            g.drawString(stats, centerX - ssw / 2, optY + 52);

            if (selected) {
                g.setColor(Constants.COLOR_HELMET);
                int arrowX = boxX - 20;
                int arrowY = optY + 18;
                int[] axPts = {arrowX, arrowX + 10, arrowX};
                int[] ayPts = {arrowY - 6, arrowY, arrowY + 6};
                g.fillPolygon(axPts, ayPts, 3);
            }
        }

        g.setFont(textFont);
        g.setColor(new Color(150, 140, 120));
        String hint = "UP/DOWN to select | ENTER to confirm | ESC back";
        int hw = g.getFontMetrics().stringWidth(hint);
        g.drawString(hint, centerX - hw / 2, startY + diffs.length * optionHeight + 30);
    }

    public void renderGameOver(Graphics2D g, int score, int width, int height) {
        g.setColor(new Color(10, 5, 0, 200));
        g.fillRect(0, 0, width, height);

        int centerX = width / 2;
        int centerY = height / 2;

        g.setFont(titleFont);
        g.setColor(new Color(200, 50, 50));
        String text = "CAVE IN!";
        int tw2 = g.getFontMetrics().stringWidth(text);
        g.drawString(text, centerX - tw2 / 2, centerY - 30);

        g.setFont(subtitleFont);
        g.setColor(Constants.COLOR_TEXT);
        String scoreText = "Final Score: " + score;
        int sw2 = g.getFontMetrics().stringWidth(scoreText);
        g.drawString(scoreText, centerX - sw2 / 2, centerY + 10);

        g.setFont(textFont);
        g.setColor(Constants.COLOR_GOLD);
        String restart = "Press ENTER to try again";
        int rw = g.getFontMetrics().stringWidth(restart);
        float blink = (float) Math.sin(animTimer * 0.08);
        if (blink > 0) {
            g.drawString(restart, centerX - rw / 2, centerY + 50);
        }
    }

    public void renderLevelComplete(Graphics2D g, int level, int score, int width, int height) {
        g.setColor(new Color(10, 8, 5, 180));
        g.fillRect(0, 0, width, height);

        int centerX = width / 2;
        int centerY = height / 2;

        g.setFont(titleFont);
        g.setColor(new Color(100, 255, 100));
        String text = "DEPTH " + level + " CLEARED!";
        int tw2 = g.getFontMetrics().stringWidth(text);
        g.drawString(text, centerX - tw2 / 2, centerY - 30);

        g.setFont(subtitleFont);
        g.setColor(Constants.COLOR_TEXT);
        String scoreText = "Score: " + score;
        int sw2 = g.getFontMetrics().stringWidth(scoreText);
        g.drawString(scoreText, centerX - sw2 / 2, centerY + 10);

        g.setFont(textFont);
        g.setColor(Constants.COLOR_GOLD);
        String next = "Press ENTER to go deeper";
        int nw = g.getFontMetrics().stringWidth(next);
        float blink = (float) Math.sin(animTimer * 0.08);
        if (blink > 0) {
            g.drawString(next, centerX - nw / 2, centerY + 50);
        }
    }

    public void renderPause(Graphics2D g, int width, int height) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, width, height);

        int centerX = width / 2;
        int centerY = height / 2;

        g.setFont(titleFont);
        g.setColor(Constants.COLOR_TEXT);
        String text = "PAUSED";
        int tw2 = g.getFontMetrics().stringWidth(text);
        g.drawString(text, centerX - tw2 / 2, centerY);

        g.setFont(textFont);
        g.setColor(Constants.COLOR_GOLD);
        String resume = "Press P to resume";
        int rw = g.getFontMetrics().stringWidth(resume);
        g.drawString(resume, centerX - rw / 2, centerY + 30);
    }

    public void renderGetReady(Graphics2D g, int level, int width, int height) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, width, height);

        int centerX = width / 2;
        int centerY = height / 2;

        g.setFont(titleFont);
        g.setColor(Constants.COLOR_HELMET);
        String text = "DEPTH " + level;
        int tw2 = g.getFontMetrics().stringWidth(text);
        g.drawString(text, centerX - tw2 / 2, centerY - 10);

        g.setFont(subtitleFont);
        g.setColor(Constants.COLOR_TEXT);
        String ready = "DESCENDING...";
        int rw = g.getFontMetrics().stringWidth(ready);
        g.drawString(ready, centerX - rw / 2, centerY + 25);
    }
}
