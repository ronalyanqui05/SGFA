package pe.edu.sgfa.ocr.service;

public record RahpPalabraReconocida(String texto, int x, int y, int ancho, int alto) {

    public int rahpCentroX() {
        return x + ancho / 2;
    }

    public int rahpCentroY() {
        return y + alto / 2;
    }
}
