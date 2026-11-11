package com.arena.tools;

import java.awt.image.BufferedImage;

/** Mapeamento das celulas das imagens de referencia em art/ para os sprites do jogo. */
final class SpriteSheets {
    /** Celula de uma grade regular. */
    static BufferedImage cell(BufferedImage sheet, int cols, int rows, int r, int c, int inset) {
        double cw = sheet.getWidth() / (double) cols, ch = sheet.getHeight() / (double) rows;
        int x = (int) Math.round(c * cw) + inset, y = (int) Math.round(r * ch) + inset;
        return Img.copy(sheet, x, y, (int) cw - inset * 2, (int) ch - inset * 2);
    }

    /** Recorta um personagem: remove fundo, ruido, numeros e sombra; reduz e corta as bordas vazias. */
    static BufferedImage character(BufferedImage cell, double scale, int eraseTop) {
        if (eraseTop > 0) {
            int bg = Img.borderColor(cell, 0);
            for (int y = 0; y < eraseTop; y++) for (int x = 0; x < cell.getWidth(); x++) cell.setRGB(x, y, bg);
        }
        // Cantos arredondados brancos da imagem original
        Img.removeBackground(cell, Img.rgb(250, 249, 249), 40, 0);
        int bg = Img.borderColor(cell, 0);
        // Tolerancia proporcional ao contraste: fundos escuros pedem tolerancia baixa
        int lum = (Img.r(bg) * 3 + Img.g(bg) * 6 + Img.b(bg)) / 10;
        double flood = lum < 45 ? 14 : lum < 80 ? 22 : 42;
        Img.removeBackground(cell, bg, flood, flood * 0.45);
        Img.keepMainComponents(cell, 0.04, true);
        BufferedImage t = Img.trim(cell);
        return Img.trim(Img.downscale(t, scale, true));
    }

    private SpriteSheets() {}
}
