package pe.edu.sgfa.ocr.service;

public record PalabraReconocida(String texto, int x, int y, int ancho, int alto) {

    public int centroX() {
        return x + ancho / 2;
    }

    public int centroY() {
        return y + alto / 2;
    }
}
