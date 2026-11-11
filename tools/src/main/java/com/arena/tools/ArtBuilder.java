package com.arena.tools;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Gera assets/atlas/game.atlas a partir das imagens de referencia em art/:
 *  - hero.png     planilha 8x4 do aventureiro (frente, costas, lado)
 *  - enemies.jpg  planilha 8x4: espectro, cavaleiro, lobisomem, necromante
 *  - icons.jpg    planilha 8x4 de icones de melhoria
 *  - biomes.jpg   3 ilustracoes de bioma (so a paleta e extraida)
 * e pinta proceduralmente os tiles, props e efeitos com essas paletas.
 *
 * Uso: ./gradlew :tools:run
 */
public final class ArtBuilder {
    public static void main(String[] args) throws IOException {
        File art = new File(args.length > 0 ? args[0] : "art");
        File out = new File(args.length > 1 ? args[1] : "assets/atlas");
        Packer packer = new Packer();

        BufferedImage hero = ImageIO.read(new File(art, "hero.png"));
        BufferedImage enemies = ImageIO.read(new File(art, "enemies.jpg"));
        BufferedImage icons = ImageIO.read(new File(art, "icons.jpg"));
        BufferedImage biomes = ImageIO.read(new File(art, "biomes.jpg"));

        // --- Heroi (grade 8x4, celulas ~88x98). Lado = virado para a direita.
        frames(packer, "hero_down", hero, 8, 4, 1.0, 0, new int[][]{{0, 0}, {0, 1}, {2, 0}, {3, 1}});
        frames(packer, "hero_up", hero, 8, 4, 1.0, 0, new int[][]{{0, 3}, {1, 3}, {2, 3}, {3, 3}});
        frames(packer, "hero_side", hero, 8, 4, 1.0, 0, new int[][]{{0, 4}, {1, 4}, {2, 4}, {0, 6}});

        // --- Inimigos (grade 8x4, celulas 250x279)
        double es = 0.42;
        frames(packer, "wraith", enemies, 8, 4, es, 0, new int[][]{{0, 0}, {1, 0}});
        frames(packer, "wraith_attack", enemies, 8, 4, es, 0, new int[][]{{1, 3}});
        frames(packer, "knight", enemies, 8, 4, es, 0, new int[][]{{0, 4}, {1, 4}, {0, 5}, {1, 5}});
        frames(packer, "werewolf", enemies, 8, 4, es, 0, new int[][]{{2, 4}, {2, 5}, {2, 6}, {2, 7}});
        frames(packer, "werewolf_front", enemies, 8, 4, es, 0, new int[][]{{2, 0}});
        frames(packer, "necro", enemies, 8, 4, es, 0, new int[][]{{3, 4}});
        frames(packer, "necro_cast", enemies, 8, 4, es, 0, new int[][]{{3, 6}});

        // --- Icones (o topo de cada celula tem o rotulo em ingles: apagamos)
        icon(packer, "icon_dash", icons, 0, 3);
        icon(packer, "icon_damage", icons, 0, 4);
        icon(packer, "icon_fire_rate", icons, 0, 6);
        icon(packer, "icon_bullet_speed", icons, 1, 0);
        icon(packer, "icon_multishot", icons, 1, 2);
        icon(packer, "icon_pierce", icons, 1, 6);
        icon(packer, "icon_ricochet", icons, 2, 1);
        icon(packer, "icon_sine", icons, 2, 3);
        icon(packer, "icon_homing", icons, 2, 5);
        icon(packer, "icon_orbit", icons, 2, 7);
        icon(packer, "icon_move_speed", icons, 3, 4);
        icon(packer, "icon_max_hp", icons, 3, 5);
        icon(packer, "icon_magnet", icons, 3, 6);

        // --- Tiles, props e efeitos pintados com as paletas dos biomas
        Painter.paintAll(packer, biomes);

        packer.write(out, "game");
    }

    static void frames(Packer p, String name, BufferedImage sheet, int cols, int rows, double scale, int eraseTop, int[][] cells) {
        for (int i = 0; i < cells.length; i++) {
            BufferedImage c = SpriteSheets.cell(sheet, cols, rows, cells[i][0], cells[i][1], 2);
            p.add(name, i, SpriteSheets.character(c, scale, eraseTop));
        }
    }

    static void icon(Packer p, String name, BufferedImage sheet, int r, int c) {
        BufferedImage cell = SpriteSheets.cell(sheet, 8, 4, r, c, 3);
        BufferedImage sprite = SpriteSheets.character(cell, 1.0, 74);
        double s = 64.0 / Math.max(sprite.getWidth(), sprite.getHeight());
        BufferedImage small = Img.downscale(sprite, s, false);
        BufferedImage sq = Img.argb(64, 64);
        int ox = (64 - small.getWidth()) / 2, oy = (64 - small.getHeight()) / 2;
        for (int y = 0; y < small.getHeight(); y++)
            for (int x = 0; x < small.getWidth(); x++) sq.setRGB(ox + x, oy + y, small.getRGB(x, y));
        p.add(name, sq);
    }

    private ArtBuilder() {}
}
