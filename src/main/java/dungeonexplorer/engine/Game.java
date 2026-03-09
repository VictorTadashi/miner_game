package dungeonexplorer.engine;

import dungeonexplorer.util.Constants;

import javax.swing.*;
import java.awt.*;

/**
 * Main game class - creates window and runs the game loop.
 * Handles window resizing when difficulty changes.
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
        frame.setResizable(false);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
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

                // Check if difficulty changed and resize window
                if (gameState.consumeDifficultyChanged()) {
                    SwingUtilities.invokeLater(this::resizeWindow);
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

    private void resizeWindow() {
        Difficulty diff = gameState.getDifficulty();
        panel.resizeForDifficulty(diff);
        frame.pack();
        frame.setLocationRelativeTo(null);
        panel.requestFocusInWindow();
    }
}
