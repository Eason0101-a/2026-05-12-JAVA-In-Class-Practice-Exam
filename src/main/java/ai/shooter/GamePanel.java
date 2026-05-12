package ai.shooter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.Timer;

public final class GamePanel extends JPanel implements KeyListener {
    private static final int CELL_SIZE = 40;
    private static final int GRID_COLUMNS = 22;
    private static final int GRID_ROWS = 22;
    private static final int PLAY_WIDTH = GRID_COLUMNS * CELL_SIZE;
    private static final int PLAY_HEIGHT = GRID_ROWS * CELL_SIZE;
    private static final int HUD_WIDTH = 340;
    private static final int PANEL_WIDTH = PLAY_WIDTH + HUD_WIDTH;
    private static final int PANEL_HEIGHT = PLAY_HEIGHT;
    private static final int PLAYER_SIZE = 28;
    private static final int ENEMY_SIZE = 34;
    private static final int BULLET_WIDTH = 6;
    private static final int BULLET_HEIGHT = 18;

    private final Random random = new Random();
    private final GridMap map = new GridMap(GRID_COLUMNS, GRID_ROWS, CELL_SIZE);
    private final PathFinder pathFinder = new PathFinder();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<EnemyBullet> enemyBullets = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Point playerSpawn = new Point(PLAY_WIDTH / 2, PLAY_HEIGHT - 90);
    private final int[] starX;
    private final double[] starY;
    private final int[] starRadius;
    private final double[] starSpeed;
    private final Timer timer;

    private Player player;
    private boolean paused;
    private boolean gameOver;
    private int score;
    private int hp;
    private int level;
    private int enemiesDestroyed;
    private long lastUpdateNanos;
    private int tick;
    private int lastEnemySpawnTick;
    private int lastShotTick;
    private int lastEnemyShotTick;
    private long nowMillisForPaint;

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(new Color(6, 12, 24));
        setFocusable(true);
        addKeyListener(this);

        starX = new int[80];
        starY = new double[80];
        starRadius = new int[80];
        starSpeed = new double[80];
        for (int i = 0; i < starX.length; i++) {
            starX[i] = random.nextInt(PLAY_WIDTH);
            starY[i] = random.nextInt(PLAY_HEIGHT);
            starRadius[i] = 1 + random.nextInt(3);
            starSpeed[i] = 30 + random.nextInt(60);
        }

        timer = new Timer(16, e -> updateGame());
        resetGame();
        timer.start();

        javax.swing.SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    private void resetGame() {
        player = new Player(playerSpawn.x, playerSpawn.y, PLAYER_SIZE, 5);
        enemies.clear();
        bullets.clear();
        enemyBullets.clear();
        obstacles.clear();
        pressedKeys.clear();
        paused = false;
        gameOver = false;
        score = 0;
        hp = 5;
        level = 1;
        enemiesDestroyed = 0;
        lastUpdateNanos = System.nanoTime();
        tick = 0;
        lastEnemySpawnTick = -9999;
        lastShotTick = -9999;
        lastEnemyShotTick = -9999;

        generateObstacles();
    }

