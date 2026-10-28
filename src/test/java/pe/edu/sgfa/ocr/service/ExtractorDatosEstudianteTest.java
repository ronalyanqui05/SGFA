package pe.edu.sgfa.ocr.service;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractorDatosEstudianteTest {

    @Test
    void extraeDatosCuandoElTextoTraeEtiquetasClaras() {
        String txt = """
                INSTITUTO DE EDUCACION SUPERIOR
                DNI: 45678912
                APELLIDO PATERNO: QUISPE
                APELLIDO MATERNO: MAMANI
                NOMBRES: JUAN CARLOS
                FECHA DE NACIMIENTO: 15/03/2001
                """;

        var dts = ExtractorDatosEstudiante.extraer(txt);

        assertThat(dts.dni()).isEqualTo("45678912");
        assertThat(dts.apellidoPaterno()).isEqualTo("QUISPE");
        assertThat(dts.apellidoMaterno()).isEqualTo("MAMANI");
        assertThat(dts.nombres()).isEqualTo("JUAN CARLOS");
        assertThat(dts.fechaNacimiento()).isEqualTo(LocalDate.of(2001, 3, 15));
    }

    @Test
    void partePellidosJuntosComoMejorEsfuerzoCuandoNoHayPaternoMaternoSeparados() {
        String txt = "Apellidos: PEREZ GOMEZ\nNombres: ANA";

        var dts = ExtractorDatosEstudiante.extraer(txt);

        assertThat(dts.apellidoPaterno()).isEqualTo("PEREZ");
        assertThat(dts.apellidoMaterno()).isEqualTo("GOMEZ");
    }

    @Test
    void noFallaYDevuelveCamposNulosConTextoVacioOIrrelevante() {
        var dts = ExtractorDatosEstudiante.extraer("texto sin ningun dato reconocible");

        assertThat(dts.dni()).isNull();
        assertThat(dts.nombres()).isNull();
        assertThat(dts.apellidoPaterno()).isNull();
        assertThat(dts.fechaNacimiento()).isNull();

        var dtsVc = ExtractorDatosEstudiante.extraer("");
        assertThat(dtsVc.dni()).isNull();

        var dtsNl = ExtractorDatosEstudiante.extraer(null);
        assertThat(dtsNl.dni()).isNull();
    }
}
