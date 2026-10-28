package pe.edu.sgfa.nota.service;

import pe.edu.sgfa.nota.domain.RegistroAcademico;
import pe.edu.sgfa.programa.domain.PlanCurso;

public record FilaRegistro(PlanCurso planCurso, RegistroAcademico registro) {
}
