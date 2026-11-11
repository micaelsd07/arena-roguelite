package com.arena.render;

import com.arena.audio.AudioManager;
import com.arena.world.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

/** HUD em pixels de tela: vida, XP, onda, estatisticas de pool, tela de level up e game over. */
public class Hud {
    private final Assets assets;
    private final GlyphLayout layout = new GlyphLayout();
    private final Rectangle card = new Rectangle();

    public Hud(Assets assets) { this.assets = assets; }

    public void render(SpriteBatch batch, GameWorld w, AudioManager audio, float sw, float sh, int hoverCard) {
        BitmapFont font = assets.font;
        Texture px = assets.pixel;
        Player p = w.player;

        // Vida
        bar(batch, 20, sh - 40, 300, 20, p.hp / p.stats.maxHp, new Color(0.9f, 0.25f, 0.3f, 1f));
        // XP
        bar(batch, 20, sh - 66, 300, 12, p.xp / (float) p.xpToNext(), new Color(0.3f, 0.85f, 1f, 1f));
        font.getData().setScale(1.2f);
        font.setColor(Color.WHITE);
        font.draw(batch, (int) Math.ceil(p.hp) + " / " + (int) p.stats.maxHp, 28, sh - 24);
        font.draw(batch, "Nível " + p.level + "   Onda " + w.waves.wave + "   Abates " + w.kills
                + "   Tempo " + formatTime(w.time), 20, sh - 78);

        // Estatisticas tecnicas (mostra o pooling funcionando)
        font.getData().setScale(1f);
        font.setColor(0.7f, 0.8f, 0.9f, 1f);
        int ep = 0;
        for (GameWorld.EnemyPool pool : w.enemyPools) ep += pool.getFree();
        String stats = "FPS " + Gdx.graphics.getFramesPerSecond()
                + "\nInimigos " + w.enemies.size + " (pool livre " + ep + ")"
                + "\nProjéteis " + (w.bullets.size + w.orbitBullets.size) + " (pool livre " + w.bulletPool.getFree() + ")"
                + "\nGemas " + w.gems.size + " (pool livre " + w.gemPool.getFree() + ")"
                + "\nCorpos Box2D " + w.box2d.getBodyCount();
        font.draw(batch, stats, sw - 260, sh - 20, 240, Align.right, false);

        String vol = audio.isMuted() ? "Som: mudo (M)" : "Som: " + Math.round(audio.getMasterVolume() * 100) + "%  (M / - / +)";
        font.draw(batch, vol, sw - 260, 30, 240, Align.right, false);

        // Banner de onda
        if (w.waves.bannerTime > 0f && w.state == GameWorld.State.PLAYING) {
            font.getData().setScale(2.6f);
            font.setColor(1f, 0.9f, 0.4f, Math.min(1f, w.waves.bannerTime));
            font.draw(batch, "ONDA " + w.waves.wave, 0, sh * 0.72f, sw, Align.center, false);
        }

        if (w.state == GameWorld.State.LEVEL_UP) drawLevelUp(batch, w, sw, sh, hoverCard);
        else if (w.state == GameWorld.State.GAME_OVER) drawGameOver(batch, w, sw, sh);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.setColor(Color.WHITE);
    }

    private void drawLevelUp(SpriteBatch batch, GameWorld w, float sw, float sh, int hover) {
        BitmapFont font = assets.font;
        dim(batch, sw, sh, 0.65f);
        font.getData().setScale(2.4f);
        font.setColor(0.4f, 0.95f, 1f, 1f);
        font.draw(batch, "NÍVEL " + w.player.level + "!", 0, sh * 0.78f, sw, Align.center, false);
        font.getData().setScale(1.2f);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Escolha uma melhoria (clique ou 1 / 2 / 3)", 0, sh * 0.78f - 50, sw, Align.center, false);

        for (int i = 0; i < 3; i++) {
            Upgrade u = w.choices[i];
            cardRect(i, sw, sh, card);
            batch.setColor(i == hover ? new Color(0.25f, 0.35f, 0.5f, 1f) : new Color(0.14f, 0.17f, 0.24f, 1f));
            batch.draw(assets.pixel, card.x, card.y, card.width, card.height);
            batch.setColor(i == hover ? new Color(0.5f, 0.9f, 1f, 1f) : new Color(0.3f, 0.4f, 0.55f, 1f));
            outline(batch, card, 3);
            font.getData().setScale(1.1f);
            font.setColor(0.6f, 0.7f, 0.8f, 1f);
            font.draw(batch, "[" + (i + 1) + "]", card.x + 12, card.y + card.height - 12);
            font.getData().setScale(1.5f);
            font.setColor(Color.WHITE);
            font.draw(batch, u.title, card.x + 12, card.y + card.height - 46, card.width - 24, Align.center, true);
            font.getData().setScale(1.1f);
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, u.description, card.x + 16, card.y + card.height - 110, card.width - 32, Align.center, true);
        }
    }

    private void drawGameOver(SpriteBatch batch, GameWorld w, float sw, float sh) {
        BitmapFont font = assets.font;
        dim(batch, sw, sh, 0.75f);
        font.getData().setScale(3f);
        font.setColor(1f, 0.3f, 0.35f, 1f);
        font.draw(batch, "GAME OVER", 0, sh * 0.65f, sw, Align.center, false);
        font.getData().setScale(1.5f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Onda " + w.waves.wave + "   ·   Nível " + w.player.level + "   ·   Abates " + w.kills
                + "   ·   Tempo " + formatTime(w.time), 0, sh * 0.65f - 70, sw, Align.center, false);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Pressione R para jogar de novo", 0, sh * 0.65f - 120, sw, Align.center, false);
    }

    /** Mesmo layout usado para desenhar e para detectar o clique nas cartas. */
    public static Rectangle cardRect(int i, float sw, float sh, Rectangle out) {
        float w = Math.min(280f, (sw - 80f) / 3f), h = 200f, gap = 24f;
        float total = 3 * w + 2 * gap;
        return out.set((sw - total) / 2f + i * (w + gap), sh * 0.5f - h / 2f - 20f, w, h);
    }

    private void bar(SpriteBatch batch, float x, float y, float w, float h, float frac, Color c) {
        batch.setColor(0f, 0f, 0f, 0.6f);
        batch.draw(assets.pixel, x - 2, y - 2, w + 4, h + 4);
        batch.setColor(c);
        batch.draw(assets.pixel, x, y, w * Math.max(0f, Math.min(1f, frac)), h);
    }

    private void dim(SpriteBatch batch, float sw, float sh, float a) {
        batch.setColor(0f, 0f, 0f, a);
        batch.draw(assets.pixel, 0, 0, sw, sh);
    }

    private void outline(SpriteBatch batch, Rectangle r, float t) {
        batch.draw(assets.pixel, r.x, r.y, r.width, t);
        batch.draw(assets.pixel, r.x, r.y + r.height - t, r.width, t);
        batch.draw(assets.pixel, r.x, r.y, t, r.height);
        batch.draw(assets.pixel, r.x + r.width - t, r.y, t, r.height);
    }

    static String formatTime(float t) {
        int s = (int) t;
        return String.format("%d:%02d", s / 60, s % 60);
    }
}