    private void generateObstacles() {
        obstacles.clear();
        int count = 18;
        Rectangle playerSafe = new Rectangle(player.getX() - 2 * CELL_SIZE,
                player.getY() - 2 * CELL_SIZE, CELL_SIZE * 5, CELL_SIZE * 5);
        Rectangle topSafe = new Rectangle(0, 0, PLAY_WIDTH, CELL_SIZE * 3);

        int attempts = 0;
        while (obstacles.size() < count && attempts++ < 400) {
            int col = random.nextInt(GRID_COLUMNS);
            int row = random.nextInt(GRID_ROWS);
            Rectangle cellRect = new Rectangle(col * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            if (cellRect.intersects(playerSafe) || cellRect.intersects(topSafe)) {
                continue;
            }
            obstacles.add(new Obstacle(col, row * CELL_SIZE, CELL_SIZE));
        }
        rebuildBlockedGrid();
    }

    private void updateObstacles() {
        double obstacleSpeed = 1.6 + level * 0.08;
        for (Obstacle obstacle : obstacles) {
            obstacle.update(obstacleSpeed, GRID_COLUMNS, PLAY_HEIGHT, random);
        }
        rebuildBlockedGrid();
    }

    private void rebuildBlockedGrid() {
        map.clearBlocked();
        for (Obstacle obstacle : obstacles) {
            int row = obstacle.getRow();
            int col = obstacle.getCol();
            map.setBlocked(col, row, true);
        }
    }

    private void updateGame() {
        long nowNanos = System.nanoTime();
        long nowMillis = System.currentTimeMillis();
        long deltaMillis = (nowNanos - lastUpdateNanos) / 1_000_000L;
        if (deltaMillis < 0) {
            deltaMillis = 0;
        }
        lastUpdateNanos = nowNanos;

        nowMillisForPaint = nowMillis;
        updateBackground(deltaMillis);

        if (!paused && !gameOver) {
            tick++;
            level = 1 + score / 10;

            updateObstacles();
            updatePlayerMovement();
            updatePlayerShooting();
            spawnEnemies();
            enemyShoot();

            updateBullets();
            updateEnemyBullets();
            updateEnemies();
            handleCollisions();
        }

        repaint();
    }

    private void updateBackground(long deltaMillis) {
        double deltaSeconds = deltaMillis / 1000.0;
        for (int i = 0; i < starY.length; i++) {
            starY[i] += starSpeed[i] * deltaSeconds;
            if (starY[i] > PLAY_HEIGHT) {
                starY[i] = 0;
                starX[i] = random.nextInt(PLAY_WIDTH);
                starRadius[i] = 1 + random.nextInt(3);
                starSpeed[i] = 30 + random.nextInt(60);
            }
        }
    }

    private void updatePlayerMovement() {
        double dx = 0;
        double dy = 0;

        if (pressedKeys.contains(KeyEvent.VK_LEFT) || pressedKeys.contains(KeyEvent.VK_A)) {
            dx -= player.getSpeed();
        }
        if (pressedKeys.contains(KeyEvent.VK_RIGHT) || pressedKeys.contains(KeyEvent.VK_D)) {
            dx += player.getSpeed();
        }
        if (pressedKeys.contains(KeyEvent.VK_UP) || pressedKeys.contains(KeyEvent.VK_W)) {
            dy -= player.getSpeed();
        }
        if (pressedKeys.contains(KeyEvent.VK_DOWN) || pressedKeys.contains(KeyEvent.VK_S)) {
            dy += player.getSpeed();
        }

        if (dx != 0 && dy != 0) {
            double inv = 1.0 / Math.sqrt(2.0);
            dx *= inv;
            dy *= inv;
        }

        if (dx != 0) {
            movePlayerAxis((int) Math.round(dx), 0);
        }
        if (dy != 0) {
            movePlayerAxis(0, (int) Math.round(dy));
        }
    }

    private void movePlayerAxis(int dx, int dy) {
        int oldX = player.getX();
        int oldY = player.getY();
        player.move(dx, dy);
        clampPlayer();

        Rectangle playerBounds = player.getBounds();
        if (intersectsObstacle(playerBounds)) {
            player.setPosition(oldX, oldY);
        }
    }

    private void clampPlayer() {
        int maxX = PLAY_WIDTH - player.getSize();
        int maxY = PLAY_HEIGHT - player.getSize();
        int x = Math.max(0, Math.min(maxX, player.getX()));
        int y = Math.max(0, Math.min(maxY, player.getY()));
        player.setPosition(x, y);
    }

    private void updateBullets() {
        Iterator<Bullet> iterator = bullets.iterator();
        while (iterator.hasNext()) {
            Bullet bullet = iterator.next();
            bullet.update();
            if (!bullet.isActive()) {
                iterator.remove();
            }
        }
    }

    private void updatePlayerShooting() {
        if (!pressedKeys.contains(KeyEvent.VK_SPACE)) {
            return;
        }

        int intervalTicks = 5;
        if (tick - lastShotTick < intervalTicks) {
            return;
        }

        int bulletX = player.getCenterX() - BULLET_WIDTH / 2;
        int bulletY = player.getY() - BULLET_HEIGHT - 2;
        bullets.add(new Bullet(bulletX, bulletY, BULLET_WIDTH, BULLET_HEIGHT, 10));
        lastShotTick = tick;
    }

    private void spawnEnemies() {
        int intervalTicks = Math.max(24, 80 - level * 4);
        if (tick - lastEnemySpawnTick < intervalTicks) {
            return;
        }

        int attempts = 40;
        while (attempts-- > 0) {
            int col = random.nextInt(GRID_COLUMNS);
            int row = 0;
            if (!map.isWalkable(col, row)) {
                continue;
            }

            int x = col * CELL_SIZE + CELL_SIZE / 2 - ENEMY_SIZE / 2;
            int y = 6;
            double speed = 1.6 + level * 0.08;
            enemies.add(new Enemy(x, y, ENEMY_SIZE, speed));
            break;
        }

        lastEnemySpawnTick = tick;
    }

    private void enemyShoot() {
        if (enemies.isEmpty()) {
            return;
        }

        int intervalTicks = Math.max(22, 62 - level * 3);
        if (tick - lastEnemyShotTick < intervalTicks) {
            return;
        }

        Enemy shooter = enemies.get(random.nextInt(enemies.size()));
        int bulletX = shooter.getCenterX() - 4;
        int bulletY = shooter.getY() + shooter.getSize();
        enemyBullets.add(EnemyBullet.createHoming(bulletX, bulletY, player));
        lastEnemyShotTick = tick;
    }

    private void updateEnemyBullets() {
        Iterator<EnemyBullet> iterator = enemyBullets.iterator();
        while (iterator.hasNext()) {
            EnemyBullet bullet = iterator.next();
            bullet.update(player, PLAY_WIDTH, PLAY_HEIGHT);
            if (!bullet.isActive()) {
                iterator.remove();
            }
        }
    }

    private void updateEnemies() {
        Iterator<Enemy> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            enemy.updateTowardsPlayer(player, map, pathFinder);
            if (enemy.getY() > PLAY_HEIGHT - CELL_SIZE) {
                iterator.remove();
            }
        }
    }

