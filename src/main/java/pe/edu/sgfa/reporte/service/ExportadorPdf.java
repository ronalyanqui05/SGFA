package pe.edu.sgfa.reporte.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.common.exception.SgfaException;
import pe.edu.sgfa.reporte.service.TablaReporte.Grupo;

@Component
public class ExportadorPdf {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final float MARGEN = 28;
    private static final float PIE = 26;
    private static final PDFont NORMAL = PDType1Font.HELVETICA;
    private static final PDFont NEGRITA = PDType1Font.HELVETICA_BOLD;
    private static final float[] ROJO = {0.72f, 0.10f, 0.10f};
    private static final float[] NARANJA = {0.80f, 0.42f, 0.00f};
    private static final float[] VERDE = {0.10f, 0.45f, 0.20f};
    private static final Map<String, float[]> SEMAFORO = Map.ofEntries(
            Map.entry("ALTO", ROJO), Map.entry("NO PROMUEVE", ROJO), Map.entry("Sin notas", ROJO),
            Map.entry("MEDIO", NARANJA), Map.entry("INCOMPLETO", NARANJA), Map.entry("POR DEFINIR", NARANJA),
            Map.entry("Parcial", NARANJA), Map.entry("BAJO", VERDE), Map.entry("PROMUEVE", VERDE),
            Map.entry("Completo", VERDE), Map.entry("Quinto superior", VERDE), Map.entry("Tercio superior", VERDE));

    public record Oficial(String correlativo, String codigoVerificacion, Path logo) {
    }

    public void escribir(TablaReporte tbl, String institucion, Path dstn) {
        escribir(tbl, institucion, dstn, null);
    }

    public void escribir(TablaReporte tbl, String institucion, Path dstn, Oficial oficial) {
        try (PDDocument doc = new PDDocument()) {
            int pgns = new Dibujo(tbl, institucion, null, oficial).dibujar();
            new Dibujo(tbl, institucion, doc, oficial).total(pgns).dibujar();
            doc.save(dstn.toFile());
        } catch (IOException e) {
            throw new SgfaException("No se pudo escribir el PDF.", e);
        }
    }

    private static String limpio(String texto) {
        StringBuilder sb = new StringBuilder();
        for (char c : (texto == null ? "" : texto).toCharArray()) {
            sb.append(c >= 32 && c < 256 ? c : (c == '–' || c == '—' ? '-' : (c < 32 ? ' ' : '?')));
        }
        return sb.toString();
    }

    private static float ancho(PDFont fnt, float tam, String texto) {
        try {
            return fnt.getStringWidth(texto) / 1000f * tam;
        } catch (IOException e) {
            throw new SgfaException("No se pudo medir el texto.", e);
        }
    }

    private static List<String> renglones(PDFont fnt, float tam, String texto, float mxm) {
        List<String> sld = new ArrayList<>();
        StringBuilder actl = new StringBuilder();
        for (String plbr : limpio(texto).split(" ")) {
            while (ancho(fnt, tam, plbr) > mxm && plbr.length() > 1) {
                int crt = plbr.length() - 1;
                while (crt > 1 && ancho(fnt, tam, plbr.substring(0, crt)) > mxm) {
                    crt--;
                }
                if (actl.length() > 0) {
                    sld.add(actl.toString());
                    actl.setLength(0);
                }
                sld.add(plbr.substring(0, crt));
                plbr = plbr.substring(crt);
            }
            String prb = actl.length() == 0 ? plbr : actl + " " + plbr;
            if (actl.length() > 0 && ancho(fnt, tam, prb) > mxm) {
                sld.add(actl.toString());
                actl = new StringBuilder(plbr);
            } else {
                actl = new StringBuilder(prb);
            }
        }
        sld.add(actl.toString());
        return sld;
    }

    private static final class Dibujo {
        private final TablaReporte t;
        private final String institucion;
        private final PDDocument doc;
        private final Oficial oficial;
        private PDImageXObject logoImagen;
        private final boolean horizontal;
        private final float anchoPagina;
        private final float altoPagina;
        private final float[] anchos;
        private final float tam;
        private int totalPaginas;
        private int pagina;
        private PDPageContentStream cs;
        private float y;

