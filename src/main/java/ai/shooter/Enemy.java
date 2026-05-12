package ai.shooter;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;

public final class Enemy {
    private final int size;
    private int x;
    private int y;
    private final double speed;
    private long lastShotMillis = 0L;
    private List<Point> pathCells = Collections.emptyList();
    private int pathIndex;
    private int repathCooldown;

    public Enemy(int x, int y, int size, double speed) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.speed = speed;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getSize() {
        return size;
    }

    public int getCenterX() {
        return x + size / 2;
    }

    public int getCenterY() {
        return y + size / 2;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, size, size);
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void updateTowardsPlayer(Player player, GridMap map, PathFinder pathFinder) {
        int cols = map.getCols();
        int rows = map.getRows();
        int cell = map.getCellSize();

        int myCol = clamp((x + size / 2) / cell, 0, cols - 1);
        int myRow = clamp((y + size / 2) / cell, 0, rows - 1);
        int playerCol = clamp(player.getCenterX() / cell, 0, cols - 1);
        int playerRow = clamp(player.getCenterY() / cell, 0, rows - 1);

        if (y < cell / 2) {
            y += Math.max(1, (int) Math.round(speed));
            return;
        }

        if (repathCooldown <= 0 || pathIndex >= pathCells.size()) {
            pathCells = pathFinder.findPath(map, myCol, myRow, playerCol, playerRow);
            pathIndex = 0;
            repathCooldown = 10;
        } else {
            repathCooldown--;
        }

        if (!pathCells.isEmpty() && pathIndex < pathCells.size()) {
            Point target = pathCells.get(pathIndex);
            int targetX = target.x * cell + cell / 2 - size / 2;
            int targetY = target.y * cell + cell / 2 - size / 2;

            double dx = targetX - x;
            double dy = targetY - y;
            double minDown = Math.max(1.0, speed * 0.9);
            dy = Math.max(dy, minDown);
            double maxSide = dy * 0.75;
            dx = clampDouble(dx, -maxSide, maxSide);
            double dist = Math.hypot(dx, dy);

            if (dist < speed + 0.5) {
                int stepX = targetX - x;
                if (Math.abs(stepX) > 1) {
                    x += stepX > 0 ? 1 : -1;
                } else {
                    x = targetX;
                }
                y += Math.max(1, (int) Math.round(minDown));
                pathIndex++;
            } else if (dist > 0) {
                int stepY = Math.max(1, (int) Math.round(speed * dy / dist));
                int stepX = (int) Math.round(speed * dx / dist);
                if (stepX == 0 && Math.abs(dx) > 0.8) {
                    stepX = dx > 0 ? 1 : -1;
                }
                if (Math.abs(stepX) > stepY) {
                    stepX = stepY * Integer.signum(stepX);
                }
                x += stepX;
                y += stepY;
            }
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public long getLastShotMillis() {
        return lastShotMillis;
    }

    public void setLastShotMillis(long millis) {
        this.lastShotMillis = millis;
    }
}