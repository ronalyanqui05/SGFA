CREATE TABLE auditoria (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id  INTEGER REFERENCES usuarios(id),
    accion      TEXT NOT NULL,
    entidad     TEXT,
    entidad_id  TEXT,
    detalle     TEXT,
    fecha       TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX idx_auditoria_usuario ON auditoria(usuario_id);
CREATE INDEX idx_auditoria_fecha ON auditoria(fecha);
