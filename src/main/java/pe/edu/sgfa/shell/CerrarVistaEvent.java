package pe.edu.sgfa.shell;

import javafx.event.Event;
import javafx.event.EventType;

public class CerrarVistaEvent extends Event {

    public static final EventType<CerrarVistaEvent> CERRAR_VISTA =
            new EventType<>(Event.ANY, "CERRAR_VISTA");

    public CerrarVistaEvent() {
        super(CERRAR_VISTA);
    }
}
