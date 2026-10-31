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

import pe.edu.sgfa.common.exception.RyeSgfaException;
import pe.edu.sgfa.reporte.service.CphTablaReporte.CphGrupo;

@Component
public class CphExportadorPdf {

    private static final DateTimeFormatter FECHA_PCH = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final float MARGEN_PCH = 28;
    private static final float PIE_PCH = 26;
    private static final PDFont NORMAL_PCH = PDType1Font.HELVETICA;
    private static final PDFont NEGRITA_PCH = PDType1Font.HELVETICA_BOLD;
    private static final float[] ROJO_PCH = {0.72f, 0.10f, 0.10f};
    private static final float[] NARANJA_PCH = {0.80f, 0.42f, 0.00f};
    private static final float[] VERDE_PCH = {0.10f, 0.45f, 0.20f};
    private static final Map<String, float[]> SEMAFORO_PCH = Map.ofEntries(
            Map.entry("ALTO", ROJO_PCH), Map.entry("NO PROMUEVE", ROJO_PCH), Map.entry("Sin notas", ROJO_PCH),
            Map.entry("MEDIO", NARANJA_PCH), Map.entry("INCOMPLETO", NARANJA_PCH), Map.entry("POR DEFINIR", NARANJA_PCH),
            Map.entry("Parcial", NARANJA_PCH), Map.entry("BAJO", VERDE_PCH), Map.entry("PROMUEVE", VERDE_PCH),
            Map.entry("Completo", VERDE_PCH), Map.entry("Quinto superior", VERDE_PCH), Map.entry("Tercio superior", VERDE_PCH));

    public record CphOficial(String correlativo, String codigoVerificacion, Path logo) {
    }

    public void cphEscribir(CphTablaReporte tablaPch, String institucionPch, Path destinoPch) {
        cphEscribir(tablaPch, institucionPch, destinoPch, null);
    }

    public void cphEscribir(CphTablaReporte tablaPch, String institucionPch, Path destinoPch, CphOficial oficialPch) {
        try (PDDocument docPch = new PDDocument()) {
            int paginasPch = new CphDibujo(tablaPch, institucionPch, null, oficialPch).cphDibujar();
            new CphDibujo(tablaPch, institucionPch, docPch, oficialPch).cphTotal(paginasPch).cphDibujar();
            docPch.save(destinoPch.toFile());
        } catch (IOException e) {
            throw new RyeSgfaException("No se pudo escribir el PDF.", e);
        }
    }

    private static String cphLimpio(String textoPch) {
        StringBuilder sb = new StringBuilder();
        for (char c : (textoPch == null ? "" : textoPch).toCharArray()) {
            sb.append(c >= 32 && c < 256 ? c : (c == '–' || c == '—' ? '-' : (c < 32 ? ' ' : '?')));
        }
        return sb.toString();
    }

    private static float cphAncho(PDFont fuentePch, float tamPch, String textoPch) {
        try {
            return fuentePch.getStringWidth(textoPch) / 1000f * tamPch;
        } catch (IOException e) {
            throw new RyeSgfaException("No se pudo medir el texto.", e);
        }
    }

    private static List<String> cphRenglones(PDFont fuentePch, float tamPch, String textoPch, float maximoPch) {
        List<String> salidaPch = new ArrayList<>();
        StringBuilder actualPch = new StringBuilder();
        for (String palabraPch : cphLimpio(textoPch).split(" ")) {
            while (cphAncho(fuentePch, tamPch, palabraPch) > maximoPch && palabraPch.length() > 1) {
                int cortePch = palabraPch.length() - 1;
                while (cortePch > 1 && cphAncho(fuentePch, tamPch, palabraPch.substring(0, cortePch)) > maximoPch) {
                    cortePch--;
                }
                if (actualPch.length() > 0) {
                    salidaPch.add(actualPch.toString());
                    actualPch.setLength(0);
                }
                salidaPch.add(palabraPch.substring(0, cortePch));
                palabraPch = palabraPch.substring(cortePch);
            }
            String pruebaPch = actualPch.length() == 0 ? palabraPch : actualPch + " " + palabraPch;
            if (actualPch.length() > 0 && cphAncho(fuentePch, tamPch, pruebaPch) > maximoPch) {
                salidaPch.add(actualPch.toString());
                actualPch = new StringBuilder(palabraPch);
            } else {
                actualPch = new StringBuilder(pruebaPch);
            }
        }
        salidaPch.add(actualPch.toString());
        return salidaPch;
    }

