package com.arena.render;

import com.arena.audio.AudioManager;
import com.arena.world.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;

/** HUD em pixels de tela. */
public class Hud {
    private static final Color PANEL = new Color(0.07f, 0.06f, 0.10f, 0.85f);
    private static final Color BORDER = new Color(0.36f, 0.30f, 0.46f, 1f);
    private static final Color GOLD = new Color(1f, 0.85f, 0.4f, 1f);

    private final Assets assets;
    private final Fx fx;
    private final Rectangle card = new Rectangle();
    private final Vector3 proj = new Vector3();
    public boolean showStats = true;

    public Hud(Assets assets, Fx fx) { this.assets = assets; this.fx = fx; }

    public void render(SpriteBatch batch, GameWorld w, AudioManager audio, OrthographicCamera worldCam,
                       int vx, int vy, int vw, int vh, float sw, float sh, int hoverCard) {
        BitmapFont font = assets.font;
        Player p = w.player;

        damageNumbers(batch, worldCam, vx, vy, vw, vh);

        // Vinheta vermelha ao levar dano
        if (fx.hurtFlash > 0f) {
            batch.setColor(0.8f, 0f, 0f, 0.28f * fx.hurtFlash);
            batch.draw(assets.pixel, 0, 0, sw, sh);
        }

        // ---------------- vida e XP
        panel(batch, 12, sh - 92, 360, 80);
        batch.setColor(1, 1, 1, 1);
        batch.draw(assets.region("icon_max_hp"), 18, sh - 66, 46, 46);
        bar(batch, 70, sh - 50, 288, 20, p.hp / p.stats.maxHp, new Color(0.85f, 0.18f, 0.22f, 1f));
        bar(batch, 70, sh - 74, 288, 10, p.xp / (float) p.xpToNext(), new Color(0.3f, 0.8f, 1f, 1f));
        font.getData().setScale(1.15f);
        font.setColor(Color.WHITE);
        font.draw(batch, (int) Math.ceil(p.hp) + " / " + (int) p.stats.maxHp, 78, sh - 33);
        font.getData().setScale(1f);
        font.setColor(0.7f, 0.9f, 1f, 1f);
        font.draw(batch, "Nv " + p.level, 22, sh - 70);

        font.getData().setScale(1.1f);
        font.setColor(GOLD);
        font.draw(batch, "Onda " + w.waves.wave + "  ·  " + w.biome.title, 16, sh - 100);
        font.setColor(0.8f, 0.8f, 0.85f, 1f);
        font.getData().setScale(1f);
        font.draw(batch, "Abates " + w.kills + "   Tempo " + formatTime(w.time), 16, sh - 122);

        // ---------------- habilidades
        float slot = 60, gap = 12, total = slot * 2 + gap, bx = sw / 2f - total / 2f, by = 18;
        abilitySlot(batch, bx, by, slot, assets.region("icon_dash"), "Espaço",
                Math.max(0f, p.dashCooldown) / Player.DASH_COOLDOWN, 0);
        TextureRegion sp = p.special != null ? assets.region(p.special.icon) : null;
        abilitySlot(batch, bx + slot + gap, by, slot, sp, "Q",
                p.special != null ? Math.max(0f, p.specialCooldown) / p.special.cooldown(p.specialLevel) : 0f,
                p.special != null ? p.specialLevel : 0);

        // ---------------- melhorias obtidas
        float ix = 14, iy = 14;
        for (Upgrade u : Upgrade.values()) {
            int n = p.taken[u.ordinal()];
            if (n == 0 || u.special != null) continue;
            batch.setColor(1, 1, 1, 1);
            batch.draw(assets.region(u.icon), ix, iy, 34, 34);
            if (n > 1) {
                font.getData().setScale(0.9f);
                font.setColor(Color.WHITE);
                font.draw(batch, "x" + n, ix + 20, iy + 12);
            }
            ix += 38;
            if (ix > sw / 2f - total / 2f - 50) { ix = 14; iy += 38; }
        }

        // ---------------- chefe
        if (w.boss != null) {
            float bw = Math.max(240, Math.min(560, sw - 820)), bxx = sw / 2f - bw / 2f;
            panel(batch, bxx - 8, sh - 62, bw + 16, 50);
            bar(batch, bxx, sh - 52, bw, 14, w.boss.hp / w.boss.maxHp, new Color(0.65f, 0.2f, 0.85f, 1f));
            font.getData().setScale(1.2f);
            font.setColor(GOLD);
            font.draw(batch, w.boss.type.displayName(), 0, sh - 20, sw, Align.center, false);
        }

        // ---------------- estatisticas tecnicas (F3)
        if (showStats) {
            font.getData().setScale(0.9f);
            font.setColor(0.6f, 0.7f, 0.8f, 0.85f);
            int ep = 0;
            for (GameWorld.EnemyPool pool : w.enemyPools) ep += pool.getFree();
            String s = "FPS " + Gdx.graphics.getFramesPerSecond()
                    + "\nInimigos " + w.enemies.size + " (pool livre " + ep + ")"
                    + "\nProjéteis " + (w.bullets.size + w.orbitBullets.size) + " (pool livre " + w.bulletPool.getFree() + ")"
                    + "\nGemas " + w.gems.size + " (pool livre " + w.gemPool.getFree() + ")"
                    + "\nPartículas " + fx.particles.active.size + " (pool livre " + fx.particles.freeCount() + ")"
                    + "\nCorpos Box2D " + w.box2d.getBodyCount() + "   F3: ocultar";
            font.draw(batch, s, sw - 300, sh - 16, 285, Align.right, false);
        }
        font.getData().setScale(0.9f);
        font.setColor(0.6f, 0.6f, 0.7f, 0.8f);
        String vol = audio.isMuted() ? "Som: mudo (M)" : "Som " + Math.round(audio.getMasterVolume() * 100) + "%  (M / - / +)";
        font.draw(batch, vol, sw - 260, 24, 245, Align.right, false);

        // ---------------- banners
        if (fx.biomeBanner > 0f && fx.bannerBiome != null) {
            float a = Math.min(1f, fx.biomeBanner) * Math.min(1f, (3.5f - fx.biomeBanner) * 2f);
            font.getData().setScale(3f);
            font.setColor(0.9f, 0.85f, 1f, a);
            font.draw(batch, fx.bannerBiome.title, 0, sh * 0.68f, sw, Align.center, false);
        } else if (w.waves.bannerTime > 0f && w.state == GameWorld.State.PLAYING) {
            font.getData().setScale(2.4f);
            font.setColor(GOLD.r, GOLD.g, GOLD.b, Math.min(1f, w.waves.bannerTime));
            String t = w.waves.isBossWave() ? "ONDA " + w.waves.wave + " — CHEFE!" : "ONDA " + w.waves.wave;
            font.draw(batch, t, 0, sh * 0.7f, sw, Align.center, false);
        }
        if (w.biomeFade > 0f) {
            batch.setColor(0, 0, 0, Math.min(1f, w.biomeFade));
            batch.draw(assets.pixel, 0, 0, sw, sh);
        }

        if (w.state == GameWorld.State.LEVEL_UP) drawLevelUp(batch, w, sw, sh, hoverCard);
        else if (w.state == GameWorld.State.GAME_OVER) drawGameOver(batch, w, sw, sh);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.setColor(Color.WHITE);
    }

