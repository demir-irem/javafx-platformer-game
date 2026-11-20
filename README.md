# JavaFX Platformer Game

A 2D side-scrolling platformer written in Java with JavaFX. Collect apples, avoid spikes, and use your score to destroy spikes.

▶️ **Demo video:** https://youtu.be/F7VvvFK1w2Y

## Gameplay
- Move with **A / D** or **← / →**, jump with **W** or **↑**
- New spikes and apples appear every 3 seconds
- Every **2 apples** collected removes a random spike and gives **+1 point**
- With **5+ points**, press **SPACE** to destroy the nearest spike (costs 5 points)
- Touching a spike restarts the level

## What I implemented
- **Game object classes** (Player, Apple, Spike, Block, Terrain) using inheritance from a common `GameObject` base class, with sprite animation
- **Level loading** from `level.txt` (object type + x, y coordinates)
- **Collision detection** with world bounds and platforms
- **Movement physics:** horizontal movement, jumping, gravity, landing and ceiling detection
- **Random spawning** of spikes on platforms and apples in empty positions
- **Scoring and HUD updates**, apple collection and spike removal
- **Camera** that follows the player and stops at the world edges
- **Spike destroyer** skill, which finds the nearest spike by Euclidean distance

## Project structure
```
GameEngine.java   game loop, input, physics, spawning, camera
GameObject.java   base class: sprite, size, frame animation
Player / Apple / Spike / Block / Terrain .java
level.txt         level layout
assets/           sprites
```

## Run
Requires Java 17+ and the [JavaFX SDK](https://openjfx.io/).
```bash
javac --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls *.java
java  --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls GameEngine
```
Run it from the project folder so `level.txt` and `assets/` can be found.

*Developed for the BBM104 course at Hacettepe University. The course provided a starter framework: the `GameObject` base class, the window/HUD setup, the game loop, the level file and the sprites. I implemented the game logic listed above.*