    private static final class CphDibujo {
        private final CphTablaReporte t;
        private final String institucionPch;
        private final PDDocument docPch;
        private final CphOficial oficialPch;
        private PDImageXObject logoImagenPch;
        private final boolean horizontalPch;
        private final float anchoPaginaPch;
        private final float altoPaginaPch;
        private final float[] anchosPch;
        private final float tamPch;
        private int totalPaginasPch;
        private int paginaPch;
        private PDPageContentStream cs;
        private float y;

        CphDibujo(CphTablaReporte t, String institucionPch, PDDocument docPch, CphOficial oficialPch) {
            this.t = t;
            this.institucionPch = institucionPch;
            this.docPch = docPch;
            this.oficialPch = oficialPch;
            this.horizontalPch = t.columnas().size() > 8;
            PDRectangle a4 = PDRectangle.A4;
            this.anchoPaginaPch = horizontalPch ? a4.getHeight() : a4.getWidth();
            this.altoPaginaPch = horizontalPch ? a4.getWidth() : a4.getHeight();
            float sumaPch = t.anchos().stream().mapToInt(Integer::intValue).sum();
            this.anchosPch = new float[t.columnas().size()];
            for (int i = 0; i < anchosPch.length; i++) {
                anchosPch[i] = (anchoPaginaPch - 2 * MARGEN_PCH) * t.anchos().get(i) / sumaPch;
            }
            this.tamPch = t.columnas().size() > 20 ? 6.5f : (t.columnas().size() > 12 ? 7f : 8f);
        }

        CphDibujo cphTotal(int paginasPch) {
            this.totalPaginasPch = paginasPch;
            return this;
        }

        int cphDibujar() throws IOException {
            cphAbrirPagina(true);
            cphEncabezadoTabla();
            for (int i = 0; i < t.filas().size(); i++) {
                List<String> filaPch = t.filas().get(i);
                boolean seccionPch = t.cphEsSeccion(i);
                float altoPch = seccionPch ? cphAlturaSeccion() : cphAlturaFila(filaPch, false);

                float necesarioPch = seccionPch && i + 1 < t.filas().size() ? altoPch + cphAlturaFila(t.filas().get(i + 1), false) : altoPch;
                if (y - necesarioPch < MARGEN_PCH + PIE_PCH) {
                    cphCerrarPagina();
                    cphAbrirPagina(false);
                    cphEncabezadoTabla();
                }
                if (seccionPch) {
                    cphDibujarSeccion(CphTablaReporte.cphTextoDeSeccion(filaPch), altoPch);
                } else {
                    cphDibujarFila(filaPch, altoPch);
                }
            }
            pie();
            cphFirmas();
            cphCerrarPagina();
            return paginaPch;
        }

        private void cphAbrirPagina(boolean primeraPch) throws IOException {
            paginaPch++;
            if (docPch != null) {
                PDPage p = new PDPage(new PDRectangle(anchoPaginaPch, altoPaginaPch));
                docPch.addPage(p);
                cs = new PDPageContentStream(docPch, p);
            }
            y = altoPaginaPch - MARGEN_PCH;
            if (primeraPch) {
                float arribaPch = y;
                boolean conLogoPch = cphTieneLogo();
                if (conLogoPch) {
                    cphDibujarLogo(arribaPch);
                }
                if (institucionPch != null && !institucionPch.isBlank()) {
                    cphCentrado(institucionPch, NEGRITA_PCH, 11);
                }
                if (oficialPch != null) {
                    String numeroPch = "N\u00B0 " + oficialPch.correlativo();
                    cphTexto(numeroPch, NEGRITA_PCH, 9, anchoPaginaPch - MARGEN_PCH - cphAncho(NEGRITA_PCH, 9, cphLimpio(numeroPch)), arribaPch - 9, null);
                }
                cphCentrado(t.titulo(), NEGRITA_PCH, 13);
                y -= 2;
                if (conLogoPch) {
                    y = Math.min(y, arribaPch - LOGO_PCH - 6);
                }
                for (String lineaPch : t.subtitulo()) {
                    cphTexto(lineaPch, NORMAL_PCH, 8, MARGEN_PCH, y - 8, null);
                    y -= 11;
                }
                y -= 6;
            } else {
                cphTexto(t.titulo() + " (continuacion)", NEGRITA_PCH, 8, MARGEN_PCH, y - 8, null);
                if (oficialPch != null) {
                    String numeroPch = "N\u00B0 " + oficialPch.correlativo();
                    cphTexto(numeroPch, NEGRITA_PCH, 8, anchoPaginaPch - MARGEN_PCH - cphAncho(NEGRITA_PCH, 8, cphLimpio(numeroPch)), y - 8, null);
                }
                y -= 12;
                cphLinea(MARGEN_PCH, y, anchoPaginaPch - MARGEN_PCH, 0.4f, 0.7f);
                y -= 6;
            }
        }

