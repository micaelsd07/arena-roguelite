package com.arena.world;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Pool;

/** Inimigo reaproveitado via pool. O corpo Box2D e criado uma vez e depois so ativado/desativado. */
public class Enemy implements Pool.Poolable {
    public final EnemyType type;
    public Body body;
    public float hp, maxHp;
    public float shootTimer;
    public float flash;          // tempo do "piscar branco" ao levar dano
    public int orbitContacts;    // quantos projeteis orbitais estao encostando
    public float orbitHitTimer;  // cooldown de dano dos orbitais
    public boolean dead;

    public Enemy(EnemyType type) { this.type = type; }

    public float x() { return body.getPosition().x; }

    public float y() { return body.getPosition().y; }

    @Override
    public void reset() {
        hp = maxHp = 0;
        shootTimer = 0;
        flash = 0;
        orbitContacts = 0;
        orbitHitTimer = 0;
        dead = false;
        if (body != null) body.setActive(false);
    }
}
