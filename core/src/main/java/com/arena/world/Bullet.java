package com.arena.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

/**
 * Projetil: corpo Box2D kinematic + fixture sensor. A velocidade e recalculada a cada frame
 * para produzir trajetorias (reta, senoidal, teleguiada) e o projetil orbital e posicionado direto.
 */
public class Bullet implements Pool.Poolable {
    public Body body;
    public boolean enemyOwned;
    public boolean orbit;
    public float radius;
    public float damage;
    public float speed;
    public float life;
    public float time;
    public final Vector2 dir = new Vector2();
    public int pierce;
    public int ricochet;
    public float sineAmp;       // amplitude da ondulacao (m)
    public float homing;        // velocidade de curva (rad/s)
    public float knockback;
    public float retargetTimer;
    public Enemy target;
    public int orbitIndex;
    public final Array<Enemy> alreadyHit = new Array<>(false, 4);
    public boolean dead;

    public float x() { return body.getPosition().x; }

    public float y() { return body.getPosition().y; }

    @Override
    public void reset() {
        enemyOwned = orbit = dead = false;
        radius = damage = speed = life = time = sineAmp = homing = retargetTimer = knockback = 0;
        dir.setZero();
        pierce = ricochet = orbitIndex = 0;
        target = null;
        alreadyHit.clear();
        if (body != null) body.setActive(false);
    }
}
