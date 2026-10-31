package pe.edu.sgfa.shell;

import java.util.List;

import javafx.scene.control.ComboBox;

import pe.edu.sgfa.common.domain.RyeIdentifiableEntity;

public final class RyeComboBoxUtil {

    private RyeComboBoxUtil() {
    }

    public static <T extends RyeIdentifiableEntity> void ryeReemplazarPreservandoSeleccion(ComboBox<T> comboYnq, List<T> nuevosItemsYnq) {
        Long idSeleccionadoYnq = comboYnq.getValue() == null ? null : comboYnq.getValue().getId();
        comboYnq.getItems().setAll(nuevosItemsYnq);
        if (idSeleccionadoYnq != null) {
            nuevosItemsYnq.stream()
                    .filter(itemYnq -> idSeleccionadoYnq.equals(itemYnq.getId()))
                    .findFirst()
                    .ifPresent(comboYnq.getSelectionModel()::select);
        }
    }
}
