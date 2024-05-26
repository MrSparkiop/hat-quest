package com.hat_quest;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.hat_quest.BonusSystem;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

import java.util.Iterator;

public class MainWork implements Screen {

    // Textures and Sounds
    private Texture dropImage; // Texture for raindrops
    private Texture bucketImage; // Texture for the bucket
    private Sound dropSound; // Sound played when a raindrop is caught
    private static Music rainMusic; // Background music
    private Texture enemyDropImage; // Texture for enemy drops
    private Texture shieldImage;

    // Camera and Rendering
    private OrthographicCamera camera; // Camera for the game
    private SpriteBatch batch; // Renders 2D images

    // Game Objects
    private Rectangle bucket; // Represents the bucket
    private Array<Rectangle> raindrops; // Stores raindrops
    private Array<Rectangle> enemyDrops; // Stores enemy drops
    private long lastDropTime; // Time of last raindrop spawn
    private long lastEnemyDropTime; // Time of last enemy drop spawn
    private boolean showDeathScreen = false; // Show death screen
    private boolean isPaused = false; // Is the game paused
    private BitmapFont font; // Font for displaying text
    private ScoreBoard scoreBoard; // Score board for tracking points
    private float gravity = 1000; // Gravity force
    private float jumpVelocity = 250; // Variable for controlling jump height
    private float jumpHeight = 600; // Jump height
    private int jumpCount = 0; // Counter for jumps
    private int lives;
    private BonusSystem bonusSystem;
    private Shield shield;

    private Game game;

    public MainWork(Game game) {
        this.game = game;
    }

    //renamed create to show dont get them confused
    @Override
    public void show() {
        // Load resources and initialize game objects
        loadResources();
        setupCamera();
        createBucket();
        initializeRaindrops();
        enemyDrops = new Array<Rectangle>();
        lastEnemyDropTime = TimeUtils.nanoTime();
        font = new BitmapFont();
        scoreBoard = new ScoreBoard();
        bonusSystem = new BonusSystem("bonus.png");
        shield = new Shield();
        lives = 3;
    }

