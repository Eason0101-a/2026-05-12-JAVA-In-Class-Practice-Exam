package ai.shooter;

import javax.swing.JFrame;

public final class GameFrame extends JFrame {
    public GameFrame() {
        super("AI Shooter Challenge");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setContentPane(new GamePanel());
        pack();
        setLocationRelativeTo(null);
    }
}