package pe.edu.sgfa.nota.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import pe.edu.sgfa.nota.domain.RegistroAcademico;
import pe.edu.sgfa.nota.domain.SituacionAcademica;
import pe.edu.sgfa.nota.service.FilaRegistro;
import pe.edu.sgfa.nota.service.ItemNota;
import pe.edu.sgfa.programa.domain.PlanCurso;

public class FilaNotaUI {

    private final PlanCurso planCurso;
    private final StringProperty nota = new SimpleStringProperty("");
    private final ObjectProperty<SituacionAcademica> situacion = new SimpleObjectProperty<>();
    private final StringProperty observacion = new SimpleStringProperty("");

    public FilaNotaUI(FilaRegistro fl) {
        this.planCurso = fl.planCurso();
        RegistroAcademico registro = fl.registro();
        if (registro != null) {
            if (registro.getNotaFinal() != null) {
                nota.set(formatearNota(registro.getNotaFinal()));
            }
            situacion.set(registro.getSituacion());
            observacion.set(registro.getObservacion() == null ? "" : registro.getObservacion());
        }
    }

    public PlanCurso getPlanCurso() {
        return planCurso;
    }

    public StringProperty notaProperty() {
        return nota;
    }

    public ObjectProperty<SituacionAcademica> situacionProperty() {
        return situacion;
    }

    public StringProperty observacionProperty() {
        return observacion;
    }

    public Double getNotaComoNumero() {
        String txt = nota.get();
        if (txt == null || txt.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(txt.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getPonderadoTexto() {
        Double vlr = getNotaComoNumero();
        if (vlr == null) {
            return "";
        }
        return formatearNota(vlr * planCurso.getCurso().getCreditos());
    }

    public ItemNota aItem() {
        SituacionAcademica sit = situacion.get();
        String obs = observacion.get();
        return new ItemNota(planCurso.getId(), getNotaComoNumero(), sit == null ? null : sit.getId(),
                obs == null || obs.isBlank() ? null : obs.trim());
    }

    private static String formatearNota(double vlr) {
        if (vlr == Math.rint(vlr)) {
            return String.valueOf((long) vlr);
        }
        return String.valueOf(vlr);
    }
}
