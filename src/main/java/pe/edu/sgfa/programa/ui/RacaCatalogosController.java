package pe.edu.sgfa.programa.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.periodo.ui.YmclPeriodosTabController;
import pe.edu.sgfa.shell.RyeRefrescable;

@Component
public class RacaCatalogosController {

    @FXML
    private TabPane tabPane;

    @FXML
    private RacaProgramasTabController programasTabController;
    @FXML
    private RacaPlanesTabController planesTabController;
    @FXML
    private RacaCiclosTabController ciclosTabController;
    @FXML
    private RacaCursosTabController cursosTabController;
    @FXML
    private RacaPlanCursosTabController planCursosTabController;
    @FXML
    private YmclPeriodosTabController periodosTabController;

    @FXML
    public void initialize() {
        RyeRefrescable[] porPestanaCrr = {
                programasTabController, planesTabController, ciclosTabController,
                cursosTabController, planCursosTabController, periodosTabController
        };
        tabPane.getSelectionModel().selectedIndexProperty().addListener((obsCrr, antesCrr, indiceCrr) -> {
            int i = indiceCrr.intValue();
            if (i >= 0 && i < porPestanaCrr.length) {
                porPestanaCrr[i].ryeRefrescarAlMostrar();
            }
        });
    }
}
