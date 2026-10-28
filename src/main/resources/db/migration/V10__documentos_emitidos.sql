CREATE TABLE documentos_emitidos (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo                 TEXT NOT NULL CHECK (tipo IN ('ACTA', 'ORDEN_MERITO')),
    correlativo          TEXT NOT NULL UNIQUE,
    numero_secuencial    INTEGER NOT NULL,
    anio                 INTEGER NOT NULL,
    titulo               TEXT NOT NULL,
    descripcion          TEXT,
    codigo_verificacion  TEXT NOT NULL,
    archivo_path         TEXT NOT NULL,
    usuario_id           INTEGER REFERENCES usuarios(id),
    fecha_emision        TEXT NOT NULL,
    estado               TEXT NOT NULL DEFAULT 'EMITIDO' CHECK (estado IN ('EMITIDO', 'ANULADO')),
    observacion          TEXT,
    created_at           TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
    updated_at           TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ', 'now')),
    UNIQUE (tipo, anio, numero_secuencial)
);
CREATE INDEX idx_documentos_emitidos_tipo_anio ON documentos_emitidos(tipo, anio);
