package dungeonexplorer.engine;

import dungeonexplorer.entities.Enemy;
import dungeonexplorer.entities.Player;
import dungeonexplorer.ui.HUD;
import dungeonexplorer.ui.MapRenderer;
import dungeonexplorer.ui.ScreenOverlay;
import dungeonexplorer.util.Constants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * Main game panel for the side-scrolling platformer.
 * Handles rendering with camera viewport and continuous key input.
 */
public class GamePanel extends JPanel implements KeyListener {
    private final GameState gameState;
    private MapRenderer mapRenderer;
    private final HUD hud;
    private final ScreenOverlay overlay;

    // Double buffering
    private BufferedImage buffer;
    private int currentWidth;
    private int currentHeight;

    public GamePanel(GameState gameState) {
        this.gameState = gameState;
        this.mapRenderer = new MapRenderer();
        this.hud = new HUD();
        this.overlay = new ScreenOverlay();

        this.currentWidth = Constants.VIEWPORT_WIDTH;
        this.currentHeight = Constants.VIEWPORT_HEIGHT + Constants.HUD_HEIGHT;

        setPreferredSize(new Dimension(currentWidth, currentHeight));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        buffer = new BufferedImage(currentWidth, currentHeight, BufferedImage.TYPE_INT_ARGB);
    }

    public void resizeForDifficulty(Difficulty diff) {
        currentWidth = diff.windowWidth();
        currentHeight = diff.windowHeight();
        setPreferredSize(new Dimension(currentWidth, currentHeight));
        buffer = new BufferedImage(currentWidth, currentHeight, BufferedImage.TYPE_INT_ARGB);
        mapRenderer = new MapRenderer();
        revalidate();
    }

    public void updateRenderers() {
        mapRenderer.update();
        overlay.update();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = buffer.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Clear
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, currentWidth, currentHeight);

        GameState.State state = gameState.getState();

        if (state == GameState.State.TITLE) {
            overlay.renderTitleScreen(g2, currentWidth, currentHeight);
        } else if (state == GameState.State.DIFFICULTY_SELECT) {
            overlay.renderDifficultySelect(g2, gameState.getSelectedDifficultyIndex(),
                                           currentWidth, currentHeight);
        } else {
            // Render HUD at top
            hud.render(g2, gameState.getPlayer(), gameState.getMap(), gameState.getLevel(),
                       currentWidth, gameState.getDifficulty());

            // Set clip for game viewport (below HUD)
            g2.setClip(0, Constants.HUD_HEIGHT, Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT);
            g2.translate(0, Constants.HUD_HEIGHT);

            double camX = gameState.getCameraX();
            double camY = gameState.getCameraY();

            // Render map with camera offset
            mapRenderer.render(g2, gameState.getMap(), camX, camY,
                              Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT);

            // Render enemies
            for (Enemy enemy : gameState.getEnemies()) {
                enemy.render(g2, camX, camY);
            }

            // Render player
            if (gameState.getPlayer() != null) {
                gameState.getPlayer().render(g2, camX, camY);
            }

            // Reset transform and clip
            g2.translate(0, -Constants.HUD_HEIGHT);
            g2.setClip(null);

            // Overlays (full screen)
            switch (state) {
                case GET_READY:
                    overlay.renderGetReady(g2, gameState.getLevel(), currentWidth, currentHeight);
                    break;
                case PAUSED:
                    overlay.renderPause(g2, currentWidth, currentHeight);
                    break;
                case GAME_OVER:
                    overlay.renderGameOver(g2, gameState.getPlayer().getScore(),
                                          currentWidth, currentHeight);
                    break;
                case LEVEL_COMPLETE:
                    overlay.renderLevelComplete(g2, gameState.getLevel(),
                                               gameState.getPlayer().getScore(),
                                               currentWidth, currentHeight);
                    break;
            }
        }

        g2.dispose();
        g.drawImage(buffer, 0, 0, null);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        GameState.State state = gameState.getState();

        switch (state) {
            case TITLE:
                if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                    gameState.startGame();
                }
                break;

            case DIFFICULTY_SELECT:
                if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) {
                    gameState.moveDifficultySelection(-1);
                } else if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) {
                    gameState.moveDifficultySelection(1);
                } else if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                    gameState.selectDifficulty(gameState.getSelectedDifficultyIndex());
                } else if (key == KeyEvent.VK_1) {
                    gameState.selectDifficulty(0);
                } else if (key == KeyEvent.VK_2) {
                    gameState.selectDifficulty(1);
                } else if (key == KeyEvent.VK_3) {
                    gameState.selectDifficulty(2);
                } else if (key == KeyEvent.VK_ESCAPE) {
                    gameState.setState(GameState.State.TITLE);
                }
                break;

            case PLAYING:
                handlePlayingKeyPress(key);
                break;

            case PAUSED:
                if (key == KeyEvent.VK_P || key == KeyEvent.VK_ESCAPE) {
                    gameState.setState(GameState.State.PLAYING);
                }
                break;

            case GAME_OVER:
                if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                    gameState.restartGame();
                }
                break;

            case LEVEL_COMPLETE:
                if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                    gameState.nextLevel();
                }
                break;
        }
    }

    private void handlePlayingKeyPress(int key) {
        Player player = gameState.getPlayer();
        if (player == null) return;

        switch (key) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                player.setMoveLeft(true);
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                player.setMoveRight(true);
                break;
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                player.setJumpPressed(true);
                break;
            case KeyEvent.VK_Z:
            case KeyEvent.VK_J:
            case KeyEvent.VK_X:
                player.startAttack();
                break;
            case KeyEvent.VK_P:
                gameState.setState(GameState.State.PAUSED);
                break;
            case KeyEvent.VK_R:
                gameState.restartGame();
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        Player player = gameState.getPlayer();
        if (player == null) return;

        switch (key) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                player.setMoveLeft(false);
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                player.setMoveRight(false);
                break;
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                player.setJumpPressed(false);
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}
