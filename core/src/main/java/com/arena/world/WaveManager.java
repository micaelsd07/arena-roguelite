package com.arena.world;

import com.arena.audio.Sfx;
import com.badlogic.gdx.utils.Array;

/**
 * Hordas por orcamento: cada onda recebe 10 + onda*5 pontos; cada inimigo custa pontos e
 * tipos mais caros so aparecem a partir de certa onda. A proxima onda comeca quando a atual
 * foi toda gerada e limpa, ou apos 30 s.
 */
public class WaveManager {
    public static final float WAVE_TIMEOUT = 30f;
    public static final int MAX_ALIVE = 300;

    private final GameWorld world;
    private final Array<EnemyType> candidates = new Array<>(false, 3);

    public int wave;
    public int budget;
    public float waveTime;
    public float bannerTime;
    private float spawnTimer;

    WaveManager(GameWorld world) { this.world = world; }

    void startNextWave() {
        wave++;
        budget = 10 + wave * 5;
        waveTime = 0f;
        spawnTimer = 0.5f;
        bannerTime = 2.5f;
        world.audio.play(Sfx.WAVE_START);
    }

    void update(float dt) {
        waveTime += dt;
        bannerTime -= dt;

        if (budget > 0) {
            spawnTimer -= dt;
            if (spawnTimer <= 0f) {
                spawnTimer = Math.max(0.12f, 1.0f - wave * 0.05f);
                int group = 1 + world.random.nextInt(Math.min(4, 1 + wave / 2));
                for (int i = 0; i < group && budget > 0; i++) spawnOne();
            }
        }

        boolean cleared = budget <= 0 && world.enemies.size == 0;
        if (cleared || waveTime >= WAVE_TIMEOUT) startNextWave();
    }

    private void spawnOne() {
        if (world.enemies.size >= MAX_ALIVE) return;
        candidates.clear();
        for (EnemyType t : EnemyType.values()) {
            if (t.minWave <= wave && t.cost <= budget) candidates.add(t);
        }
        if (candidates.isEmpty()) { budget = 0; return; }
        // Peso: chasers sao mais comuns
        EnemyType type = world.random.nextFloat() < 0.55f ? candidates.first()
                : candidates.get(world.random.nextInt(candidates.size));
        budget -= type.cost;
        world.spawnEnemy(type, 1f + 0.12f * (wave - 1));
    }
}