    private void handleCollisions() {
        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            Rectangle bulletBounds = bullet.getBounds();

            boolean hit = false;
            Iterator<Enemy> enemyIterator = enemies.iterator();
            while (enemyIterator.hasNext()) {
                Enemy enemy = enemyIterator.next();
                if (bulletBounds.intersects(enemy.getBounds())) {
                    enemyIterator.remove();
                    bulletIterator.remove();
                    enemiesDestroyed++;
                    score += 1;
                    hit = true;
                    break;
                }
            }

            if (hit) {
                continue;
            }

            Iterator<EnemyBullet> enemyBulletIterator = enemyBullets.iterator();
            while (enemyBulletIterator.hasNext()) {
                EnemyBullet enemyBullet = enemyBulletIterator.next();
                if (bulletBounds.intersects(enemyBullet.getBounds())) {
                    enemyBulletIterator.remove();
                    bulletIterator.remove();
                    hit = true;
                    break;
                }
            }

            if (hit) {
                continue;
            }

            Iterator<Obstacle> obstacleIterator = obstacles.iterator();
            while (obstacleIterator.hasNext()) {
                Obstacle obstacle = obstacleIterator.next();
                if (bulletBounds.intersects(obstacle.getBounds())) {
                    bulletIterator.remove();
                    obstacle.respawn(GRID_COLUMNS, PLAY_HEIGHT, random);
                    rebuildBlockedGrid();
                    hit = true;
                    break;
                }
            }
        }

