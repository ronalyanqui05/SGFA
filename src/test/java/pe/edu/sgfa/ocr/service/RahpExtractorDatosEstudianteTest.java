package pe.edu.sgfa.ocr.service;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RahpExtractorDatosEstudianteTest {

    @Test
    void extraeDatosCuandoElTextoTraeEtiquetasClaras() {
        String textoHnc = """
                INSTITUTO DE EDUCACION SUPERIOR
                DNI: 45678912
                APELLIDO PATERNO: QUISPE
                APELLIDO MATERNO: MAMANI
                NOMBRES: JUAN CARLOS
                FECHA DE NACIMIENTO: 15/03/2001
                """;

        var datosHnc = RahpExtractorDatosEstudiante.rahpExtraer(textoHnc);

        assertThat(datosHnc.dni()).isEqualTo("45678912");
        assertThat(datosHnc.apellidoPaterno()).isEqualTo("QUISPE");
        assertThat(datosHnc.apellidoMaterno()).isEqualTo("MAMANI");
        assertThat(datosHnc.nombres()).isEqualTo("JUAN CARLOS");
        assertThat(datosHnc.fechaNacimiento()).isEqualTo(LocalDate.of(2001, 3, 15));
    }

    @Test
    void partePellidosJuntosComoMejorEsfuerzoCuandoNoHayPaternoMaternoSeparados() {
        String textoHnc = "Apellidos: PEREZ GOMEZ\nNombres: ANA";

        var datosHnc = RahpExtractorDatosEstudiante.rahpExtraer(textoHnc);

        assertThat(datosHnc.apellidoPaterno()).isEqualTo("PEREZ");
        assertThat(datosHnc.apellidoMaterno()).isEqualTo("GOMEZ");
    }

    @Test
    void noFallaYDevuelveCamposNulosConTextoVacioOIrrelevante() {
        var datosHnc = RahpExtractorDatosEstudiante.rahpExtraer("texto sin ningun dato reconocible");

        assertThat(datosHnc.dni()).isNull();
        assertThat(datosHnc.nombres()).isNull();
        assertThat(datosHnc.apellidoPaterno()).isNull();
        assertThat(datosHnc.fechaNacimiento()).isNull();

        var datosVacioHnc = RahpExtractorDatosEstudiante.rahpExtraer("");
        assertThat(datosVacioHnc.dni()).isNull();

        var datosNuloHnc = RahpExtractorDatosEstudiante.rahpExtraer(null);
        assertThat(datosNuloHnc.dni()).isNull();
    }
}
