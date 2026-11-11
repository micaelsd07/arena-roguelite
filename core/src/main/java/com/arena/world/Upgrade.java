package com.arena.world;

/** Melhorias oferecidas ao subir de nivel. Todas acumulam; as especiais equipam/evoluem a habilidade. */
public enum Upgrade {
    DAMAGE("Dano +25%", "Flechas causam mais dano.", "icon_damage") {
        public void apply(Player p) { p.stats.damage *= 1.25f; }
    },
    FIRE_RATE("Cadência +20%", "Atira mais rápido.", "icon_fire_rate") {
        public void apply(Player p) { p.stats.fireRate *= 1.20f; }
    },
    BULLET_SPEED("Flechas velozes", "Projéteis 20% mais rápidos.", "icon_bullet_speed") {
        public void apply(Player p) { p.stats.bulletSpeed *= 1.20f; }
    },
    MULTISHOT("Multishot", "+1 flecha em leque.", "icon_multishot") {
        public void apply(Player p) { p.stats.multishot++; }
    },
    PIERCE("Perfuração", "Atravessa +1 inimigo.", "icon_pierce") {
        public void apply(Player p) { p.stats.pierce++; }
    },
    RICOCHET("Ricochete", "Quica em paredes e obstáculos +1 vez.", "icon_ricochet") {
        public void apply(Player p) { p.stats.ricochet++; }
    },
    SINE("Trajetória senoidal", "A flecha ondula e cobre mais área.", "icon_sine") {
        public void apply(Player p) { p.stats.sineLevel++; }
    },
    HOMING("Teleguiada", "A flecha curva até o inimigo mais próximo.", "icon_homing") {
        public void apply(Player p) { p.stats.homingLevel++; }
    },
    ORBIT("Órbita", "+1 flecha encantada girando ao seu redor.", "icon_orbit") {
        public void apply(Player p) { p.stats.orbitCount++; }
    },
    MOVE_SPEED("Velocidade +10%", "Você se move mais rápido.", "icon_move_speed") {
        public void apply(Player p) { p.stats.moveSpeed *= 1.10f; }
    },
    MAX_HP("Vida máxima +20", "Aumenta a vida e cura 20.", "icon_max_hp") {
        public void apply(Player p) {
            p.stats.maxHp += 20f;
            p.hp = Math.min(p.stats.maxHp, p.hp + 20f);
        }
    },
    MAGNET("Ímã +30%", "Coleta gemas de mais longe.", "icon_magnet") {
        public void apply(Player p) { p.stats.magnet *= 1.30f; }
    },
    SP_ARROWS(Special.ARROW_NOVA, "Dispara um anel de flechas ao seu redor."),
    SP_ICE(Special.ICE_SHIELD, "Absorve golpes e congela quem encostar."),
    SP_CHAIN(Special.CHAIN_LIGHTNING, "Um raio salta entre os inimigos próximos."),
    SP_METEOR(Special.METEORS, "Meteoros caem em volta do ponto mirado.");

    public final String title;
    public final String description;
    public final String icon;
    public final Special special;

    Upgrade(String title, String description, String icon) {
        this.title = title; this.description = description; this.icon = icon; this.special = null;
    }

    Upgrade(Special special, String description) {
        this.title = special.title; this.description = description; this.icon = special.icon; this.special = special;
    }

    public void apply(Player p) {
        if (p.special == special) p.specialLevel++;
        else { p.special = special; p.specialLevel = 1; p.specialCooldown = 0f; }
    }

    /** Titulo exibido na carta, considerando o nivel atual da especial. */
    public String cardTitle(Player p) {
        if (special == null) return title;
        return p.special == special ? title + " Nv " + (p.specialLevel + 1) : title;
    }
}
