# AI Shooter Challenge

這是一個使用 Java Swing 製作的 2D 射擊遊戲，依照 PDF 題目實作了：

- 玩家可使用 WASD 或方向鍵移動
- SPACE 射擊
- 敵人使用 BFS 在格子地圖上追蹤玩家
- 障礙物會阻擋敵人路徑
- 分數、HP、關卡、暫停、重開機制

## 執行方式

需求：Java 17 以上。

在專案根目錄執行：

```powershell
$files = Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d out $files
java -cp out ai.shooter.Main
```

## BFS AI 說明

敵人把遊戲區視為 4 方向格子地圖，使用 BFS 找出從自己到玩家所在格子的最短路徑。
每次更新只沿著路徑前進一步，因此敵人會繞開障礙物並持續逼近玩家。