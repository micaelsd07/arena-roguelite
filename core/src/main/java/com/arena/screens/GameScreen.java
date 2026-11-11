package com.arena.screens;

import com.arena.ArenaGame;
import com.arena.render.Hud;
import com.arena.render.WorldRenderer;
import com.arena.world.GameWorld;
import com.arena.world.InputState;
import com.arena.world.Phys;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class GameScreen extends ScreenAdapter {
    private static final float VIEW_W = 24f, VIEW_H = 13.5f;

    private final ArenaGame game;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final FitViewport viewport = new FitViewport(VIEW_W, VIEW_H, camera);
    private final ScreenViewport hudViewport = new ScreenViewport();
    private final WorldRenderer worldRenderer;
    private final Hud hud;
    private final InputState input = new InputState();
    private final Vector2 mouse = new Vector2();
    private final Rectangle card = new Rectangle();
    private GameWorld world;
    private float accumulator;

    public GameScreen(ArenaGame game) {
        this.game = game;
        worldRenderer = new WorldRenderer(game.assets);
        hud = new Hud(game.assets);
        newRun();
    }

    private void newRun() {
        if (world != null) world.dispose();
        world = new GameWorld(game.audio, TimeUtils.millis());
        accumulator = 0f;
    }

    @Override
    public void render(float delta) {
        AudioKeys.handle(game.audio);
        readInput();

        // Passo fixo de fisica (60 Hz), no maximo 5 passos por frame
        accumulator += Math.min(delta, 0.25f);
        int steps = 0;
        while (accumulator >= Phys.STEP && steps++ < 5) {
            world.update(input, Phys.STEP);
            accumulator -= Phys.STEP;
        }
        if (steps >= 5) accumulator = 0f;

        // Camera segue o jogador, limitada as bordas da arena
        camera.position.set(
                MathUtils.clamp(world.player.x(), VIEW_W / 2f - 1f, Phys.ARENA_W - VIEW_W / 2f + 1f),
                MathUtils.clamp(world.player.y(), VIEW_H / 2f - 1f, Phys.ARENA_H - VIEW_H / 2f + 1f), 0);
        camera.update();

        ScreenUtils.clear(0.04f, 0.045f, 0.06f, 1f);
        viewport.apply();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        worldRenderer.render(game.batch, world, input.aimX, input.aimY);
        game.batch.end();

        hudViewport.apply();
        game.batch.setProjectionMatrix(hudViewport.getCamera().combined);
        game.batch.begin();
        hud.render(game.batch, world, game.audio, hudViewport.getWorldWidth(), hudViewport.getWorldHeight(), hoveredCard());
        game.batch.end();
    }

    private void readInput() {
        input.moveX = (Gdx.input.isKeyPressed(Input.Keys.D) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.A) ? 1 : 0);
        input.moveY = (Gdx.input.isKeyPressed(Input.Keys.W) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.S) ? 1 : 0);
        mouse.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mouse);
        input.aimX = mouse.x;
        input.aimY = mouse.y;
        input.firing = Gdx.input.isButtonPressed(Input.Buttons.LEFT);

        if (world.state == GameWorld.State.LEVEL_UP) {
            input.firing = false;
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) world.chooseUpgrade(0);
            else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) world.chooseUpgrade(1);
            else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) world.chooseUpgrade(2);
            else if (Gdx.input.justTouched()) {
                int c = hoveredCard();
                if (c >= 0) world.chooseUpgrade(c);
            }
        } else if (world.state == GameWorld.State.GAME_OVER && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            newRun();
        }
    }

    private int hoveredCard() {
        if (world.state != GameWorld.State.LEVEL_UP) return -1;
        Vector2 m = hudViewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        for (int i = 0; i < 3; i++) {
            if (Hud.cardRect(i, hudViewport.getWorldWidth(), hudViewport.getWorldHeight(), card).contains(m)) return i;
        }
        return -1;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        hudViewport.update(width, height, true);
    }

    @Override
    public void dispose() { world.dispose(); }
}