    @Override
    public void render(float delta) {
        if (isPaused) {
            ScreenUtils.clear(0, 0, 0.2f, 1);
            camera.update();
            batch.setProjectionMatrix(camera.combined);

            batch.begin();
            font.draw(batch, "Paused", 350, 240);
            font.draw(batch, "Press 'P' to Resume", 320, 200);
            batch.end();

            if (Gdx.input.isKeyJustPressed(Keys.P)) {
                isPaused = false;
                rainMusic.play(); // Resume music when unpausing
            }
            return;
        }

        // Update game state and render graphics
        handleInput();
        update(delta);
        ScreenUtils.clear(0, 0, 0.2f, 1);
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        if (!showDeathScreen) {
            renderBucket();
            renderRaindrops();
            renderEnemyDrops();
            bonusSystem.renderBonusDrops(batch);
            if (shield.isActive()) {
                renderShield();
            }

        } else {
            font.draw(batch, "Game Over!", 350, 240);
            font.draw(batch, "Press 'R' to Restart", 320, 200);
            font.draw(batch, "Esc for Main Menu", 320, 160);
            if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
                game.setScreen(new StartMenuScreen((MainGame) game));
                rainMusic.stop(); // Stop music when going to main menu
                return;
            }
        }
        int screenWidth = Gdx.graphics.getWidth();
        font.draw(batch, "Lives: " + lives, screenWidth - 100, 460);
        scoreBoard.draw(batch); // Draw the score board
        batch.end();
    }

    @Override
    public void resize(int width, int height) {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        dropImage.dispose();
        bucketImage.dispose();
        dropSound.dispose();
        rainMusic.dispose();
        if (batch != null) batch.dispose();
        scoreBoard.dispose();
        bonusSystem.dispose();
    }

    // Load textures and sounds
    private void loadResources() {
        dropImage = new Texture(Gdx.files.internal("droplet.png"));
        bucketImage = new Texture(Gdx.files.internal("bucket.png"));
        dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.wav"));
        rainMusic = Gdx.audio.newMusic(Gdx.files.internal("rain.mp3"));
        enemyDropImage = new Texture(Gdx.files.internal("enemy_droplet.png"));
        shieldImage = new Texture(Gdx.files.internal("shield.png")); // Load shield texture
        rainMusic.setLooping(true);
        rainMusic.play();
    }

    // Setup camera for rendering
    private void setupCamera() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);
        batch = new SpriteBatch();

    }

    // Create the bucket object
    private void createBucket() {
        bucket = new Rectangle();
        bucket.x = (float)800 / 2 - (float)64 / 2;
        bucket.y = 20;
        bucket.width = 64;
        bucket.height = 64;
    }

    // Initialize raindrop array and spawn first raindrop
    private void initializeRaindrops() {
        raindrops = new Array<Rectangle>();
        spawnRaindrop();
    }

    // Handle user input for moving the bucket and jumping
    private void handleInput() {
        if (!showDeathScreen) {
            if (Gdx.input.isKeyPressed(Keys.A))
                bucket.x -= 300 * Gdx.graphics.getDeltaTime();
            if (Gdx.input.isKeyPressed(Keys.D))
                bucket.x += 300 * Gdx.graphics.getDeltaTime();
            if (Gdx.input.isKeyJustPressed(Keys.SPACE) && jumpCount < 2) {
                jumpVelocity = jumpHeight;
                jumpCount++;
            }
            if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
                game.setScreen(new StartMenuScreen((MainGame) game));
                rainMusic.stop(); // Stop music when going to main menu
                return;
            }
            if (Gdx.input.isKeyJustPressed(Keys.P)) {
                isPaused = true;
                rainMusic.pause(); // Pause music
            }
            if (Gdx.input.isKeyJustPressed(Keys.F) && shield.canActivate()) {
                shield.activate();
            }
        } else {
            if (Gdx.input.isKeyPressed(Keys.R)) {
                restartGame();
            }
            if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
                game.setScreen(new StartMenuScreen((MainGame) game));
                rainMusic.stop(); // Stop music when going to main menu
                return;
            }
        }
    }

    // Update game objects
    private void update(float delta) {
        if (showDeathScreen) {
            return;
        }
        // Update bucket position based on jump velocity
        bucket.y += jumpVelocity * delta;

        // Apply gravity to jump velocity to simulate falling
        jumpVelocity -= gravity * delta;

        // Check if bucket reaches bottom of the screen
        if (bucket.y < 0) {
            bucket.y = 0;
            jumpVelocity = 0; // Reset jump velocity
            jumpCount = 0; // Reset jump count when bucket is on the ground
        }

        // Ensure the bucket stays within the screen bounds
        if (bucket.x < 0)
            bucket.x = 0;
        if (bucket.x > 800 - 64)
            bucket.x = 800 - 64;

        updateRaindrops(delta);
        updateEnemyDrops(delta);
        shield.update();
        bonusSystem.updateBonusDrops(delta, bucket, this);

        long timeNow = TimeUtils.nanoTime();
        if (timeNow - lastDropTime > 1000000000) { // 1 second for normal drops
            spawnRaindrop();
            lastDropTime = timeNow;
        }
        if (timeNow - lastEnemyDropTime > 1500000000) { // 1.5 seconds for enemy drops
            if (MathUtils.randomBoolean(0.5f)) { // 50% chance to spawn an enemy drop
                spawnEnemyDrop();
            }
            lastEnemyDropTime = timeNow;
        }
        if (MathUtils.randomBoolean(0.001f)) { // chance to spawn a bonus drop
            bonusSystem.spawnBonusDrop();
        }
    }

    // Update raindrops and handle collisions
    private void updateRaindrops(float delta) {
        if (showDeathScreen) {
            return;
        }
        for (Iterator<Rectangle> iter = raindrops.iterator(); iter.hasNext();) {
            Rectangle raindrop = iter.next();
            raindrop.y -= 200 * delta; // Update raindrop position
            if (raindrop.y + 64 < 0) {
                iter.remove(); // Remove raindrop if it's below the screen
            } else if (raindrop.overlaps(bucket)) {
                dropSound.play(); // Play sound if bucket catches raindrop
                scoreBoard.addScore(1); // Add score when a raindrop is caught
                iter.remove();
            }
        }
    }

    // Update enemy drops and handle collisions
    private void updateEnemyDrops(float delta) {
        for (Iterator<Rectangle> iter = enemyDrops.iterator(); iter.hasNext();) {
            Rectangle enemyDrop = iter.next();
            enemyDrop.y -= 200 * delta;
            if (enemyDrop.y + 64 < 0) {
                iter.remove();
            } else if (enemyDrop.overlaps(bucket)) {
                if (shield.isActive()) {
                    iter.remove();
                } else {
                    lives--;
                    if (lives <= 0) {
                        showDeathScreen = true;
                        rainMusic.stop();
                    }
                    iter.remove();
                }
            }
        }
    }

    // Render the bucket
    private void renderBucket() {
        batch.draw(bucketImage, bucket.x, bucket.y);
    }

    // Render raindrops
    private void renderRaindrops() {
        for (Rectangle raindrop : raindrops) {
            batch.draw(dropImage, raindrop.x, raindrop.y);
        }
    }

    // Render enemy drops
    private void renderEnemyDrops() {
        for (Rectangle enemyDrop : enemyDrops) {
            batch.draw(enemyDropImage, enemyDrop.x, enemyDrop.y);
        }
    }

    private void renderShield() {
        batch.draw(shieldImage, bucket.x - 10, bucket.y - 10, bucket.width + 20, bucket.height + 20);
    }


    // Spawn a new raindrop
    private void spawnRaindrop() {
        Rectangle raindrop = new Rectangle();
        raindrop.x = MathUtils.random(0, 800 - 64);
        raindrop.y = 480;
        raindrop.width = 64;
        raindrop.height = 64;
        raindrops.add(raindrop);
        lastDropTime = TimeUtils.nanoTime();
    }

    // Spawn a new enemy drop
    private void spawnEnemyDrop() {
        Rectangle enemyDrop = new Rectangle();
        enemyDrop.x = MathUtils.random(0, 800 - 64);
        enemyDrop.y = 480;
        enemyDrop.width = 64;
        enemyDrop.height = 64;
        enemyDrops.add(enemyDrop);
        lastEnemyDropTime = TimeUtils.nanoTime();
    }

    // Restart the game
    private void restartGame() {
        raindrops.clear();
        enemyDrops.clear();
        createBucket();
        lastDropTime = TimeUtils.nanoTime();
        lastEnemyDropTime = TimeUtils.nanoTime();
        showDeathScreen = false;
        scoreBoard.resetScore();
        lives = 3;
        rainMusic.play();
    }

    public void addLife() {
        if (lives < 3) {
            lives++;
        }
    }

    // Method to update the volume
    public static void updateVolume(float volume) {
        rainMusic.setVolume(volume);

    }
}