        Dibujo(TablaReporte t, String institucion, PDDocument doc, Oficial oficial) {
            this.t = t;
            this.institucion = institucion;
            this.doc = doc;
            this.oficial = oficial;
            this.horizontal = t.columnas().size() > 8;
            PDRectangle a4 = PDRectangle.A4;
            this.anchoPagina = horizontal ? a4.getHeight() : a4.getWidth();
            this.altoPagina = horizontal ? a4.getWidth() : a4.getHeight();
            float sm = t.anchos().stream().mapToInt(Integer::intValue).sum();
            this.anchos = new float[t.columnas().size()];
            for (int i = 0; i < anchos.length; i++) {
                anchos[i] = (anchoPagina - 2 * MARGEN) * t.anchos().get(i) / sm;
            }
            this.tam = t.columnas().size() > 20 ? 6.5f : (t.columnas().size() > 12 ? 7f : 8f);
        }

        Dibujo total(int pgns) {
            this.totalPaginas = pgns;
            return this;
        }

        int dibujar() throws IOException {
            abrirPagina(true);
            encabezadoTabla();
            for (int i = 0; i < t.filas().size(); i++) {
                List<String> fl = t.filas().get(i);
                boolean sccn = t.esSeccion(i);
                float alt = sccn ? alturaSeccion() : alturaFila(fl, false);

                float ncsr = sccn && i + 1 < t.filas().size() ? alt + alturaFila(t.filas().get(i + 1), false) : alt;
                if (y - ncsr < MARGEN + PIE) {
                    cerrarPagina();
                    abrirPagina(false);
                    encabezadoTabla();
                }
                if (sccn) {
                    dibujarSeccion(TablaReporte.textoDeSeccion(fl), alt);
                } else {
                    dibujarFila(fl, alt);
                }
            }
            pie();
            firmas();
            cerrarPagina();
            return pagina;
        }

        private void abrirPagina(boolean prmr) throws IOException {
            pagina++;
            if (doc != null) {
                PDPage p = new PDPage(new PDRectangle(anchoPagina, altoPagina));
                doc.addPage(p);
                cs = new PDPageContentStream(doc, p);
            }
            y = altoPagina - MARGEN;
            if (prmr) {
                float arrb = y;
                boolean conLg = tieneLogo();
                if (conLg) {
                    dibujarLogo(arrb);
                }
                if (institucion != null && !institucion.isBlank()) {
                    centrado(institucion, NEGRITA, 11);
                }
                if (oficial != null) {
                    String nmr = "N\u00B0 " + oficial.correlativo();
                    texto(nmr, NEGRITA, 9, anchoPagina - MARGEN - ancho(NEGRITA, 9, limpio(nmr)), arrb - 9, null);
                }
                centrado(t.titulo(), NEGRITA, 13);
                y -= 2;
                if (conLg) {
                    y = Math.min(y, arrb - LOGO - 6);
                }
                for (String linea : t.subtitulo()) {
                    texto(linea, NORMAL, 8, MARGEN, y - 8, null);
                    y -= 11;
                }
                y -= 6;
            } else {
                texto(t.titulo() + " (continuacion)", NEGRITA, 8, MARGEN, y - 8, null);
                if (oficial != null) {
                    String nmr = "N\u00B0 " + oficial.correlativo();
                    texto(nmr, NEGRITA, 8, anchoPagina - MARGEN - ancho(NEGRITA, 8, limpio(nmr)), y - 8, null);
                }
                y -= 12;
                linea(MARGEN, y, anchoPagina - MARGEN, 0.4f, 0.7f);
                y -= 6;
            }
        }

        private void cerrarPagina() throws IOException {
            if (cs == null) {
                return;
            }
            linea(MARGEN, MARGEN + 14, anchoPagina - MARGEN, 0.5f, 0f);
            texto("Fecha de emision: " + LocalDate.now().format(FECHA), NORMAL, 8, MARGEN, MARGEN + 3, null);
            String cntr = oficial != null ? "Codigo de verificacion: " + oficial.codigoVerificacion() : "Reporte interno";
            texto(cntr, oficial != null ? NORMAL : NORMAL, 8, (anchoPagina - ancho(NORMAL, 8, cntr)) / 2, MARGEN + 3, null);
            String num = "Pagina " + pagina + " de " + totalPaginas;
            texto(num, NORMAL, 8, anchoPagina - MARGEN - ancho(NORMAL, 8, num), MARGEN + 3, null);
            cs.close();
            cs = null;
        }

        private float alturaTexto(PDFont f, float tmn, String texto, float anchCld) {
            return renglones(f, tmn, texto, anchCld - 4).size() * tmn * 1.25f + 3;
        }

