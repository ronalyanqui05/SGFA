CREATE TABLE fichas_generadas (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    correlativo        TEXT NOT NULL UNIQUE,
    numero_secuencial  INTEGER NOT NULL,
    anio               INTEGER NOT NULL,
    trayectoria_id     INTEGER NOT NULL REFERENCES trayectorias_academicas(id),
    archivo_path       TEXT NOT NULL,
    usuario_id         INTEGER REFERENCES usuarios(id),
    fecha_generacion   TEXT NOT NULL DEFAULT (datetime('now')),
    estado             TEXT NOT NULL DEFAULT 'GENERADA' CHECK (estado IN ('GENERADA', 'ANULADA')),
    observacion        TEXT,
    created_at         TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at         TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE (anio, numero_secuencial)
);
CREATE INDEX idx_fichas_generadas_trayectoria ON fichas_generadas(trayectoria_id);
CREATE INDEX idx_fichas_generadas_anio ON fichas_generadas(anio);
