package ai.shooter;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PathFinder {
    private static final int[][] OFFSETS = {
            {-1, 0},
            {1, 0},
            {0, -1},
            {0, 1}
    };

    public List<Point> findPath(GridMap map, int startCol, int startRow, int goalCol, int goalRow) {
        if (!map.isInside(startCol, startRow) || !map.isInside(goalCol, goalRow)) {
            return Collections.emptyList();
        }

        if (startCol == goalCol && startRow == goalRow) {
            return List.of(new Point(startCol, startRow));
        }

        boolean[][] visited = new boolean[map.getRows()][map.getCols()];
        ArrayDeque<Node> queue = new ArrayDeque<>();
        queue.add(new Node(startRow, startCol, null));
        visited[startRow][startCol] = true;

        while (!queue.isEmpty()) {
            Node current = queue.removeFirst();
            if (current.col == goalCol && current.row == goalRow) {
                return buildPath(current);
            }

            for (int[] offset : OFFSETS) {
                int nextRow = current.row + offset[0];
                int nextCol = current.col + offset[1];

                if (!map.isWalkable(nextCol, nextRow) || visited[nextRow][nextCol]) {
                    continue;
                }

                visited[nextRow][nextCol] = true;
                queue.addLast(new Node(nextRow, nextCol, current));
            }
        }

        return Collections.emptyList();
    }

    private List<Point> buildPath(Node target) {
        List<Point> path = new ArrayList<>();
        Node current = target;
        while (current != null) {
            path.add(new Point(current.col, current.row));
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }
}