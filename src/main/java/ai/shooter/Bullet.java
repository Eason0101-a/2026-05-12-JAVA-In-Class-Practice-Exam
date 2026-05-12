package ai.shooter;

import java.awt.Rectangle;

public final class Bullet {
    private final int width;
    private final int height;
    private final int speed;
    private int x;
    private int y;
    private boolean active = true;

    public Bullet(int x, int y, int width, int height, int speed) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.speed = speed;
    }

    public void update() {
        y -= speed;
        if (y + height < 0) {
            active = false;
        }
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        active = false;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}