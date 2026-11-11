package com.arena.world;

import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.Array;

/** Os tres biomas: nome, luz ambiente, tom dos inimigos e layout de obstaculos/decoracao. */
public enum Biome {
    CRYPT("Cripta Esquecida", "crypt", 0.20f, 0.18f, 0.28f, 1f, 1f, 1f),
    FOREST("Floresta Corrompida", "forest", 0.20f, 0.27f, 0.30f, 0.86f, 1f, 0.9f),
    RUINS("Ruínas Sombrias", "ruins", 0.33f, 0.27f, 0.27f, 1f, 0.9f, 0.82f);

    public final String title, key;
    public final float ambR, ambG, ambB;
    public final float tintR, tintG, tintB;

    Biome(String title, String key, float ambR, float ambG, float ambB, float tintR, float tintG, float tintB) {
        this.title = title; this.key = key;
        this.ambR = ambR; this.ambG = ambG; this.ambB = ambB;
        this.tintR = tintR; this.tintG = tintG; this.tintB = tintB;
    }

    public static Biome forWave(int wave) { return wave <= 5 ? CRYPT : wave <= 10 ? FOREST : RUINS; }

    /** Monta obstaculos e decoracao. Centro da arena (spawn do jogador) fica livre. */
    public void build(Array<Obstacle> obstacles, Array<Decor> decor, RandomXS128 r) {
        float W = Phys.ARENA_W, H = Phys.ARENA_H;
        switch (this) {
            case CRYPT:
                for (float[] p : new float[][]{{9, 7}, {9, 18}, {31, 7}, {31, 18}, {15, 4}, {25, 21}})
                    obstacles.add(new Obstacle(p[0], p[1], 0.45f, 0, "prop_pillar", 1));
                for (float[] p : new float[][]{{20, 3.6f}, {20, 21.4f}, {4.5f, 12.5f}, {35.5f, 12.5f}})
                    obstacles.add(new Obstacle(p[0], p[1], 0.95f, 0.45f, "prop_sarcophagus", 1));
                decor.add(Decor.of(Decor.Layer.FLOOR, "decal_rug", 0, 20, 12.5f));
                decor.add(Decor.of(Decor.Layer.FLOOR, "decal_rug", 1, 13, 9));
                decor.add(Decor.of(Decor.Layer.FLOOR, "decal_rug", 1, 27, 16));
                for (int i = 0; i < 14; i++) decor.add(Decor.of(Decor.Layer.FLOOR, "decal_crack", -1, rx(r), ry(r)));
                for (int i = 0; i < 9; i++) decor.add(Decor.of(Decor.Layer.PROP, "prop_bones", -1, rx(r), ry(r)));
                for (float x = 3; x < W; x += 6)
                    decor.add(new Decor(Decor.Layer.WALL, "prop_torch", -1, 3, x, H + 0.55f, 5.5f, 1f, 0.6f, 0.25f, true));
                break;
            case FOREST:
                for (float[] p : new float[][]{{7, 6}, {33, 19}, {9, 19.5f}, {31, 5.5f}, {20, 22}, {20, 3.2f}})
                    obstacles.add(new Obstacle(p[0], p[1], 0.55f, 0, "prop_tree", 1));
                for (float[] p : new float[][]{{14, 7}, {26, 18}, {4, 12.5f}, {36, 12}, {14, 19}, {27, 7.5f}})
                    obstacles.add(new Obstacle(p[0], p[1], 0.55f, 0, "prop_rock0", 1));
                for (int i = 0; i < 5; i++) decor.add(Decor.of(Decor.Layer.FLOOR, "decal_puddle", -1, rx(r), ry(r)));
                for (int i = 0; i < 8; i++) decor.add(Decor.of(Decor.Layer.FLOOR, "decal_roots", -1, rx(r), ry(r)));
                for (int i = 0; i < 16; i++) {
                    boolean blue = r.nextBoolean();
                    decor.add(new Decor(Decor.Layer.PROP, "prop_mushroom", blue ? 0 : 1, 1, rx(r), ry(r), 2.6f,
                            blue ? 0.3f : 0.75f, blue ? 0.7f : 0.35f, 1f, false));
                }
                break;
            case RUINS:
                for (float[] p : new float[][]{{8, 8}, {32, 17}, {12, 19}, {28, 6}})
                    obstacles.add(new Obstacle(p[0], p[1], 1.05f, 0.4f, "prop_ruinwall", 1));
                for (float[] p : new float[][]{{16, 5.5f}, {24, 19.5f}, {5, 17}, {35, 8}})
                    obstacles.add(new Obstacle(p[0], p[1], 0.4f, 0, "prop_column", 1));
                for (float[] p : new float[][]{{20, 4.2f}, {20, 21}, {4.5f, 6}, {35.5f, 19}})
                    obstacles.add(new Obstacle(p[0], p[1], 0.45f, 0, "prop_campfire", 3));
                for (float[] p : new float[][]{{20, 4.2f}, {20, 21}, {4.5f, 6}, {35.5f, 19}})
                    decor.add(new Decor(Decor.Layer.FLOOR, "", -1, 1, p[0], p[1] + 0.3f, 7f, 1f, 0.55f, 0.2f, true));
                for (float[] p : new float[][]{{14, 14}, {27, 11}, {36, 3}, {3, 22}})
                    obstacles.add(new Obstacle(p[0], p[1], 0.5f, 0, "prop_rock1", 1));
                for (int i = 0; i < 6; i++) decor.add(Decor.of(Decor.Layer.FLOOR, "decal_rift", -1, rx(r), ry(r)));
                for (int i = 0; i < 12; i++) decor.add(Decor.of(Decor.Layer.PROP, "prop_shrub", -1, rx(r), ry(r)));
                break;
        }
    }

    private static float rx(RandomXS128 r) { return 2f + r.nextFloat() * (Phys.ARENA_W - 4f); }

    private static float ry(RandomXS128 r) { return 1.5f + r.nextFloat() * (Phys.ARENA_H - 3f); }
}
