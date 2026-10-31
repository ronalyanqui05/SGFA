package pe.edu.sgfa.ocr.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import pe.edu.sgfa.nota.domain.YmclSituacionAcademica;
import pe.edu.sgfa.nota.service.YmclItemNota;
import pe.edu.sgfa.ocr.service.RahpReconocedorActa.RahpFilaActaDetectada;

public class RahpFilaActaUI {

    private final StringProperty ordenNroHnc = new SimpleStringProperty("");
    private final StringProperty matriculaHnc = new SimpleStringProperty("");
    private final StringProperty apellidoPaternoHnc = new SimpleStringProperty("");
    private final StringProperty apellidoMaternoHnc = new SimpleStringProperty("");
    private final StringProperty nombresHnc = new SimpleStringProperty("");
    private final List<ObjectProperty<YmclSituacionAcademica>> situacionesHnc;
    private final List<StringProperty> notasHnc;

    public RahpFilaActaUI(int cantidadCursosHnc) {
        situacionesHnc = new ArrayList<>(cantidadCursosHnc);
        notasHnc = new ArrayList<>(cantidadCursosHnc);
        for (int i = 0; i < cantidadCursosHnc; i++) {
            situacionesHnc.add(new SimpleObjectProperty<>());
            notasHnc.add(new SimpleStringProperty(""));
        }
    }

    public static RahpFilaActaUI desde(RahpFilaActaDetectada detectadaHnc,
                                    java.util.function.Function<String, YmclSituacionAcademica> situacionDeHnc) {
        RahpFilaActaUI filaHnc = new RahpFilaActaUI(detectadaHnc.notasPorCurso().size());
        filaHnc.ordenNroHnc.set(rahpVacioSiNulo(detectadaHnc.ordenNro()));
        filaHnc.matriculaHnc.set(rahpVacioSiNulo(detectadaHnc.matricula()));
        filaHnc.apellidoPaternoHnc.set(rahpVacioSiNulo(detectadaHnc.apellidoPaterno()));
        filaHnc.apellidoMaternoHnc.set(rahpVacioSiNulo(detectadaHnc.apellidoMaterno()));
        filaHnc.nombresHnc.set(rahpVacioSiNulo(detectadaHnc.nombres()));
        for (int i = 0; i < detectadaHnc.notasPorCurso().size(); i++) {
            var notaCursoHnc = detectadaHnc.notasPorCurso().get(i);
            if (notaCursoHnc.situacionTexto() != null) {
                filaHnc.situacionesHnc.get(i).set(situacionDeHnc.apply(notaCursoHnc.situacionTexto()));
            }
            filaHnc.notasHnc.get(i).set(rahpVacioSiNulo(notaCursoHnc.notaTexto()));
        }
        return filaHnc;
    }

    private static String rahpVacioSiNulo(String textoHnc) {
        return textoHnc == null ? "" : textoHnc;
    }

    public StringProperty ordenNroProperty() {
        return ordenNroHnc;
    }

    public StringProperty matriculaProperty() {
        return matriculaHnc;
    }

    public StringProperty apellidoPaternoProperty() {
        return apellidoPaternoHnc;
    }

    public StringProperty apellidoMaternoProperty() {
        return apellidoMaternoHnc;
    }

    public StringProperty nombresProperty() {
        return nombresHnc;
    }

    public ObjectProperty<YmclSituacionAcademica> situacionProperty(int cursoHnc) {
        return situacionesHnc.get(cursoHnc);
    }

    public StringProperty notaProperty(int cursoHnc) {
        return notasHnc.get(cursoHnc);
    }

    public int rahpCantidadCursos() {
        return notasHnc.size();
    }

    public boolean rahpTieneDatos() {
        return !matriculaHnc.get().isBlank() || !apellidoPaternoHnc.get().isBlank() || !nombresHnc.get().isBlank();
    }

    private Double rahpNotaComoNumero(int cursoHnc) {
        String textoHnc = notasHnc.get(cursoHnc).get();
        if (textoHnc == null || textoHnc.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(textoHnc.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<YmclItemNota> rahpAItems(List<Long> planCursoIdPorColumnaHnc) {
        List<YmclItemNota> itemsHnc = new ArrayList<>();
        for (int i = 0; i < rahpCantidadCursos(); i++) {
            YmclSituacionAcademica situacionHnc = situacionesHnc.get(i).get();
            if (situacionHnc == null) {
                continue;
            }
            itemsHnc.add(new YmclItemNota(planCursoIdPorColumnaHnc.get(i), rahpNotaComoNumero(i), situacionHnc.getId(), null));
        }
        return itemsHnc;
    }
}
