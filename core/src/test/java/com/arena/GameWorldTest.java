package com.arena;

import com.arena.audio.GameAudio;
import com.arena.audio.Sfx;
import com.arena.world.*;
import com.badlogic.gdx.physics.box2d.Box2D;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.EnumMap;

import static org.junit.Assert.*;

/** Simula partidas inteiras sem janela, com um "bot" que foge, atira, usa dash e a habilidade especial. */
public class GameWorldTest {
    @BeforeClass
    public static void natives() { Box2D.init(); }

    static class Counter implements WorldEvents {
        int deaths, explosions, lightning, biomes, chests;
        @Override public void enemyDied(Enemy e) { deaths++; }
        @Override public void explosion(float x, float y, float r) { explosions++; }
        @Override public void lightning(com.badlogic.gdx.utils.FloatArray p) { lightning++; }
        @Override public void biomeChanged(Biome b) { biomes++; }
        @Override public void chestOpened(float x, float y) { chests++; }
    }

    @Test
    public void fullRunReachesBossesBiomesAndUsesEverySystem() {
        EnumMap<Sfx, Integer> played = new EnumMap<>(Sfx.class);
        GameAudio audio = s -> played.merge(s, 1, Integer::sum);
        Counter events = new Counter();
        GameWorld w = new GameWorld(audio, events, 42L);
        InputState in = new InputState();
        Upgrade[] priority = {Upgrade.SP_METEOR, Upgrade.SP_CHAIN, Upgrade.MULTISHOT, Upgrade.DAMAGE, Upgrade.FIRE_RATE,
                Upgrade.ORBIT, Upgrade.PIERCE, Upgrade.MAX_HP, Upgrade.HOMING, Upgrade.RICOCHET};

        int maxEnemies = 0, maxBullets = 0, levelUps = 0;
        boolean sawBoss = false;
        float seconds = 0;
        // Modo "deus" para o bot conseguir ver as tres fases e os chefes
        while (seconds < 600f && w.waves.wave < 12) {
            if (w.state == GameWorld.State.LEVEL_UP) {
                int pick = 0;
                outer:
                for (Upgrade want : priority)
                    for (int i = 0; i < 3; i++) if (w.choices[i] == want) { pick = i; break outer; }
                w.chooseUpgrade(pick);
                levelUps++;
                continue;
            }
            w.player.hp = w.player.stats.maxHp;
            bot(w, in, seconds);
            w.update(in, Phys.STEP);
            seconds += Phys.STEP;
            sawBoss |= w.boss != null;
            maxEnemies = Math.max(maxEnemies, w.enemies.size);
            maxBullets = Math.max(maxBullets, w.bullets.size);
            assertTrue("inimigos acima do limite", w.enemies.size <= WaveManager.MAX_ALIVE);
            for (Enemy e : w.enemies) assertTrue("inimigo fora da arena", e.x() > -1 && e.x() < Phys.ARENA_W + 1 && e.y() > -1 && e.y() < Phys.ARENA_H + 1);
        }

        System.out.printf("tempo=%.0fs onda=%d bioma=%s nivel=%d abates=%d levelUps=%d maxInimigos=%d maxProjeteis=%d corposBox2D=%d%n",
                seconds, w.waves.wave, w.biome, w.player.level, w.kills, levelUps, maxEnemies, maxBullets, w.box2d.getBodyCount());
        System.out.println("especial=" + w.player.special + " nv" + w.player.specialLevel + " eventos: mortes=" + events.deaths
                + " explosoes=" + events.explosions + " raios=" + events.lightning + " biomas=" + events.biomes + " baus=" + events.chests);
        System.out.println("sons: " + played);

        assertTrue("deveria chegar ao terceiro bioma", w.waves.wave >= 11);
        assertEquals(Biome.RUINS, w.biome);
        assertTrue("deveria ter enfrentado chefe", sawBoss);
        assertEquals("dois chefes derrotados = dois baus", 2, events.chests);
        assertEquals(2, events.biomes);
        assertTrue(played.containsKey(Sfx.DASH) && played.containsKey(Sfx.BOSS_ROAR) && played.containsKey(Sfx.CHEST));
        assertTrue("especial usada", events.explosions > 0 || events.lightning > 0);
        assertTrue("pool de projeteis reaproveitado", w.bulletPool.getFree() > 0);
        assertTrue("corpos Box2D limitados pelo pico", w.box2d.getBodyCount() < maxBullets + maxEnemies + 250);
        w.dispose();
    }

    @Test
    public void idlePlayerEventuallyDies() {
        GameWorld w = new GameWorld(GameAudio.SILENT, WorldEvents.NONE, 7L);
        InputState in = new InputState();
        for (int i = 0; i < 60 * 300 && w.state != GameWorld.State.GAME_OVER; i++) {
            if (w.state == GameWorld.State.LEVEL_UP) w.chooseUpgrade(0);
            w.update(in, Phys.STEP);
        }
        assertEquals(GameWorld.State.GAME_OVER, w.state);
        w.dispose();
    }

    @Test
    public void bulletsRicochetOffObstacles() {
        GameWorld w = new GameWorld(GameAudio.SILENT, WorldEvents.NONE, 3L);
        w.player.stats.ricochet = 5;
        Obstacle o = w.obstacles.first();
        // mira direto no primeiro obstaculo
        InputState in = new InputState();
        w.player.body.setTransform(o.x - 4f, o.y, 0);
        in.aimX = o.x; in.aimY = o.y; in.firing = true;
        int[] ric = {0};
        GameWorld w2 = w; // apenas para legibilidade
        for (int i = 0; i < 30; i++) w2.update(in, Phys.STEP);
        boolean bounced = false;
        for (Bullet b : w.bullets) if (b.ricochet < 5 && b.dir.x < 0) bounced = true;
        assertTrue("alguma flecha deveria ter ricocheteado no obstaculo", bounced);
        w.dispose();
    }

    /** Foge do inimigo mais proximo, atira nele, usa dash quando cercado e a especial quando pronta. */
    private static void bot(GameWorld w, InputState in, float t) {
        Enemy target = null;
        float best = Float.MAX_VALUE;
        for (Enemy e : w.enemies) {
            float d = (e.x() - w.player.x()) * (e.x() - w.player.x()) + (e.y() - w.player.y()) * (e.y() - w.player.y());
            if (d < best) { best = d; target = e; }
        }
        for (Chest c : w.chests) if (!c.isOpen()) { in.moveX = c.x - w.player.x(); in.moveY = c.y - w.player.y(); }
        float cx = Phys.ARENA_W / 2 - w.player.x(), cy = Phys.ARENA_H / 2 - w.player.y();
        in.moveX = (float) Math.cos(t * 0.7) + cx * 0.15f;
        in.moveY = (float) Math.sin(t * 0.7) + cy * 0.15f;
        for (Chest c : w.chests) if (!c.isOpen()) { in.moveX = c.x - w.player.x(); in.moveY = c.y - w.player.y(); }
        in.dash = best < 2f;
        in.special = target != null;
        if (target != null) {
            in.aimX = target.x();
            in.aimY = target.y();
            in.firing = true;
        } else in.firing = false;
    }
}