        private void cphCerrarPagina() throws IOException {
            if (cs == null) {
                return;
            }
            cphLinea(MARGEN_PCH, MARGEN_PCH + 14, anchoPaginaPch - MARGEN_PCH, 0.5f, 0f);
            cphTexto("Fecha de emision: " + LocalDate.now().format(FECHA_PCH), NORMAL_PCH, 8, MARGEN_PCH, MARGEN_PCH + 3, null);
            String centroPch = oficialPch != null ? "Codigo de verificacion: " + oficialPch.codigoVerificacion() : "Reporte interno";
            cphTexto(centroPch, oficialPch != null ? NORMAL_PCH : NORMAL_PCH, 8, (anchoPaginaPch - cphAncho(NORMAL_PCH, 8, centroPch)) / 2, MARGEN_PCH + 3, null);
            String numPch = "Pagina " + paginaPch + " de " + totalPaginasPch;
            cphTexto(numPch, NORMAL_PCH, 8, anchoPaginaPch - MARGEN_PCH - cphAncho(NORMAL_PCH, 8, numPch), MARGEN_PCH + 3, null);
            cs.close();
            cs = null;
        }

        private float cphAlturaTexto(PDFont f, float tamanoPch, String textoPch, float anchoCeldaPch) {
            return cphRenglones(f, tamanoPch, textoPch, anchoCeldaPch - 4).size() * tamanoPch * 1.25f + 3;
        }

        private float cphAlturaGrupos() {
            if (!t.cphTieneGrupos()) {
                return 0;
            }
            float maxPch = 0;
            int colPch = 0;
            for (CphGrupo g : t.grupos()) {
                float anchoPch = 0;
                for (int k = 0; k < g.columnas(); k++) {
                    anchoPch += anchosPch[colPch + k];
                }
                if (!g.texto().isEmpty()) {
                    maxPch = Math.max(maxPch, cphAlturaTexto(NEGRITA_PCH, tamPch, g.texto(), anchoPch));
                }
                colPch += g.columnas();
            }
            return maxPch;
        }

        private float cphAlturaColumnas() {
            float maxPch = 0;
            for (int i = 0; i < anchosPch.length; i++) {
                maxPch = Math.max(maxPch, cphAlturaTexto(NEGRITA_PCH, tamPch, t.columnas().get(i), anchosPch[i]));
            }
            return maxPch;
        }

        private void cphEncabezadoTabla() throws IOException {
            float hGruposPch = cphAlturaGrupos();
            float hColsPch = cphAlturaColumnas();
            if (y - hGruposPch - hColsPch < MARGEN_PCH + PIE_PCH) {
                return;
            }
            float x = MARGEN_PCH;
            if (t.cphTieneGrupos()) {
                int colPch = 0;
                for (CphGrupo g : t.grupos()) {
                    float anchoPch = 0;
                    for (int k = 0; k < g.columnas(); k++) {
                        anchoPch += anchosPch[colPch + k];
                    }
                    if (g.texto().isEmpty()) {

                        cphCelda(x, y, anchoPch, hGruposPch + hColsPch, t.columnas().get(colPch), NEGRITA_PCH, true, false, 0.88f, null);
                    } else {
                        cphCelda(x, y, anchoPch, hGruposPch, g.texto(), NEGRITA_PCH, true, false, 0.88f, null);
                        float xc = x;
                        for (int k = 0; k < g.columnas(); k++) {
                            cphCelda(xc, y - hGruposPch, anchosPch[colPch + k], hColsPch, t.columnas().get(colPch + k), NEGRITA_PCH, true, false, 0.94f, null);
                            xc += anchosPch[colPch + k];
                        }
                    }
                    x += anchoPch;
                    colPch += g.columnas();
                }
                y -= hGruposPch + hColsPch;
            } else {
                for (int i = 0; i < anchosPch.length; i++) {
                    cphCelda(x, y, anchosPch[i], hColsPch, t.columnas().get(i), NEGRITA_PCH, true, false, 0.88f, null);
                    x += anchosPch[i];
                }
                y -= hColsPch;
            }
        }

