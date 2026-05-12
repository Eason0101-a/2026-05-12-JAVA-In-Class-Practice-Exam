package ai.shooter;

import java.awt.Rectangle;

public final class EnemyBullet {
    private final int width;
    private final int height;
    private final double speed;
    private double preciseX;
    private double preciseY;
    private double vx;
    private double vy;
    private boolean active = true;

    public EnemyBullet(int x, int y, int width, int height, double speed, double vx, double vy) {
        this.preciseX = x;
        this.preciseY = y;
        this.width = width;
        this.height = height;
        this.speed = speed;
        this.vx = vx;
        this.vy = vy;
    }

    public static EnemyBullet createHoming(int x, int y, Player player) {
        double dx = player.getCenterX() - x;
        double dy = player.getCenterY() - y;
        double dist = Math.max(1.0, Math.hypot(dx, dy));
        double speed = 4.6;
        double vy = Math.max(speed * 0.85, speed * dy / dist);
        double vx = speed * dx / dist;
        double maxSide = vy * 0.7;
        vx = Math.max(-maxSide, Math.min(maxSide, vx));
        return new EnemyBullet(x, y, 8, 14, speed, vx, vy);
    }

    public void update(Player player, int playWidth, int playHeight) {
        if (!active) {
            return;
        }

        double dx = player.getCenterX() - preciseX;
        double dy = player.getCenterY() - preciseY;
        double dist = Math.max(1.0, Math.hypot(dx, dy));
        double desiredVy = Math.max(speed * 0.85, speed * dy / dist);
        double desiredVx = speed * dx / dist;
        double maxSide = desiredVy * 0.7;
        desiredVx = Math.max(-maxSide, Math.min(maxSide, desiredVx));

        vx = vx * 0.9 + desiredVx * 0.1;
        vy = vy * 0.9 + desiredVy * 0.1;
        vy = Math.max(speed * 0.75, vy);
        double currentMaxSide = vy * 0.7;
        vx = Math.max(-currentMaxSide, Math.min(currentMaxSide, vx));

        preciseX += vx;
        preciseY += vy;

        if (preciseY >= playHeight || preciseX + width < 0 || preciseX > playWidth) {
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
        return new Rectangle(getX(), getY(), width, height);
    }

    public int getX() {
        return (int) Math.round(preciseX);
    }

    public int getY() {
        return (int) Math.round(preciseY);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
