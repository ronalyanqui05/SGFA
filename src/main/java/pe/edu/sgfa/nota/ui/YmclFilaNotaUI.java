package pe.edu.sgfa.nota.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import pe.edu.sgfa.nota.domain.YmclRegistroAcademico;
import pe.edu.sgfa.nota.domain.YmclSituacionAcademica;
import pe.edu.sgfa.nota.service.YmclFilaRegistro;
import pe.edu.sgfa.nota.service.YmclItemNota;
import pe.edu.sgfa.programa.domain.RacaPlanCurso;

public class YmclFilaNotaUI {

    private final RacaPlanCurso planCursoChp;
    private final StringProperty notaChp = new SimpleStringProperty("");
    private final ObjectProperty<YmclSituacionAcademica> situacionChp = new SimpleObjectProperty<>();
    private final StringProperty observacionChp = new SimpleStringProperty("");

    public YmclFilaNotaUI(YmclFilaRegistro filaChp) {
        this.planCursoChp = filaChp.planCurso();
        YmclRegistroAcademico registroChp = filaChp.registro();
        if (registroChp != null) {
            if (registroChp.getNotaFinal() != null) {
                notaChp.set(ymclFormatearNota(registroChp.getNotaFinal()));
            }
            situacionChp.set(registroChp.getSituacion());
            observacionChp.set(registroChp.getObservacion() == null ? "" : registroChp.getObservacion());
        }
    }

    public RacaPlanCurso getPlanCurso() {
        return planCursoChp;
    }

    public StringProperty notaProperty() {
        return notaChp;
    }

    public ObjectProperty<YmclSituacionAcademica> situacionProperty() {
        return situacionChp;
    }

    public StringProperty observacionProperty() {
        return observacionChp;
    }

    public Double getNotaComoNumero() {
        String textoChp = notaChp.get();
        if (textoChp == null || textoChp.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(textoChp.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getPonderadoTexto() {
        Double valorChp = getNotaComoNumero();
        if (valorChp == null) {
            return "";
        }
        return ymclFormatearNota(valorChp * planCursoChp.getCurso().getCreditos());
    }

    public YmclItemNota ymclAItem() {
        YmclSituacionAcademica sitChp = situacionChp.get();
        String obsChp = observacionChp.get();
        return new YmclItemNota(planCursoChp.getId(), getNotaComoNumero(), sitChp == null ? null : sitChp.getId(),
                obsChp == null || obsChp.isBlank() ? null : obsChp.trim());
    }

    private static String ymclFormatearNota(double valorChp) {
        if (valorChp == Math.rint(valorChp)) {
            return String.valueOf((long) valorChp);
        }
        return String.valueOf(valorChp);
    }
}
