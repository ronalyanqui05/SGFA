package pe.edu.sgfa.ficha.service;

import net.sf.jasperreports.engine.JasperPrint;

import pe.edu.sgfa.ficha.domain.RacaFichaGenerada;

public record RacaFichaConfirmada(RacaFichaGenerada registro, JasperPrint print) {
}
