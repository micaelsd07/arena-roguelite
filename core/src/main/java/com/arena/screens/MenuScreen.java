package com.arena.screens;

import com.arena.ArenaGame;
import com.arena.audio.Sfx;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class MenuScreen extends ScreenAdapter {
    private final ArenaGame game;
    private final ScreenViewport viewport = new ScreenViewport();
    private float time;

    public MenuScreen(ArenaGame game) { this.game = game; }

    @Override
    public void render(float delta) {
        time += delta;
        AudioKeys.handle(game.audio);
        if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.audio.play(Sfx.SELECT);
            game.setScreen(new GameScreen(game));
            dispose();
            return;
        }
        ScreenUtils.clear(0.06f, 0.07f, 0.09f, 1f);
        viewport.apply();
        float w = viewport.getWorldWidth(), h = viewport.getWorldHeight();
        game.batch.setProjectionMatrix(viewport.getCamera().combined);
        game.batch.begin();
        BitmapFont f = game.assets.font;
        f.getData().setScale(3.4f);
        f.setColor(0.35f, 0.95f, 0.55f, 1f);
        f.draw(game.batch, "ARENA ROGUELITE", 0, h * 0.72f, w, Align.center, false);
        f.getData().setScale(1.3f);
        f.setColor(Color.LIGHT_GRAY);
        f.draw(game.batch, "WASD: mover    Mouse: mirar    Botão esquerdo: atirar\n"
                + "Sobreviva às ondas, colete gemas e escolha melhorias.\nM: mudo    - / +: volume",
                0, h * 0.55f, w, Align.center, false);
        f.getData().setScale(1.7f);
        f.setColor(1f, 0.9f, 0.4f, 0.55f + 0.45f * (float) Math.abs(Math.sin(time * 3)));
        f.draw(game.batch, "Clique para começar", 0, h * 0.32f, w, Align.center, false);
        f.getData().setScale(1f);
        f.setColor(Color.WHITE);
        game.batch.end();
    }

    @Override
    public void resize(int width, int height) { viewport.update(width, height, true); }
}
