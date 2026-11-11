package com.arena.world;

/** Habilidades especiais ativas (Q ou botao direito). Uma equipada por vez, com niveis. */
public enum Special {
    ARROW_NOVA("Chuva de Flechas", "icon_sp_arrows", 8f),
    ICE_SHIELD("Escudo de Gelo", "icon_sp_ice", 15f),
    CHAIN_LIGHTNING("Raio em Cadeia", "icon_sp_chain", 5f),
    METEORS("Chuva de Meteoros", "icon_sp_meteor", 12f);

    public final String title, icon;
    public final float baseCooldown;

    Special(String title, String icon, float baseCooldown) {
        this.title = title; this.icon = icon; this.baseCooldown = baseCooldown;
    }

    public float cooldown(int level) { return baseCooldown * (float) Math.pow(0.88, level - 1); }
}