        private float alturaGrupos() {
            if (!t.tieneGrupos()) {
                return 0;
            }
            float max = 0;
            int col = 0;
            for (Grupo g : t.grupos()) {
                float ancho = 0;
                for (int k = 0; k < g.columnas(); k++) {
                    ancho += anchos[col + k];
                }
                if (!g.texto().isEmpty()) {
                    max = Math.max(max, alturaTexto(NEGRITA, tam, g.texto(), ancho));
                }
                col += g.columnas();
            }
            return max;
        }

        private float alturaColumnas() {
            float max = 0;
            for (int i = 0; i < anchos.length; i++) {
                max = Math.max(max, alturaTexto(NEGRITA, tam, t.columnas().get(i), anchos[i]));
            }
            return max;
        }

        private void encabezadoTabla() throws IOException {
            float hGrps = alturaGrupos();
            float hCls = alturaColumnas();
            if (y - hGrps - hCls < MARGEN + PIE) {
                return;
            }
            float x = MARGEN;
            if (t.tieneGrupos()) {
                int col = 0;
                for (Grupo g : t.grupos()) {
                    float ancho = 0;
                    for (int k = 0; k < g.columnas(); k++) {
                        ancho += anchos[col + k];
                    }
                    if (g.texto().isEmpty()) {

                        celda(x, y, ancho, hGrps + hCls, t.columnas().get(col), NEGRITA, true, false, 0.88f, null);
                    } else {
                        celda(x, y, ancho, hGrps, g.texto(), NEGRITA, true, false, 0.88f, null);
                        float xc = x;
                        for (int k = 0; k < g.columnas(); k++) {
                            celda(xc, y - hGrps, anchos[col + k], hCls, t.columnas().get(col + k), NEGRITA, true, false, 0.94f, null);
                            xc += anchos[col + k];
                        }
                    }
                    x += ancho;
                    col += g.columnas();
                }
                y -= hGrps + hCls;
            } else {
                for (int i = 0; i < anchos.length; i++) {
                    celda(x, y, anchos[i], hCls, t.columnas().get(i), NEGRITA, true, false, 0.88f, null);
                    x += anchos[i];
                }
                y -= hCls;
            }
        }

        private float alturaFila(List<String> fl, boolean enftzd) {
            float max = tam * 1.25f + 3;
            for (int i = 0; i < anchos.length && i < fl.size(); i++) {
                if (!t.numericas().get(i)) {
                    max = Math.max(max, alturaTexto(esTotal(fl) ? NEGRITA : NORMAL, tam, fl.get(i), anchos[i]));
                }
            }
            return max;
        }

        private float alturaSeccion() {
            return tam * 1.25f + 7;
        }

        private void dibujarSeccion(String texto, float alt) throws IOException {
            celda(MARGEN, y, anchoPagina - 2 * MARGEN, alt, texto, NEGRITA, false, false, 0.80f, null);
            y -= alt;
        }

        private static boolean esTotal(List<String> fl) {
            return fl.contains("TOTAL") || fl.contains("PROMEDIO DEL CURSO");
        }

        private void dibujarFila(List<String> fl, float alt) throws IOException {
            boolean total = esTotal(fl);
            float x = MARGEN;
            for (int i = 0; i < anchos.length; i++) {
                String vlr = i < fl.size() ? fl.get(i) : "";
                celda(x, y, anchos[i], alt, vlr, total ? NEGRITA : NORMAL, false, t.numericas().get(i),
                        total ? 0.92f : 1f, SEMAFORO.get(vlr));
                x += anchos[i];
            }
            y -= alt;
        }

        private void celda(float x, float arrb, float ancho, float alt, String vlr, PDFont fnt, boolean encbzd,
                           boolean nmrc, float grs, float[] clr) throws IOException {
            if (cs == null) {
                return;
            }
            if (grs < 1f) {
                cs.setNonStrokingColor(grs, grs, grs);
                cs.addRect(x, arrb - alt, ancho, alt);
                cs.fill();
            }
            cs.setStrokingColor(0.45f, 0.45f, 0.45f);
            cs.setLineWidth(0.4f);
            cs.addRect(x, arrb - alt, ancho, alt);
            cs.stroke();
            float yTxt = arrb - tam - 1.5f;
            for (String r : renglones(fnt, tam, vlr, ancho - 4)) {
                float xt = x + 2;
                float w = ancho(fnt, tam, r);
                if (encbzd) {
                    xt = x + (ancho - w) / 2;
                } else if (nmrc) {
                    xt = x + ancho - 2 - w;
                }
                texto(r, fnt, tam, xt, yTxt, clr);
                yTxt -= tam * 1.25f;
            }
        }

