package pe.edu.sgfa.ocr.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import pe.edu.sgfa.nota.domain.SituacionAcademica;
import pe.edu.sgfa.nota.service.ItemNota;
import pe.edu.sgfa.ocr.service.ReconocedorActa.FilaActaDetectada;

public class FilaActaUI {

    private final StringProperty ordenNro = new SimpleStringProperty("");
    private final StringProperty matricula = new SimpleStringProperty("");
    private final StringProperty apellidoPaterno = new SimpleStringProperty("");
    private final StringProperty apellidoMaterno = new SimpleStringProperty("");
    private final StringProperty nombres = new SimpleStringProperty("");
    private final List<ObjectProperty<SituacionAcademica>> situaciones;
    private final List<StringProperty> notas;

    public FilaActaUI(int cantidadCursos) {
        situaciones = new ArrayList<>(cantidadCursos);
        notas = new ArrayList<>(cantidadCursos);
        for (int i = 0; i < cantidadCursos; i++) {
            situaciones.add(new SimpleObjectProperty<>());
            notas.add(new SimpleStringProperty(""));
        }
    }

    public static FilaActaUI desde(FilaActaDetectada dtctd,
                                    java.util.function.Function<String, SituacionAcademica> situacionDe) {
        FilaActaUI fl = new FilaActaUI(dtctd.notasPorCurso().size());
        fl.ordenNro.set(vacioSiNulo(dtctd.ordenNro()));
        fl.matricula.set(vacioSiNulo(dtctd.matricula()));
        fl.apellidoPaterno.set(vacioSiNulo(dtctd.apellidoPaterno()));
        fl.apellidoMaterno.set(vacioSiNulo(dtctd.apellidoMaterno()));
        fl.nombres.set(vacioSiNulo(dtctd.nombres()));
        for (int i = 0; i < dtctd.notasPorCurso().size(); i++) {
            var ntCrs = dtctd.notasPorCurso().get(i);
            if (ntCrs.situacionTexto() != null) {
                fl.situaciones.get(i).set(situacionDe.apply(ntCrs.situacionTexto()));
            }
            fl.notas.get(i).set(vacioSiNulo(ntCrs.notaTexto()));
        }
        return fl;
    }

    private static String vacioSiNulo(String txt) {
        return txt == null ? "" : txt;
    }

    public StringProperty ordenNroProperty() {
        return ordenNro;
    }

    public StringProperty matriculaProperty() {
        return matricula;
    }

    public StringProperty apellidoPaternoProperty() {
        return apellidoPaterno;
    }

    public StringProperty apellidoMaternoProperty() {
        return apellidoMaterno;
    }

    public StringProperty nombresProperty() {
        return nombres;
    }

    public ObjectProperty<SituacionAcademica> situacionProperty(int crs) {
        return situaciones.get(crs);
    }

    public StringProperty notaProperty(int crs) {
        return notas.get(crs);
    }

    public int cantidadCursos() {
        return notas.size();
    }

    public boolean tieneDatos() {
        return !matricula.get().isBlank() || !apellidoPaterno.get().isBlank() || !nombres.get().isBlank();
    }

    private Double notaComoNumero(int crs) {
        String txt = notas.get(crs).get();
        if (txt == null || txt.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(txt.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<ItemNota> aItems(List<Long> plnCrsIdPorClmn) {
        List<ItemNota> itms = new ArrayList<>();
        for (int i = 0; i < cantidadCursos(); i++) {
            SituacionAcademica stcn = situaciones.get(i).get();
            if (stcn == null) {
                continue;
            }
            itms.add(new ItemNota(plnCrsIdPorClmn.get(i), notaComoNumero(i), stcn.getId(), null));
        }
        return itms;
    }
}
