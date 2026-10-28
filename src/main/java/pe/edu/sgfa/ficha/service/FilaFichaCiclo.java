package pe.edu.sgfa.ficha.service;

public class FilaFichaCiclo {

    private final int cicloNumero;
    private final String cicloNombre;
    private final String cursoCodigo;
    private final String cursoNombre;
    private final int creditos;
    private final Double notaFinal;
    private final Double ponderado;
    private final String fechaRegistro;
    private final String observacion;
    private final Double promedioCiclo;
    private final int totalCreditosCiclo;
    private final double sumaPonderadaCiclo;

    public FilaFichaCiclo(int cicloNumero, String cicloNombre, String cursoCodigo, String cursoNombre, int creditos,
                           Double notaFinal, Double ponderado, String fechaRegistro, String observacion,
                           Double promedioCiclo, int totalCreditosCiclo, double sumaPonderadaCiclo) {
        this.cicloNumero = cicloNumero;
        this.cicloNombre = cicloNombre;
        this.cursoCodigo = cursoCodigo;
        this.cursoNombre = cursoNombre;
        this.creditos = creditos;
        this.notaFinal = notaFinal;
        this.ponderado = ponderado;
        this.fechaRegistro = fechaRegistro;
        this.observacion = observacion;
        this.promedioCiclo = promedioCiclo;
        this.totalCreditosCiclo = totalCreditosCiclo;
        this.sumaPonderadaCiclo = sumaPonderadaCiclo;
    }

    public int getCicloNumero() {
        return cicloNumero;
    }

    public String getCicloNombre() {
        return cicloNombre;
    }

    public String getCicloTitulo() {
        return TituloCiclo.de(cicloNumero);
    }

    public String getCursoCodigo() {
        return cursoCodigo;
    }

    public String getCursoNombre() {
        return cursoNombre;
    }

    public int getCreditos() {
        return creditos;
    }

    public Double getNotaFinal() {
        return notaFinal;
    }

    public Double getPonderado() {
        return ponderado;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public String getObservacion() {
        return observacion;
    }

    public Double getPromedioCiclo() {
        return promedioCiclo;
    }

    public int getTotalCreditosCiclo() {
        return totalCreditosCiclo;
    }

    public double getSumaPonderadaCiclo() {
        return sumaPonderadaCiclo;
    }
}
