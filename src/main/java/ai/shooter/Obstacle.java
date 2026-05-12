package ai.shooter;

import java.awt.Rectangle;
import java.util.Random;

public final class Obstacle {
    private int col;
    private double y;
    private final int cellSize;

    public Obstacle(int col, double y, int cellSize) {
        this.col = col;
        this.y = y;
        this.cellSize = cellSize;
    }

    public int getCol() {
        return col;
    }

    public double getY() {
        return y;
    }

    public int getRow() {
        return (int) Math.floor((y + cellSize * 0.5) / cellSize);
    }

    public Rectangle getBounds() {
        int x = col * cellSize;
        int py = (int) Math.round(y);
        return new Rectangle(x + 4, py + 3, cellSize - 8, cellSize - 6);
    }

    public void update(double speed, int cols, int gameHeight, Random random) {
        y += speed;
        if (y >= gameHeight) {
            respawn(cols, gameHeight, random);
        }
    }

    public void respawn(int cols, int gameHeight, Random random) {
        y = -cellSize - random.nextInt(Math.max(1, gameHeight / 3));
        col = random.nextInt(cols);
    }
}