        Rectangle playerBounds = player.getBounds();
        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            if (playerBounds.intersects(enemy.getBounds())) {
                enemyIterator.remove();
                hp--;
                if (hp <= 0) {
                    gameOver = true;
                }
                break;
            }
        }

        Iterator<EnemyBullet> enemyBulletIterator = enemyBullets.iterator();
        while (enemyBulletIterator.hasNext()) {
            EnemyBullet enemyBullet = enemyBulletIterator.next();
            if (playerBounds.intersects(enemyBullet.getBounds())) {
                enemyBulletIterator.remove();
                hp--;
                if (hp <= 0) {
                    gameOver = true;
                }
                break;
            }
        }
    }

    private boolean intersectsObstacle(Rectangle bounds) {
        for (Obstacle obstacle : obstacles) {
            if (bounds.intersects(obstacle.getBounds())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        paintBackground(g2);
        paintPlayField(g2);
        paintHud(g2);
        paintOverlays(g2);

        g2.dispose();
    }

    private void paintBackground(Graphics2D g2) {
        g2.setPaint(new java.awt.GradientPaint(0, 0, new Color(6, 12, 24), 0, PANEL_HEIGHT, new Color(13, 22, 42)));
        g2.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g2.setColor(new Color(255, 255, 255, 180));
        for (int i = 0; i < starX.length; i++) {
            int y = (int) Math.round(starY[i]);
            g2.fillOval(starX[i], y, starRadius[i], starRadius[i]);
        }
    }

    private void paintPlayField(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRoundRect(18, 18, PLAY_WIDTH - 36, PLAY_HEIGHT - 36, 24, 24);

        g2.setColor(new Color(38, 132, 205, 150));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(18, 18, PLAY_WIDTH - 36, PLAY_HEIGHT - 36, 24, 24);

        g2.setColor(new Color(52, 86, 130, 70));
        for (int x = 20; x < PLAY_WIDTH; x += CELL_SIZE) {
            g2.drawLine(x, 20, x, PLAY_HEIGHT - 20);
        }
        for (int y = 20; y < PLAY_HEIGHT; y += CELL_SIZE) {
            g2.drawLine(20, y, PLAY_WIDTH - 20, y);
        }

        for (Obstacle obstacle : obstacles) {
            paintAsteroid(g2, obstacle.getBounds());
        }

        for (Bullet bullet : bullets) {
            paintBullet(g2, bullet);
        }

        for (EnemyBullet enemyBullet : enemyBullets) {
            paintEnemyBullet(g2, enemyBullet);
        }

        for (Enemy enemy : enemies) {
            paintEnemy(g2, enemy);
        }

        paintPlayer(g2);
    }

    private void paintPlayer(Graphics2D g2) {
        Rectangle bounds = player.getBounds();
        Polygon ship = new Polygon();
        ship.addPoint(bounds.x + bounds.width / 2, bounds.y - 4);
        ship.addPoint(bounds.x + bounds.width - 2, bounds.y + bounds.height / 2);
        ship.addPoint(bounds.x + bounds.width * 3 / 4, bounds.y + bounds.height);
        ship.addPoint(bounds.x + bounds.width / 4, bounds.y + bounds.height);
        ship.addPoint(bounds.x + 2, bounds.y + bounds.height / 2);

        g2.setColor(new Color(220, 240, 255));
        g2.fillPolygon(ship);
        g2.setColor(new Color(0, 145, 255));
        g2.drawPolygon(ship);

        g2.setColor(new Color(80, 190, 255, 120));
        g2.fillOval(bounds.x + 7, bounds.y + bounds.height - 2, 14, 16);
    }

    private void paintEnemy(Graphics2D g2, Enemy enemy) {
        Rectangle bounds = enemy.getBounds();
        Polygon ship = new Polygon();
        ship.addPoint(bounds.x + bounds.width / 2, bounds.y);
        ship.addPoint(bounds.x + bounds.width - 1, bounds.y + bounds.height / 2);
        ship.addPoint(bounds.x + bounds.width / 2, bounds.y + bounds.height);
        ship.addPoint(bounds.x, bounds.y + bounds.height / 2);

        g2.setColor(new Color(255, 95, 95));
        g2.fillPolygon(ship);
        g2.setColor(new Color(120, 10, 10));
        g2.drawPolygon(ship);
        g2.setColor(new Color(255, 220, 110));
        g2.fillOval(bounds.x + bounds.width / 2 - 3, bounds.y + bounds.height / 2 - 3, 6, 6);
    }

    private void paintBullet(Graphics2D g2, Bullet bullet) {
        g2.setColor(new Color(255, 98, 72));
        g2.fillRoundRect(bullet.getX(), bullet.getY(), bullet.getWidth(), bullet.getHeight(), 6, 6);
        g2.setColor(new Color(255, 223, 132));
        g2.fillRect(bullet.getX() + 1, bullet.getY() + 2, bullet.getWidth() - 2, Math.max(2, bullet.getHeight() / 3));
    }

    private void paintEnemyBullet(Graphics2D g2, EnemyBullet bullet) {
        g2.setColor(new Color(255, 200, 0));
        g2.fillRoundRect(bullet.getX(), bullet.getY(), bullet.getWidth(), bullet.getHeight(), 6, 6);
        g2.setColor(new Color(255, 100, 0));
        g2.drawRoundRect(bullet.getX(), bullet.getY(), bullet.getWidth(), bullet.getHeight(), 6, 6);
    }

    private void paintAsteroid(Graphics2D g2, Rectangle bounds) {
        double pulse = 0.5 + 0.5 * Math.sin(nowMillisForPaint / 350.0 + bounds.x * 0.02 + bounds.y * 0.02);
        int glowAlpha = 90 + (int) (pulse * 60);
        int rimAlpha = 130 + (int) (pulse * 60);
        g2.setColor(new Color(70, 70, 80));
        g2.fillOval(bounds.x + 2, bounds.y + 2, bounds.width - 4, bounds.height - 4);
        g2.setColor(new Color(140, 140, 150, glowAlpha));
        g2.fillOval(bounds.x + 6, bounds.y + 4, bounds.width / 2, bounds.height / 3);
        g2.setColor(new Color(25, 25, 25, rimAlpha));
        g2.drawOval(bounds.x + 2, bounds.y + 2, bounds.width - 4, bounds.height - 4);
    }

    private void paintHud(Graphics2D g2) {
        int x = PLAY_WIDTH + 18;
        int panelX = PLAY_WIDTH + 12;

        g2.setColor(new Color(0, 0, 0, 135));
        g2.fillRoundRect(panelX, 18, HUD_WIDTH - 30, 262, 24, 24);
        g2.fillRoundRect(panelX, 296, HUD_WIDTH - 30, 270, 24, 24);

        g2.setColor(new Color(45, 170, 255, 170));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(panelX, 18, HUD_WIDTH - 30, 262, 24, 24);
        g2.drawRoundRect(panelX, 296, HUD_WIDTH - 30, 270, 24, 24);

        g2.setFont(new Font("SansSerif", Font.BOLD, 24));
        g2.setColor(Color.WHITE);
        g2.drawString("SCORE", x, 55);

        g2.setFont(new Font("SansSerif", Font.BOLD, 42));
        g2.setColor(new Color(255, 244, 210));
        g2.drawString(String.valueOf(score), x, 110);

        g2.setFont(new Font("SansSerif", Font.BOLD, 24));
        g2.setColor(Color.WHITE);
        g2.drawString("HP", x, 160);
        drawHearts(g2, x, 180, hp);

        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2.drawString("KILLS", x, 228);
        g2.setFont(new Font("SansSerif", Font.BOLD, 28));
        g2.setColor(new Color(255, 205, 96));
        g2.drawString(String.valueOf(enemiesDestroyed), x + 90, 230);

        g2.drawString("LEVEL", x, 250);
        g2.setColor(new Color(110, 210, 255));
        g2.fillRoundRect(x, 266, 240, 16, 16, 16);
        g2.setColor(new Color(45, 94, 120));
        g2.fillRoundRect(x, 266, Math.min(240, 55 + level * 18), 16, 16, 16);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 36));
        g2.drawString(String.valueOf(level), x + 255, 295);

        g2.setFont(new Font("SansSerif", Font.BOLD, 24));
        g2.drawString("CONTROLS", x, 340);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g2.drawString("Move : WASD / Arrows", x, 384);
        g2.drawString("Shoot : SPACE", x, 426);
        g2.drawString("Pause : P", x, 468);
        g2.drawString("Restart : R", x, 510);

        g2.setColor(new Color(255, 205, 96));
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.drawString("Enemy AI: BFS Grid Path", x, 552);
    }

    private void drawHearts(Graphics2D g2, int x, int y, int count) {
        for (int i = 0; i < 5; i++) {
            int heartX = x + i * 40;
            if (i < count) {
                g2.setColor(new Color(255, 72, 72));
            } else {
                g2.setColor(new Color(120, 40, 48));
            }
            paintHeart(g2, heartX, y, 28, 26);
        }
    }

    private void paintHeart(Graphics2D g2, int x, int y, int width, int height) {
        Ellipse2D left = new Ellipse2D.Double(x, y, width / 2.0, height / 2.0);
        Ellipse2D right = new Ellipse2D.Double(x + width / 2.0, y, width / 2.0, height / 2.0);
        Polygon bottom = new Polygon();
        bottom.addPoint(x, y + height / 4);
        bottom.addPoint(x + width, y + height / 4);
        bottom.addPoint(x + width / 2, y + height);
        g2.fill(left);
        g2.fill(right);
        g2.fillPolygon(bottom);
    }

    private void paintOverlays(Graphics2D g2) {
        if (paused) {
            paintCenterOverlay(g2, "PAUSED", "Press P to resume");
        }
        if (gameOver) {
            paintCenterOverlay(g2, "GAME OVER", "Press R to restart");
        }
    }

    private void paintCenterOverlay(Graphics2D g2, String title, String subtitle) {
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRect(0, 0, PLAY_WIDTH, PANEL_HEIGHT);

        g2.setColor(new Color(255, 255, 255));
        g2.setFont(new Font("SansSerif", Font.BOLD, 48));
        FontMetrics metrics = g2.getFontMetrics();
        int titleX = (PLAY_WIDTH - metrics.stringWidth(title)) / 2;
        g2.drawString(title, titleX, PANEL_HEIGHT / 2 - 10);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 24));
        metrics = g2.getFontMetrics();
        int subtitleX = (PLAY_WIDTH - metrics.stringWidth(subtitle)) / 2;
        g2.drawString(subtitle, subtitleX, PANEL_HEIGHT / 2 + 30);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        pressedKeys.add(keyCode);

        if (keyCode == KeyEvent.VK_P) {
            paused = !paused;
        } else if (keyCode == KeyEvent.VK_R) {
            resetGame();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }
}