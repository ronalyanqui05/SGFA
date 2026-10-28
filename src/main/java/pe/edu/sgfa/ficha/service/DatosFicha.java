package pe.edu.sgfa.ficha.service;

import java.util.List;

public record DatosFicha(String institucionNombre, String institucionSiglas, String institucionDireccion,
                          String logoPath, String estudianteNombreCompleto, String estudianteDni,
                          String estudianteCodigo, String programaNombre, String planCodigo, String planNombre,
                          List<DatosCiclo> ciclos) {
}