        private void pie() throws IOException {
            if (t.pie().isEmpty()) {
                return;
            }
            y -= 8;
            for (String nt : t.pie()) {
                for (String r : renglones(NORMAL, 7, nt, anchoPagina - 2 * MARGEN)) {
                    if (y - 9 < MARGEN + PIE) {
                        cerrarPagina();
                        abrirPagina(false);
                    }
                    texto(r, NORMAL, 7, MARGEN, y - 7, null);
                    y -= 9;
                }
            }
        }

        private void firmas() throws IOException {
            if (!t.firmas() && oficial == null) {
                return;
            }
            float ncsr = oficial != null ? 100 : 58;
            if (y - ncsr < MARGEN + PIE) {
                cerrarPagina();
                abrirPagina(false);
            }
            float yLn = y - (oficial != null ? 62 : 40);
            float ancho = Math.min(180, (anchoPagina - 2 * MARGEN) / 3);
            float x1 = MARGEN + (anchoPagina - 2 * MARGEN) / 4 - ancho / 2;
            float x2 = MARGEN + 3 * (anchoPagina - 2 * MARGEN) / 4 - ancho / 2;
            linea(x1, yLn, x1 + ancho, 0.8f, 0f);
            linea(x2, yLn, x2 + ancho, 0.8f, 0f);
            String a = "Secretario Académico";
            String b = "Director General";
            texto(a, NORMAL, 8, x1 + (ancho - ancho(NORMAL, 8, a)) / 2, yLn - 10, null);
            texto(b, NORMAL, 8, x2 + (ancho - ancho(NORMAL, 8, b)) / 2, yLn - 10, null);
            y = yLn - 14;
            if (oficial != null) {
                String sll = "(Firma y sello)";
                texto(sll, NORMAL, 7, x1 + (ancho - ancho(NORMAL, 7, sll)) / 2, yLn - 19, null);
                texto(sll, NORMAL, 7, x2 + (ancho - ancho(NORMAL, 7, sll)) / 2, yLn - 19, null);
                String vldz = "Documento oficial: es valido con la firma y el sello de la institucion. Compruebe su autenticidad "
                        + "con el numero y el codigo de verificacion.";
                y = yLn - 30;
                for (String r : renglones(NORMAL, 7, vldz, anchoPagina - 2 * MARGEN)) {
                    texto(r, NORMAL, 7, (anchoPagina - ancho(NORMAL, 7, r)) / 2, y - 7, null);
                    y -= 9;
                }
            }
        }

        private static final float LOGO = 46;

        private boolean tieneLogo() {
            return oficial != null && oficial.logo() != null && Files.isRegularFile(oficial.logo());
        }

        private void dibujarLogo(float arrb) throws IOException {
            if (cs == null) {
                return;
            }
            if (logoImagen == null) {
                logoImagen = PDImageXObject.createFromFile(oficial.logo().toString(), doc);
            }
            float escl = Math.min(LOGO / logoImagen.getWidth(), LOGO / logoImagen.getHeight());
            cs.drawImage(logoImagen, MARGEN, arrb - logoImagen.getHeight() * escl, logoImagen.getWidth() * escl,
                    logoImagen.getHeight() * escl);
        }

        private void centrado(String texto, PDFont fnt, float tmn) throws IOException {
            texto(texto, fnt, tmn, (anchoPagina - ancho(fnt, tmn, limpio(texto))) / 2, y - tmn, null);
            y -= tmn + 5;
        }

        private void texto(String texto, PDFont fnt, float tmn, float x, float yTxt, float[] clr) throws IOException {
            if (cs == null) {
                return;
            }
            cs.beginText();
            cs.setFont(fnt, tmn);
            float[] c = clr == null ? new float[] {0f, 0f, 0f} : clr;
            cs.setNonStrokingColor(c[0], c[1], c[2]);
            cs.newLineAtOffset(x, yTxt);
            cs.showText(limpio(texto));
            cs.endText();
        }

        private void linea(float x1, float yLn, float x2, float grsr, float grs) throws IOException {
            if (cs == null) {
                return;
            }
            cs.setStrokingColor(grs, grs, grs);
            cs.setLineWidth(grsr);
            cs.moveTo(x1, yLn);
            cs.lineTo(x2, yLn);
            cs.stroke();
        }
    }
}
