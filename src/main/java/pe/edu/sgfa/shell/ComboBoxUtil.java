package pe.edu.sgfa.shell;

import java.util.List;

import javafx.scene.control.ComboBox;

import pe.edu.sgfa.common.domain.IdentifiableEntity;

public final class ComboBoxUtil {

    private ComboBoxUtil() {
    }

    public static <T extends IdentifiableEntity> void reemplazarPreservandoSeleccion(ComboBox<T> cmb, List<T> nvsItms) {
        Long idSlccnd = cmb.getValue() == null ? null : cmb.getValue().getId();
        cmb.getItems().setAll(nvsItms);
        if (idSlccnd != null) {
            nvsItms.stream()
                    .filter(item -> idSlccnd.equals(item.getId()))
                    .findFirst()
                    .ifPresent(cmb.getSelectionModel()::select);
        }
    }
}
