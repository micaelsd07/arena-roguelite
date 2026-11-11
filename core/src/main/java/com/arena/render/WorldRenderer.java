package com.arena.render;

import com.arena.world.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** Desenha o mundo em unidades de metro. Tudo num unico SpriteBatch (centenas de sprites = poucos draw calls). */
public class WorldRenderer {
    private static final Color FLOOR = new Color(0.09f, 0.10f, 0.13f, 1f);
    private static final Color GRID = new Color(0.14f, 0.16f, 0.20f, 1f);
    private static final Color WALL = new Color(0.35f, 0.42f, 0.55f, 1f);
    private static final Color PLAYER = new Color(0.35f, 0.95f, 0.55f, 1f);
    private static final Color PLAYER_BULLET = new Color(1f, 0.95f, 0.45f, 1f);
    private static final Color ENEMY_BULLET = new Color(1f, 0.35f, 0.85f, 1f);
    private static final Color ORBIT = new Color(0.45f, 0.85f, 1f, 1f);
    private static final Color GEM = new Color(0.30f, 0.90f, 1f, 1f);

    private final Assets assets;

    public WorldRenderer(Assets assets) { this.assets = assets; }

    public void render(SpriteBatch batch, GameWorld w, float aimX, float aimY) {
        Texture px = assets.pixel;
        batch.setColor(FLOOR);
        batch.draw(px, 0, 0, Phys.ARENA_W, Phys.ARENA_H);
        batch.setColor(GRID);
        for (int x = 2; x < Phys.ARENA_W; x += 2) batch.draw(px, x - 0.02f, 0, 0.04f, Phys.ARENA_H);
        for (int y = 2; y < Phys.ARENA_H; y += 2) batch.draw(px, 0, y - 0.02f, Phys.ARENA_W, 0.04f);
        batch.setColor(WALL);
        float t = 0.25f;
        batch.draw(px, -t, -t, Phys.ARENA_W + 2 * t, t);
        batch.draw(px, -t, Phys.ARENA_H, Phys.ARENA_W + 2 * t, t);
        batch.draw(px, -t, 0, t, Phys.ARENA_H);
        batch.draw(px, Phys.ARENA_W, 0, t, Phys.ARENA_H);

        // Ima
        batch.setColor(GEM.r, GEM.g, GEM.b, 0.12f);
        circle(batch, assets.ring, w.player.x(), w.player.y(), w.player.stats.magnet);

        batch.setColor(GEM);
        for (Gem g : w.gems) {
            float r = 0.12f + 0.03f * Math.min(g.value, 5);
            circle(batch, assets.circle, g.x, g.y, r);
        }

        for (Enemy e : w.enemies) {
            if (e.flash > 0f) batch.setColor(Color.WHITE);
            else batch.setColor(e.type.color);
            circle(batch, assets.circle, e.x(), e.y(), e.type.radius);
            if (e.hp < e.maxHp && e.type != EnemyType.CHASER) {
                float bw = e.type.radius * 2f, by = e.y() + e.type.radius + 0.15f;
                batch.setColor(0, 0, 0, 0.6f);
                batch.draw(px, e.x() - bw / 2, by, bw, 0.1f);
                batch.setColor(e.type.color);
                batch.draw(px, e.x() - bw / 2, by, bw * Math.max(0, e.hp / e.maxHp), 0.1f);
            }
        }

        for (Bullet b : w.bullets) {
            batch.setColor(b.enemyOwned ? ENEMY_BULLET : PLAYER_BULLET);
            circle(batch, assets.circle, b.x(), b.y(), b.radius);
        }
        batch.setColor(ORBIT);
        for (Bullet b : w.orbitBullets) circle(batch, assets.circle, b.x(), b.y(), b.radius);

        // Jogador pisca durante a invencibilidade
        Player p = w.player;
        boolean visible = p.invulnerable <= 0f || ((int) (p.invulnerable * 20f)) % 2 == 0;
        if (visible) {
            batch.setColor(PLAYER);
            circle(batch, assets.circle, p.x(), p.y(), Player.RADIUS);
            // "cano" apontando para a mira
            float dx = aimX - p.x(), dy = aimY - p.y();
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 0.01f) {
                batch.setColor(0.1f, 0.35f, 0.2f, 1f);
                circle(batch, assets.circle, p.x() + dx / len * 0.45f, p.y() + dy / len * 0.45f, 0.18f);
            }
        }
        // Mira
        batch.setColor(1f, 1f, 1f, 0.7f);
        circle(batch, assets.ring, aimX, aimY, 0.3f);
        batch.setColor(Color.WHITE);
    }

    private static void circle(SpriteBatch batch, Texture tex, float x, float y, float r) {
        batch.draw(tex, x - r, y - r, r * 2f, r * 2f);
    }
}