        private float cphAlturaFila(List<String> filaPch, boolean enfatizadaPch) {
            float maxPch = tamPch * 1.25f + 3;
            for (int i = 0; i < anchosPch.length && i < filaPch.size(); i++) {
                if (!t.numericas().get(i)) {
                    maxPch = Math.max(maxPch, cphAlturaTexto(cphEsTotal(filaPch) ? NEGRITA_PCH : NORMAL_PCH, tamPch, filaPch.get(i), anchosPch[i]));
                }
            }
            return maxPch;
        }

        private float cphAlturaSeccion() {
            return tamPch * 1.25f + 7;
        }

        private void cphDibujarSeccion(String textoPch, float altoPch) throws IOException {
            cphCelda(MARGEN_PCH, y, anchoPaginaPch - 2 * MARGEN_PCH, altoPch, textoPch, NEGRITA_PCH, false, false, 0.80f, null);
            y -= altoPch;
        }

        private static boolean cphEsTotal(List<String> filaPch) {
            return filaPch.contains("TOTAL") || filaPch.contains("PROMEDIO DEL CURSO");
        }

        private void cphDibujarFila(List<String> filaPch, float altoPch) throws IOException {
            boolean totalPch = cphEsTotal(filaPch);
            float x = MARGEN_PCH;
            for (int i = 0; i < anchosPch.length; i++) {
                String valorPch = i < filaPch.size() ? filaPch.get(i) : "";
                cphCelda(x, y, anchosPch[i], altoPch, valorPch, totalPch ? NEGRITA_PCH : NORMAL_PCH, false, t.numericas().get(i),
                        totalPch ? 0.92f : 1f, SEMAFORO_PCH.get(valorPch));
                x += anchosPch[i];
            }
            y -= altoPch;
        }

        private void cphCelda(float x, float arribaPch, float anchoPch, float altoPch, String valorPch, PDFont fuentePch, boolean encabezadoPch,
                           boolean numericaPch, float grisPch, float[] colorPch) throws IOException {
            if (cs == null) {
                return;
            }
            if (grisPch < 1f) {
                cs.setNonStrokingColor(grisPch, grisPch, grisPch);
                cs.addRect(x, arribaPch - altoPch, anchoPch, altoPch);
                cs.fill();
            }
            cs.setStrokingColor(0.45f, 0.45f, 0.45f);
            cs.setLineWidth(0.4f);
            cs.addRect(x, arribaPch - altoPch, anchoPch, altoPch);
            cs.stroke();
            float yTextoPch = arribaPch - tamPch - 1.5f;
            for (String r : cphRenglones(fuentePch, tamPch, valorPch, anchoPch - 4)) {
                float xt = x + 2;
                float w = cphAncho(fuentePch, tamPch, r);
                if (encabezadoPch) {
                    xt = x + (anchoPch - w) / 2;
                } else if (numericaPch) {
                    xt = x + anchoPch - 2 - w;
                }
                cphTexto(r, fuentePch, tamPch, xt, yTextoPch, colorPch);
                yTextoPch -= tamPch * 1.25f;
            }
        }

        private void pie() throws IOException {
            if (t.pie().isEmpty()) {
                return;
            }
            y -= 8;
            for (String notaPch : t.pie()) {
                for (String r : cphRenglones(NORMAL_PCH, 7, notaPch, anchoPaginaPch - 2 * MARGEN_PCH)) {
                    if (y - 9 < MARGEN_PCH + PIE_PCH) {
                        cphCerrarPagina();
                        cphAbrirPagina(false);
                    }
                    cphTexto(r, NORMAL_PCH, 7, MARGEN_PCH, y - 7, null);
                    y -= 9;
                }
            }
        }

