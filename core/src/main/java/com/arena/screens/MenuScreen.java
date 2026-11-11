package com.arena.screens;

import com.arena.ArenaGame;
import com.arena.audio.Sfx;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
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
        ScreenUtils.clear(0.02f, 0.015f, 0.03f, 1f);
        viewport.apply();
        float w = viewport.getWorldWidth(), h = viewport.getWorldHeight();
        SpriteBatch b = game.batch;
        b.setProjectionMatrix(viewport.getCamera().combined);
        b.begin();

        // Chao da cripta escurecido nas bordas
        Array<TextureRegion> floor = game.assets.frames("floor_crypt");
        float tile = 64;
        for (int y = 0; y * tile < h; y++)
            for (int x = 0; x * tile < w; x++) {
                float dx = (x * tile + 32 - w / 2) / (w / 2), dy = (y * tile + 32 - h * 0.35f) / (h / 2);
                float light = Math.max(0.08f, 0.75f - (dx * dx + dy * dy) * 0.6f) * (0.95f + 0.05f * MathUtils.sin(time * 7 + x));
                b.setColor(light, light * 0.9f, light * 0.85f, 1f);
                b.draw(floor.get(((x * 7 + y * 13) % 10) < 6 ? 0 : 1 + (x + y) % 3), x * tile, y * tile, tile, tile);
            }

        // Elenco: herói ao centro, inimigos dos lados
        String[] cast = {"wraith", "necro", "hero_down", "werewolf_front", "knight"};
        float[] heights = {170, 180, 160, 180, 200};
        float spacing = Math.min(220, w / 5.5f);
        for (int i = 0; i < cast.length; i++) {
            TextureRegion r = game.assets.frame(cast[i], time + i, 3f);
            float hh = heights[i] * Math.min(1f, h / 720f), ww = hh * r.getRegionWidth() / r.getRegionHeight();
            float x = w / 2 + (i - 2) * spacing, y = h * 0.2f + (i == 2 ? 0 : 6 * MathUtils.sin(time * 2 + i));
            b.setColor(0, 0, 0, 0.5f);
            b.draw(game.assets.shadow, x - ww * 0.45f, y - 10, ww * 0.9f, 22);
            b.setColor(i == 2 ? Color.WHITE : new Color(0.75f, 0.7f, 0.8f, 1f));
            b.draw(r, x - ww / 2, y, ww / 2, 0, ww, hh, (i == 4 || i == 1) ? -1 : 1, 1, 0);
        }

        BitmapFont f = game.assets.font;
        f.getData().setScale(3.6f);
        f.setColor(0, 0, 0, 0.7f);
        f.draw(b, "ARENA ROGUELITE", 3, h * 0.86f - 3, w, Align.center, false);
        f.setColor(1f, 0.82f, 0.4f, 1f);
        f.draw(b, "ARENA ROGUELITE", 0, h * 0.86f, w, Align.center, false);
        f.getData().setScale(1.25f);
        f.setColor(0.85f, 0.82f, 0.9f, 1f);
        f.draw(b, "WASD: mover   ·   Mouse: mirar   ·   Botão esquerdo: atirar\n"
                        + "Espaço: dash   ·   Q / botão direito: habilidade especial\n"
                        + "Sobreviva às hordas da cripta, da floresta e das ruínas.",
                0, h * 0.74f, w, Align.center, false);
        f.getData().setScale(1.7f);
        f.setColor(1f, 0.9f, 0.5f, 0.55f + 0.45f * Math.abs(MathUtils.sin(time * 3)));
        f.draw(b, "Clique para começar", 0, h * 0.56f, w, Align.center, false);
        f.getData().setScale(1f);
        f.setColor(Color.WHITE);
        b.setColor(Color.WHITE);
        b.end();
    }

    @Override
    public void resize(int width, int height) { viewport.update(width, height, true); }
}
