package pe.edu.sgfa.ficha.service;

import net.sf.jasperreports.engine.JasperPrint;

import pe.edu.sgfa.ficha.domain.FichaGenerada;

public record FichaConfirmada(FichaGenerada registro, JasperPrint print) {
}