        private void cphFirmas() throws IOException {
            if (!t.firmas() && oficialPch == null) {
                return;
            }
            float necesarioPch = oficialPch != null ? 100 : 58;
            if (y - necesarioPch < MARGEN_PCH + PIE_PCH) {
                cphCerrarPagina();
                cphAbrirPagina(false);
            }
            float yLineaPch = y - (oficialPch != null ? 62 : 40);
            float anchoPch = Math.min(180, (anchoPaginaPch - 2 * MARGEN_PCH) / 3);
            float x1 = MARGEN_PCH + (anchoPaginaPch - 2 * MARGEN_PCH) / 4 - anchoPch / 2;
            float x2 = MARGEN_PCH + 3 * (anchoPaginaPch - 2 * MARGEN_PCH) / 4 - anchoPch / 2;
            cphLinea(x1, yLineaPch, x1 + anchoPch, 0.8f, 0f);
            cphLinea(x2, yLineaPch, x2 + anchoPch, 0.8f, 0f);
            String a = "Secretario Académico";
            String b = "Director General";
            cphTexto(a, NORMAL_PCH, 8, x1 + (anchoPch - cphAncho(NORMAL_PCH, 8, a)) / 2, yLineaPch - 10, null);
            cphTexto(b, NORMAL_PCH, 8, x2 + (anchoPch - cphAncho(NORMAL_PCH, 8, b)) / 2, yLineaPch - 10, null);
            y = yLineaPch - 14;
            if (oficialPch != null) {
                String selloPch = "(Firma y sello)";
                cphTexto(selloPch, NORMAL_PCH, 7, x1 + (anchoPch - cphAncho(NORMAL_PCH, 7, selloPch)) / 2, yLineaPch - 19, null);
                cphTexto(selloPch, NORMAL_PCH, 7, x2 + (anchoPch - cphAncho(NORMAL_PCH, 7, selloPch)) / 2, yLineaPch - 19, null);
                String validezPch = "Documento oficial: es valido con la firma y el sello de la institucion. Compruebe su autenticidad "
                        + "con el numero y el codigo de verificacion.";
                y = yLineaPch - 30;
                for (String r : cphRenglones(NORMAL_PCH, 7, validezPch, anchoPaginaPch - 2 * MARGEN_PCH)) {
                    cphTexto(r, NORMAL_PCH, 7, (anchoPaginaPch - cphAncho(NORMAL_PCH, 7, r)) / 2, y - 7, null);
                    y -= 9;
                }
            }
        }

        private static final float LOGO_PCH = 46;

        private boolean cphTieneLogo() {
            return oficialPch != null && oficialPch.logo() != null && Files.isRegularFile(oficialPch.logo());
        }

        private void cphDibujarLogo(float arribaPch) throws IOException {
            if (cs == null) {
                return;
            }
            if (logoImagenPch == null) {
                logoImagenPch = PDImageXObject.createFromFile(oficialPch.logo().toString(), docPch);
            }
            float escalaPch = Math.min(LOGO_PCH / logoImagenPch.getWidth(), LOGO_PCH / logoImagenPch.getHeight());
            cs.drawImage(logoImagenPch, MARGEN_PCH, arribaPch - logoImagenPch.getHeight() * escalaPch, logoImagenPch.getWidth() * escalaPch,
                    logoImagenPch.getHeight() * escalaPch);
        }

        private void cphCentrado(String textoPch, PDFont fuentePch, float tamanoPch) throws IOException {
            cphTexto(textoPch, fuentePch, tamanoPch, (anchoPaginaPch - cphAncho(fuentePch, tamanoPch, cphLimpio(textoPch))) / 2, y - tamanoPch, null);
            y -= tamanoPch + 5;
        }

        private void cphTexto(String textoPch, PDFont fuentePch, float tamanoPch, float x, float yTextoPch, float[] colorPch) throws IOException {
            if (cs == null) {
                return;
            }
            cs.beginText();
            cs.setFont(fuentePch, tamanoPch);
            float[] c = colorPch == null ? new float[] {0f, 0f, 0f} : colorPch;
            cs.setNonStrokingColor(c[0], c[1], c[2]);
            cs.newLineAtOffset(x, yTextoPch);
            cs.showText(cphLimpio(textoPch));
            cs.endText();
        }

        private void cphLinea(float x1, float yLineaPch, float x2, float grosorPch, float grisPch) throws IOException {
            if (cs == null) {
                return;
            }
            cs.setStrokingColor(grisPch, grisPch, grisPch);
            cs.setLineWidth(grosorPch);
            cs.moveTo(x1, yLineaPch);
            cs.lineTo(x2, yLineaPch);
            cs.stroke();
        }
    }
}
