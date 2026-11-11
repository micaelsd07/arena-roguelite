package com.arena;

import com.arena.audio.GameAudio;
import com.arena.audio.Sfx;
import com.arena.world.*;
import com.badlogic.gdx.physics.box2d.Box2D;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.EnumMap;

import static org.junit.Assert.*;

/** Simula partidas inteiras sem janela, com um "bot" mirando no inimigo mais proximo. */
public class GameWorldTest {
    @BeforeClass
    public static void natives() { Box2D.init(); }

    @Test
    public void botSurvivesSeveralWavesAndPoolsAreReused() {
        EnumMap<Sfx, Integer> played = new EnumMap<>(Sfx.class);
        GameAudio audio = s -> played.merge(s, 1, Integer::sum);
        GameWorld w = new GameWorld(audio, 42L);
        InputState in = new InputState();
        Upgrade[] priority = {Upgrade.MULTISHOT, Upgrade.ORBIT, Upgrade.PIERCE, Upgrade.RICOCHET, Upgrade.HOMING, Upgrade.SINE};

        int maxEnemies = 0, maxBullets = 0, levelUps = 0;
        float seconds = 0;
        while (seconds < 240f && w.state != GameWorld.State.GAME_OVER) {
            if (w.state == GameWorld.State.LEVEL_UP) {
                int pick = 0;
                outer:
                for (Upgrade want : priority)
                    for (int i = 0; i < 3; i++) if (w.choices[i] == want) { pick = i; break outer; }
                w.chooseUpgrade(pick);
                levelUps++;
                continue;
            }
            bot(w, in, seconds);
            w.update(in, Phys.STEP);
            seconds += Phys.STEP;
            maxEnemies = Math.max(maxEnemies, w.enemies.size);
            maxBullets = Math.max(maxBullets, w.bullets.size);
            assertTrue("inimigos acima do limite", w.enemies.size <= WaveManager.MAX_ALIVE);
        }

        int bodies = w.box2d.getBodyCount();
        System.out.printf("tempo=%.0fs onda=%d nivel=%d abates=%d levelUps=%d maxInimigos=%d maxProjeteis=%d corposBox2D=%d%n",
                seconds, w.waves.wave, w.player.level, w.kills, levelUps, maxEnemies, maxBullets, bodies);
        System.out.println("stats: multishot=" + w.player.stats.multishot + " orbit=" + w.player.stats.orbitCount
                + " pierce=" + w.player.stats.pierce + " ricochet=" + w.player.stats.ricochet
                + " homing=" + w.player.stats.homingLevel + " sine=" + w.player.stats.sineLevel);
        System.out.println("sons: " + played);

        assertTrue("deveria passar de varias ondas", w.waves.wave >= 4);
        assertTrue("deveria subir de nivel", levelUps >= 3);
        assertTrue("abates", w.kills > 30);
        // Pooling: corpos criados ficam bem abaixo do total de projeteis disparados
        assertTrue("pool de projeteis deveria ter objetos livres", w.bulletPool.getFree() > 0);
        assertTrue("corpos Box2D limitados pelo pico, nao pelo total", bodies < maxBullets + maxEnemies + 200);
        assertTrue(played.containsKey(Sfx.SHOOT) && played.containsKey(Sfx.PICKUP)
                && played.containsKey(Sfx.LEVEL_UP) && played.containsKey(Sfx.WAVE_START)
                && played.containsKey(Sfx.DEATH_CHASER));
        w.dispose();
    }

    @Test
    public void idlePlayerEventuallyDies() {
        GameWorld w = new GameWorld(GameAudio.SILENT, 7L);
        InputState in = new InputState();
        for (int i = 0; i < 60 * 300 && w.state != GameWorld.State.GAME_OVER; i++) {
            if (w.state == GameWorld.State.LEVEL_UP) w.chooseUpgrade(0);
            w.update(in, Phys.STEP);
        }
        assertEquals(GameWorld.State.GAME_OVER, w.state);
        w.dispose();
    }

    /** Foge do inimigo mais proximo (com desvio das paredes) e atira nele. */
    private static void bot(GameWorld w, InputState in, float t) {
        Enemy target = null;
        float best = Float.MAX_VALUE;
        for (Enemy e : w.enemies) {
            float d = (e.x() - w.player.x()) * (e.x() - w.player.x()) + (e.y() - w.player.y()) * (e.y() - w.player.y());
            if (d < best) { best = d; target = e; }
        }
        float cx = Phys.ARENA_W / 2 - w.player.x(), cy = Phys.ARENA_H / 2 - w.player.y();
        in.moveX = (float) Math.cos(t * 0.7) + cx * 0.15f;
        in.moveY = (float) Math.sin(t * 0.7) + cy * 0.15f;
        if (target != null) {
            in.aimX = target.x();
            in.aimY = target.y();
            in.firing = true;
            if (best < 16f) { in.moveX -= (target.x() - w.player.x()); in.moveY -= (target.y() - w.player.y()); }
        } else in.firing = false;
    }
}
