package com.arena.world;

import com.arena.audio.Sfx;
import com.badlogic.gdx.utils.Array;

/**
 * Hordas por orcamento (10 + onda*5). Tipos mais caros so aparecem a partir de certa onda.
 * A cada 5 ondas, um chefe: a onda so termina quando ele morre.
 * Fora isso, a proxima onda comeca quando a atual foi gerada e limpa, ou apos 30 s.
 */
public class WaveManager {
    public static final float WAVE_TIMEOUT = 30f;
    public static final int MAX_ALIVE = 300;

    private final GameWorld world;
    private final Array<EnemyType> candidates = new Array<>(false, 4);

    public int wave;
    public int budget;
    public float waveTime;
    public float bannerTime;
    private float spawnTimer;
    private boolean bossPending;

    WaveManager(GameWorld world) { this.world = world; }

    public boolean isBossWave() { return wave % 5 == 0; }

    void startNextWave() {
        wave++;
        Biome b = Biome.forWave(wave);
        if (b != world.biome) world.changeBiome(b);
        budget = isBossWave() ? 4 + wave : 10 + wave * 5;
        bossPending = isBossWave();
        waveTime = 0f;
        spawnTimer = 0.8f;
        bannerTime = 2.5f;
        world.audio.play(Sfx.WAVE_START);
    }

    void update(float dt) {
        waveTime += dt;
        bannerTime -= dt;

        if (bossPending && waveTime > 1.5f) {
            bossPending = false;
            EnemyType boss = wave == 5 ? EnemyType.KING : wave == 10 ? EnemyType.ALPHA : wave == 15 ? EnemyType.SUPREME
                    : new EnemyType[]{EnemyType.KING, EnemyType.ALPHA, EnemyType.SUPREME}[world.random.nextInt(3)];
            world.spawnMarker(boss, Phys.ARENA_W / 2f, Phys.ARENA_H - 4f, 1f + 0.12f * (wave - 5));
            world.audio.play(Sfx.BOSS_ROAR);
        }

        if (budget > 0) {
            spawnTimer -= dt;
            if (spawnTimer <= 0f) {
                spawnTimer = Math.max(0.15f, 1.1f - wave * 0.05f) * (isBossWave() ? 2.5f : 1f);
                int group = 1 + world.random.nextInt(Math.min(4, 1 + wave / 2));
                for (int i = 0; i < group && budget > 0; i++) spawnOne();
            }
        }

        boolean bossAlive = world.boss != null || bossPending;
        boolean cleared = budget <= 0 && world.enemies.size == 0 && world.markers.size == 0 && !bossAlive
                && !world.hasClosedChest(); // espera o bau do chefe ser aberto
        boolean timeout = waveTime >= WAVE_TIMEOUT && !isBossWave();
        if (cleared || timeout) startNextWave();
    }

    private void spawnOne() {
        if (world.enemies.size + world.markers.size >= MAX_ALIVE) return;
        candidates.clear();
        for (EnemyType t : EnemyType.values()) {
            if (!t.boss && t.minWave <= wave && t.cost <= budget) candidates.add(t);
        }
        if (candidates.isEmpty()) { budget = 0; return; }
        EnemyType type = world.random.nextFloat() < 0.5f ? candidates.first()
                : candidates.get(world.random.nextInt(candidates.size));
        budget -= type.cost;
        world.spawnAtEdge(type, 1f + 0.12f * (wave - 1));
    }
}
