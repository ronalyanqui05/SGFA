CREATE TABLE estudiantes (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo            TEXT UNIQUE,
    dni               TEXT UNIQUE,
    nombres           TEXT NOT NULL,
    apellido_paterno  TEXT NOT NULL,
    apellido_materno  TEXT,
    fecha_nacimiento  TEXT,
    sexo              TEXT CHECK (sexo IS NULL OR sexo IN ('M', 'F')),
    direccion         TEXT,
    telefono          TEXT,
    correo            TEXT,
    estado            TEXT NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    fecha_registro    TEXT NOT NULL DEFAULT (datetime('now')),
    created_at        TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at        TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX idx_estudiantes_apellidos_nombres ON estudiantes(apellido_paterno, apellido_materno, nombres);

CREATE TABLE trayectorias_academicas (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    estudiante_id   INTEGER NOT NULL REFERENCES estudiantes(id),
    programa_id     INTEGER NOT NULL REFERENCES programas_estudio(id),
    plan_estudio_id INTEGER NOT NULL REFERENCES planes_estudio(id),
    fecha_inicio    TEXT,
    fecha_fin       TEXT,
    estado          TEXT NOT NULL DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA', 'FINALIZADA', 'RETIRADA', 'TRASLADADA')),
    observacion     TEXT,
    created_at      TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at      TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX idx_trayectorias_estudiante ON trayectorias_academicas(estudiante_id);
CREATE INDEX idx_trayectorias_plan_estudio ON trayectorias_academicas(plan_estudio_id);
