package com.arena.world;

import com.badlogic.gdx.utils.Pool;

/** Gema de XP. Nao precisa de fisica: so segue o jogador quando entra no raio do ima. */
public class Gem implements Pool.Poolable {
    public float x, y, vx, vy;
    public int value;
    public boolean attracted;

    @Override
    public void reset() {
        x = y = vx = vy = 0;
        value = 0;
        attracted = false;
    }
}
