package ai.shooter;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class GridMap {
    private final int cols;
    private final int rows;
    private final int cellSize;
    private final List<Point> obstacleCells = new ArrayList<>();
    private final boolean[][] blocked;

    public GridMap(int cols, int rows, int cellSize) {
        this.cols = cols;
        this.rows = rows;
        this.cellSize = cellSize;
        this.blocked = new boolean[rows][cols];
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    public int getCellSize() {
        return cellSize;
    }

    public void generateObstacles(Random random, int count, Set<Point> reservedCells) {
        obstacleCells.clear();
        clearBlocked();

        int attempts = 0;
        int maxAttempts = count * 20;
        Set<Point> reserved = new HashSet<>(reservedCells);
        while (obstacleCells.size() < count && attempts < maxAttempts) {
            int col = random.nextInt(cols);
            int row = random.nextInt(rows);
            Point cell = new Point(col, row);
            attempts++;

            if (reserved.contains(cell) || blocked[row][col]) {
                continue;
            }

            blocked[row][col] = true;
            obstacleCells.add(cell);
        }
    }

    public void clearBlocked() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                blocked[row][col] = false;
            }
        }
    }

    public void setBlocked(int col, int row, boolean value) {
        if (!isInside(col, row)) {
            return;
        }
        blocked[row][col] = value;
    }

    public boolean isInside(int col, int row) {
        return col >= 0 && col < cols && row >= 0 && row < rows;
    }

    public boolean isWalkable(int col, int row) {
        return isInside(col, row) && !blocked[row][col];
    }

    public List<Point> getObstacleCells() {
        return Collections.unmodifiableList(obstacleCells);
    }

    public List<Rectangle> getObstacleBounds() {
        List<Rectangle> rectangles = new ArrayList<>(obstacleCells.size());
        for (Point cell : obstacleCells) {
            rectangles.add(new Rectangle(cell.x * cellSize, cell.y * cellSize, cellSize, cellSize));
        }
        return rectangles;
    }

    public int toPixelX(int col) {
        return col * cellSize;
    }

    public int toPixelY(int row) {
        return row * cellSize;
    }

    public int toCenterX(int col) {
        return toPixelX(col) + cellSize / 2;
    }

    public int toCenterY(int row) {
        return toPixelY(row) + cellSize / 2;
    }

    public int toCol(int x) {
        return Math.max(0, Math.min(cols - 1, x / cellSize));
    }

    public int toRow(int y) {
        return Math.max(0, Math.min(rows - 1, y / cellSize));
    }
}