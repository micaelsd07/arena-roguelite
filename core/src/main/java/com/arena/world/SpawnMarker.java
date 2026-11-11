package com.arena.world;

import com.badlogic.gdx.utils.Pool;

/** Portal que aparece antes do inimigo surgir. */
public class SpawnMarker implements Pool.Poolable {
    public static final float DURATION = 0.9f;
    public float x, y, timer, hpScale;
    public EnemyType type;

    @Override public void reset() { x = y = timer = hpScale = 0; type = null; }
}
