package pe.edu.sgfa.shell;

import javafx.event.Event;
import javafx.event.EventType;

public class RyeCerrarVistaEvent extends Event {

    public static final EventType<RyeCerrarVistaEvent> CERRAR_VISTA =
            new EventType<>(Event.ANY, "CERRAR_VISTA");

    public RyeCerrarVistaEvent() {
        super(CERRAR_VISTA);
    }
}
