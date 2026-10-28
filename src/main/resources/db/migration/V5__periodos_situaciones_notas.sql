CREATE TABLE periodos_academicos (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre       TEXT NOT NULL UNIQUE,
    anio         INTEGER NOT NULL,
    tipo         TEXT,
    fecha_inicio TEXT,
    fecha_fin    TEXT,
    estado       TEXT NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'CERRADO')),
    created_at   TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at   TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE situaciones_academicas (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo                TEXT NOT NULL UNIQUE,
    nombre                TEXT NOT NULL,
    cuenta_para_promedio  INTEGER NOT NULL DEFAULT 1,
    activo                INTEGER NOT NULL DEFAULT 1,
    created_at            TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at            TEXT NOT NULL DEFAULT (datetime('now'))
);

INSERT INTO situaciones_academicas (codigo, nombre, cuenta_para_promedio) VALUES
    ('APROBADO',      'Aprobado',      1),
    ('DESAPROBADO',   'Desaprobado',   1),
    ('PENDIENTE',     'Pendiente',     0),
    ('CONVALIDADO',   'Convalidado',   0),
    ('RETIRADO',      'Retirado',      0),
    ('RECUPERACION',  'Recuperacion',  1);

CREATE TABLE registros_academicos (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    trayectoria_id  INTEGER NOT NULL REFERENCES trayectorias_academicas(id),
    plan_curso_id   INTEGER NOT NULL REFERENCES plan_cursos(id),
    periodo_id      INTEGER NOT NULL REFERENCES periodos_academicos(id),
    nota_final      REAL CHECK (nota_final IS NULL OR (nota_final >= 0 AND nota_final <= 20)),
    situacion_id    INTEGER NOT NULL REFERENCES situaciones_academicas(id),
    fecha_registro  TEXT NOT NULL DEFAULT (datetime('now')),
    observacion     TEXT,
    usuario_id      INTEGER REFERENCES usuarios(id),
    created_at      TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at      TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE (trayectoria_id, plan_curso_id, periodo_id)
);
CREATE INDEX idx_registros_trayectoria ON registros_academicos(trayectoria_id);
CREATE INDEX idx_registros_periodo ON registros_academicos(periodo_id);
CREATE INDEX idx_registros_situacion ON registros_academicos(situacion_id);
