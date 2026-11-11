package com.arena.world;

import com.badlogic.gdx.utils.Pool;

/** Meteoro da habilidade especial: sombra de aviso e impacto em area. */
public class Meteor implements Pool.Poolable {
    public float x, y, timer, total, damage, radius;

    @Override public void reset() { x = y = timer = total = damage = radius = 0; }
}
