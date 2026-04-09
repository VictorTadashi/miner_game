package dungeonexplorer.engine;

import dungeonexplorer.util.Constants;

import javax.swing.*;
import java.awt.*;

/**
 * Main game class - creates fullscreen window and runs the game loop.
 * The game renders at an internal resolution and scales to fill the screen.
 */
public class Game {
    private JFrame frame;
    private GamePanel panel;
    private GameState gameState;
    private boolean running;

    public Game() {
        gameState = new GameState();
        panel = new GamePanel(gameState);

        frame = new JFrame("Cave Miner");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(true);
        frame.add(panel);

        // Fullscreen: borderless maximized window
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    public void start() {
        frame.setVisible(true);
        panel.requestFocusInWindow();
        running = true;

        // Game loop in a separate thread
        Thread gameThread = new Thread(this::gameLoop, "GameLoop");
        gameThread.setDaemon(true);
        gameThread.start();
    }

    private void gameLoop() {
        long lastTime = System.nanoTime();
        long accumulator = 0;

        while (running) {
            long now = System.nanoTime();
            long elapsed = now - lastTime;
            lastTime = now;
            accumulator += elapsed;

            // Fixed time step updates
            while (accumulator >= Constants.FRAME_TIME_NS) {
                gameState.update();
                panel.updateRenderers();

                // Check if difficulty changed
                if (gameState.consumeDifficultyChanged()) {
                    SwingUtilities.invokeLater(this::onDifficultyChanged);
                }

                accumulator -= Constants.FRAME_TIME_NS;
            }

            // Render
            panel.repaint();

            // Sleep to prevent excessive CPU usage
            long sleepTime = (Constants.FRAME_TIME_NS - (System.nanoTime() - lastTime)) / 1_000_000;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Called when difficulty changes. In fullscreen mode we just
     * refresh the internal buffer — no window resize needed.
     */
    private void onDifficultyChanged() {
        Difficulty diff = gameState.getDifficulty();
        panel.resizeForDifficulty(diff);
        panel.requestFocusInWindow();
    }
}
