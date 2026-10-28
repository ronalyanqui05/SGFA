package pe.edu.sgfa.programa.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import org.springframework.stereotype.Component;

import pe.edu.sgfa.periodo.ui.PeriodosTabController;
import pe.edu.sgfa.shell.Refrescable;

@Component
public class CatalogosController {

    @FXML
    private TabPane tabPane;

    @FXML
    private ProgramasTabController programasTabController;
    @FXML
    private PlanesTabController planesTabController;
    @FXML
    private CiclosTabController ciclosTabController;
    @FXML
    private CursosTabController cursosTabController;
    @FXML
    private PlanCursosTabController planCursosTabController;
    @FXML
    private PeriodosTabController periodosTabController;

    @FXML
    public void initialize() {
        Refrescable[] porPstn = {
                programasTabController, planesTabController, ciclosTabController,
                cursosTabController, planCursosTabController, periodosTabController
        };
        tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, antes, indice) -> {
            int i = indice.intValue();
            if (i >= 0 && i < porPstn.length) {
                porPstn[i].refrescarAlMostrar();
            }
        });
    }
}