    private void damageNumbers(SpriteBatch batch, OrthographicCamera cam, int vx, int vy, int vw, int vh) {
        BitmapFont font = assets.font;
        for (Fx.Number n : fx.numbers) {
            proj.set(n.x, n.y, 0);
            cam.project(proj, vx, vy, vw, vh);
            float a = Math.min(1f, n.life * 3f);
            font.getData().setScale(n.big ? 1.6f : 1.1f);
            font.setColor(0, 0, 0, a * 0.8f);
            String s = Integer.toString(Math.round(n.value));
            font.draw(batch, s, proj.x - 19, proj.y - 1, 40, Align.center, false);
            if (n.big) font.setColor(1f, 0.75f, 0.2f, a); else font.setColor(1f, 1f, 1f, a);
            font.draw(batch, s, proj.x - 20, proj.y, 40, Align.center, false);
        }
    }

    private void abilitySlot(SpriteBatch batch, float x, float y, float size, TextureRegion icon, String key, float cd, int level) {
        panel(batch, x - 4, y - 4, size + 8, size + 8);
        batch.setColor(1, 1, 1, icon == null ? 0f : 1f);
        if (icon != null) batch.draw(icon, x, y, size, size);
        if (cd > 0f) {
            batch.setColor(0, 0, 0, 0.65f);
            batch.draw(assets.pixel, x, y, size, size * cd);
        }
        BitmapFont font = assets.font;
        font.getData().setScale(0.9f);
        font.setColor(icon == null ? Color.GRAY : Color.WHITE);
        font.draw(batch, key, x, y + size + 20, size, Align.center, false);
        if (level > 0) {
            font.setColor(GOLD);
            font.draw(batch, "Nv" + level, x + 2, y + 14);
        }
    }

