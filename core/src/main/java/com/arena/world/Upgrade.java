package com.arena.world;

/** Melhorias passivas oferecidas ao subir de nivel. Todas acumulam. */
public enum Upgrade {
    DAMAGE("Dano +25%", "Projéteis causam mais dano.") {
        public void apply(Player p) { p.stats.damage *= 1.25f; }
    },
    FIRE_RATE("Cadência +20%", "Atira mais rápido.") {
        public void apply(Player p) { p.stats.fireRate *= 1.20f; }
    },
    BULLET_SPEED("Velocidade do tiro +20%", "Projéteis voam mais rápido.") {
        public void apply(Player p) { p.stats.bulletSpeed *= 1.20f; }
    },
    MULTISHOT("Multishot", "+1 projétil em leque.") {
        public void apply(Player p) { p.stats.multishot++; }
    },
    PIERCE("Perfuração", "Atravessa +1 inimigo.") {
        public void apply(Player p) { p.stats.pierce++; }
    },
    RICOCHET("Ricochete", "Quica nas paredes +1 vez.") {
        public void apply(Player p) { p.stats.ricochet++; }
    },
    SINE("Trajetória senoidal", "O tiro ondula e cobre mais área.") {
        public void apply(Player p) { p.stats.sineLevel++; }
    },
    HOMING("Teleguiado", "O tiro curva até o inimigo mais próximo.") {
        public void apply(Player p) { p.stats.homingLevel++; }
    },
    ORBIT("Órbita", "+1 projétil girando ao seu redor.") {
        public void apply(Player p) { p.stats.orbitCount++; }
    },
    MOVE_SPEED("Velocidade +10%", "Você se move mais rápido.") {
        public void apply(Player p) { p.stats.moveSpeed *= 1.10f; }
    },
    MAX_HP("Vida máxima +20", "Aumenta a vida e cura 20.") {
        public void apply(Player p) {
            p.stats.maxHp += 20f;
            p.hp = Math.min(p.stats.maxHp, p.hp + 20f);
        }
    },
    MAGNET("Ímã +30%", "Coleta gemas de mais longe.") {
        public void apply(Player p) { p.stats.magnet *= 1.30f; }
    };

    public final String title;
    public final String description;

    Upgrade(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public abstract void apply(Player p);
}
