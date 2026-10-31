package pe.edu.sgfa.ficha.service;

public class RacaFilaFichaCiclo {

    private final int cicloNumeroCrr;
    private final String cicloNombreCrr;
    private final String cursoCodigoCrr;
    private final String cursoNombreCrr;
    private final int creditos;
    private final Double notaFinalCrr;
    private final Double ponderadoCrr;
    private final String fechaRegistroCrr;
    private final String observacionCrr;
    private final Double promedioCicloCrr;
    private final int totalCreditosCicloCrr;
    private final double sumaPonderadaCicloCrr;

    public RacaFilaFichaCiclo(int cicloNumeroCrr, String cicloNombreCrr, String cursoCodigoCrr, String cursoNombreCrr, int creditosCrr,
                           Double notaFinalCrr, Double ponderadoCrr, String fechaRegistroCrr, String observacionCrr,
                           Double promedioCicloCrr, int totalCreditosCicloCrr, double sumaPonderadaCicloCrr) {
        this.cicloNumeroCrr = cicloNumeroCrr;
        this.cicloNombreCrr = cicloNombreCrr;
        this.cursoCodigoCrr = cursoCodigoCrr;
        this.cursoNombreCrr = cursoNombreCrr;
        this.creditos = creditosCrr;
        this.notaFinalCrr = notaFinalCrr;
        this.ponderadoCrr = ponderadoCrr;
        this.fechaRegistroCrr = fechaRegistroCrr;
        this.observacionCrr = observacionCrr;
        this.promedioCicloCrr = promedioCicloCrr;
        this.totalCreditosCicloCrr = totalCreditosCicloCrr;
        this.sumaPonderadaCicloCrr = sumaPonderadaCicloCrr;
    }

    public int getCicloNumero() {
        return cicloNumeroCrr;
    }

    public String getCicloNombre() {
        return cicloNombreCrr;
    }

    public String getCicloTitulo() {
        return RacaTituloCiclo.de(cicloNumeroCrr);
    }

    public String getCursoCodigo() {
        return cursoCodigoCrr;
    }

    public String getCursoNombre() {
        return cursoNombreCrr;
    }

    public int getCreditos() {
        return creditos;
    }

    public Double getNotaFinal() {
        return notaFinalCrr;
    }

    public Double getPonderado() {
        return ponderadoCrr;
    }

    public String getFechaRegistro() {
        return fechaRegistroCrr;
    }

    public String getObservacion() {
        return observacionCrr;
    }

    public Double getPromedioCiclo() {
        return promedioCicloCrr;
    }

    public int getTotalCreditosCiclo() {
        return totalCreditosCicloCrr;
    }

    public double getSumaPonderadaCiclo() {
        return sumaPonderadaCicloCrr;
    }
}