    private void drawLevelUp(SpriteBatch batch, GameWorld w, float sw, float sh, int hover) {
        BitmapFont font = assets.font;
        dim(batch, sw, sh, 0.7f);
        font.getData().setScale(2.4f);
        font.setColor(0.5f, 0.9f, 1f, 1f);
        font.draw(batch, "NÍVEL " + w.player.level + "!", 0, sh * 0.82f, sw, Align.center, false);
        font.getData().setScale(1.2f);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Escolha uma melhoria (clique ou 1 / 2 / 3)", 0, sh * 0.82f - 48, sw, Align.center, false);

        for (int i = 0; i < 3; i++) {
            Upgrade u = w.choices[i];
            cardRect(i, sw, sh, card);
            boolean hov = i == hover;
            boolean sp = u.special != null;
            batch.setColor(hov ? new Color(0.20f, 0.17f, 0.30f, 0.97f) : new Color(0.10f, 0.08f, 0.15f, 0.95f));
            batch.draw(assets.pixel, card.x, card.y, card.width, card.height);
            batch.setColor(hov ? GOLD : sp ? new Color(0.75f, 0.45f, 1f, 1f) : BORDER);
            outline(batch, card, hov ? 3 : 2);
            float is = 88;
            batch.setColor(1, 1, 1, 1);
            batch.draw(assets.region(u.icon), card.x + card.width / 2 - is / 2, card.y + card.height - is - 34, is, is);
            font.getData().setScale(1f);
            font.setColor(0.6f, 0.6f, 0.7f, 1f);
            font.draw(batch, "[" + (i + 1) + "]", card.x + 10, card.y + card.height - 10);
            if (sp) {
                font.setColor(0.8f, 0.55f, 1f, 1f);
                font.draw(batch, "HABILIDADE", card.x, card.y + card.height - 10, card.width - 10, Align.right, false);
            }
            font.getData().setScale(1.35f);
            font.setColor(Color.WHITE);
            font.draw(batch, u.cardTitle(w.player), card.x + 10, card.y + card.height - is - 48, card.width - 20, Align.center, true);
            font.getData().setScale(1.05f);
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, u.description, card.x + 14, card.y + 70, card.width - 28, Align.center, true);
            int n = w.player.taken[u.ordinal()];
            if (n > 0 && !sp) {
                font.setColor(GOLD);
                font.draw(batch, "Tem: " + n, card.x, card.y + 22, card.width, Align.center, false);
            }
        }
    }

    private void drawGameOver(SpriteBatch batch, GameWorld w, float sw, float sh) {
        BitmapFont font = assets.font;
        dim(batch, sw, sh, 0.75f);
        font.getData().setScale(3f);
        font.setColor(1f, 0.3f, 0.35f, 1f);
        font.draw(batch, "VOCÊ CAIU", 0, sh * 0.66f, sw, Align.center, false);
        font.getData().setScale(1.4f);
        font.setColor(Color.WHITE);
        font.draw(batch, "Onda " + w.waves.wave + " (" + w.biome.title + ")   ·   Nível " + w.player.level
                + "   ·   Abates " + w.kills + "   ·   Tempo " + formatTime(w.time), 0, sh * 0.66f - 70, sw, Align.center, false);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Pressione R para jogar de novo", 0, sh * 0.66f - 120, sw, Align.center, false);
    }

    /** Mesmo layout para desenhar e para detectar o clique nas cartas. */
    public static Rectangle cardRect(int i, float sw, float sh, Rectangle out) {
        float w = Math.min(260f, (sw - 80f) / 3f), h = 300f, gap = 24f;
        float total = 3 * w + 2 * gap;
        return out.set((sw - total) / 2f + i * (w + gap), sh * 0.44f - h / 2f, w, h);
    }

    private void panel(SpriteBatch batch, float x, float y, float w, float h) {
        batch.setColor(PANEL);
        batch.draw(assets.pixel, x, y, w, h);
        batch.setColor(BORDER);
        card.set(x, y, w, h);
        outline(batch, card, 2);
    }

    private void bar(SpriteBatch batch, float x, float y, float w, float h, float frac, Color c) {
        batch.setColor(0.02f, 0.02f, 0.04f, 1f);
        batch.draw(assets.pixel, x - 2, y - 2, w + 4, h + 4);
        frac = Math.max(0f, Math.min(1f, frac));
        batch.setColor(c.r * 0.6f, c.g * 0.6f, c.b * 0.6f, 1f);
        batch.draw(assets.pixel, x, y, w * frac, h);
        batch.setColor(c);
        batch.draw(assets.pixel, x, y + h * 0.35f, w * frac, h * 0.65f);
        batch.setColor(1, 1, 1, 0.25f);
        batch.draw(assets.pixel, x, y + h - 2, w * frac, 2);
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
