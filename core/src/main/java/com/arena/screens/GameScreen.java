package com.arena.screens;

import com.arena.ArenaGame;
import com.arena.render.Fx;
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
    private final Fx fx;
    private final WorldRenderer worldRenderer;
    private final Hud hud;
    private final InputState input = new InputState();
    private final Vector2 mouse = new Vector2();
    private final Vector2 camPos = new Vector2();
    private final Rectangle card = new Rectangle();
    private GameWorld world;
    private float accumulator;
    private boolean rightWasDown;

    public GameScreen(ArenaGame game) {
        this.game = game;
        fx = new Fx(game.assets);
        worldRenderer = new WorldRenderer(game.assets, fx);
        hud = new Hud(game.assets, fx);
        newRun();
    }

    /** Acesso para ferramentas de captura/teste. */
    public GameWorld world() { return world; }

    public Fx fx() { return fx; }

    private void newRun() {
        if (world != null) world.dispose();
        fx.reset();
        world = new GameWorld(game.audio, fx, TimeUtils.millis());
        camPos.set(world.player.x(), world.player.y());
        accumulator = 0f;
    }

    @Override
    public void render(float delta) {
        AudioKeys.handle(game.audio);
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) hud.showStats = !hud.showStats;
        readInput();

        accumulator += Math.min(delta, 0.25f);
        int steps = 0;
        while (accumulator >= Phys.STEP && steps++ < 5) {
            world.update(input, Phys.STEP);
            input.dash = input.special = false; // eventos de um frame so
            accumulator -= Phys.STEP;
        }
        if (steps >= 5) accumulator = 0f;
        boolean playing = world.state == GameWorld.State.PLAYING;
        fx.update(playing ? delta : 0f);

        // Camera suave, limitada a arena (com espaco para a parede de cima) + tremor
        float tx = MathUtils.clamp(world.player.x(), VIEW_W / 2f - 1f, Phys.ARENA_W - VIEW_W / 2f + 1f);
        float ty = MathUtils.clamp(world.player.y(), VIEW_H / 2f - 1f, Phys.ARENA_H + 2.6f - VIEW_H / 2f);
        camPos.lerp(new Vector2(tx, ty), Math.min(1f, delta * 10f));
        if (world.biomeFade > 0.9f) camPos.set(tx, ty);
        float shake = fx.trauma * fx.trauma * 0.45f;
        camera.position.set(camPos.x + MathUtils.random(-shake, shake), camPos.y + MathUtils.random(-shake, shake), 0);
        camera.update();

        ScreenUtils.clear(0.02f, 0.015f, 0.03f, 1f);
        viewport.apply();
        worldRenderer.render(game.batch, world, camera, playing ? delta : 0f,
                viewport.getScreenX(), viewport.getScreenY(), viewport.getScreenWidth(), viewport.getScreenHeight());

        hudViewport.apply();
        game.batch.setProjectionMatrix(hudViewport.getCamera().combined);
        game.batch.begin();
        hud.render(game.batch, world, game.audio, camera, viewport.getScreenX(), viewport.getScreenY(),
                viewport.getScreenWidth(), viewport.getScreenHeight(),
                hudViewport.getWorldWidth(), hudViewport.getWorldHeight(), hoveredCard());
        game.batch.end();
    }

    private void readInput() {
        input.moveX = (key(Input.Keys.D) || key(Input.Keys.RIGHT) ? 1 : 0) - (key(Input.Keys.A) || key(Input.Keys.LEFT) ? 1 : 0);
        input.moveY = (key(Input.Keys.W) || key(Input.Keys.UP) ? 1 : 0) - (key(Input.Keys.S) || key(Input.Keys.DOWN) ? 1 : 0);
        mouse.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mouse);
        input.aimX = mouse.x;
        input.aimY = mouse.y;
        input.firing = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        boolean right = Gdx.input.isButtonPressed(Input.Buttons.RIGHT);
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT)) input.dash = true;
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q) || (right && !rightWasDown)) input.special = true;
        rightWasDown = right;

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

    private static boolean key(int k) { return Gdx.input.isKeyPressed(k); }

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
        worldRenderer.lighting.resize(viewport.getScreenWidth(), viewport.getScreenHeight());
    }

    @Override
    public void dispose() {
        world.dispose();
        worldRenderer.lighting.dispose();
    }
}
