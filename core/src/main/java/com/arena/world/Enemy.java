package com.arena.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Pool;

/** Inimigo reaproveitado via pool. O corpo Box2D e criado uma vez e depois so ativado/desativado. */
public class Enemy implements Pool.Poolable {
    public final EnemyType type;
    public Body body;
    public float hp, maxHp;
    public float flash;          // piscar branco ao levar dano
    public float age;            // tempo de vida (animacao de surgimento)
    public float animTime;
    public boolean facingLeft;
    public int orbitContacts;
    public float orbitHitTimer;
    public float slowTimer;      // congelado pelo escudo de gelo
    public final Vector2 knock = new Vector2();
    public final Vector2 lockedDir = new Vector2();
    // Maquina de estados simples (dasher e chefes)
    public int state;
    public float stateTimer;
    public float attackTimer;
    public float castTimer;      // mostra o frame de conjuracao
    public float patternAngle;
    public int counter;
    public boolean dead;

    public Enemy(EnemyType type) { this.type = type; }

    public float x() { return body.getPosition().x; }

    public float y() { return body.getPosition().y; }

    @Override
    public void reset() {
        hp = maxHp = flash = age = animTime = orbitHitTimer = slowTimer = 0;
        stateTimer = attackTimer = castTimer = patternAngle = 0;
        state = counter = orbitContacts = 0;
        facingLeft = dead = false;
        knock.setZero();
        lockedDir.setZero();
        if (body != null) body.setActive(false);
    }
}
