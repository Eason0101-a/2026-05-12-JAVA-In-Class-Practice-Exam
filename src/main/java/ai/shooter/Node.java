package ai.shooter;

final class Node {
    final int row;
    final int col;
    final Node parent;

    Node(int row, int col, Node parent) {
        this.row = row;
        this.col = col;
        this.parent = parent;
    }
}