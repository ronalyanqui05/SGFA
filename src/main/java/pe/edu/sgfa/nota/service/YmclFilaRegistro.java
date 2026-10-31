package pe.edu.sgfa.nota.service;

import pe.edu.sgfa.nota.domain.YmclRegistroAcademico;
import pe.edu.sgfa.programa.domain.RacaPlanCurso;

public record YmclFilaRegistro(RacaPlanCurso planCurso, YmclRegistroAcademico registro) {
}
