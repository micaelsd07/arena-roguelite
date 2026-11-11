package com.arena.world;

import com.badlogic.gdx.utils.FloatArray;

/** Eventos visuais emitidos pela logica (particulas, tremor, numeros de dano...). */
public interface WorldEvents {
    default void hit(float x, float y, float dirX, float dirY, boolean enemyHit) { }
    default void damageNumber(float x, float y, float amount, boolean big) { }
    default void enemyDied(Enemy e) { }
    default void explosion(float x, float y, float radius) { }
    default void shake(float trauma) { }
    default void lightning(FloatArray points) { }
    default void dashGhost(float x, float y) { }
    default void wallSpark(float x, float y) { }
    default void playerHurt(float x, float y) { }
    default void chestOpened(float x, float y) { }
    default void biomeChanged(Biome biome) { }
    default void nova(float x, float y) { }
    default void shieldBreak(float x, float y) { }
    default void bossSlam(float x, float y) { }

    WorldEvents NONE = new WorldEvents() { };
}
