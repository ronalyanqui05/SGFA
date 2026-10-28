CREATE TABLE instituciones (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre     TEXT NOT NULL,
    siglas     TEXT,
    direccion  TEXT,
    telefono   TEXT,
    correo     TEXT,
    logo_path  TEXT,
    activo     INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE programas_estudio (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    institucion_id  INTEGER NOT NULL REFERENCES instituciones(id),
    codigo          TEXT NOT NULL,
    nombre          TEXT NOT NULL,
    descripcion     TEXT,
    duracion_ciclos INTEGER NOT NULL CHECK (duracion_ciclos > 0),
    activo          INTEGER NOT NULL DEFAULT 1,
    created_at      TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at      TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE (institucion_id, codigo)
);
CREATE INDEX idx_programas_estudio_institucion ON programas_estudio(institucion_id);

CREATE TABLE planes_estudio (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    programa_id  INTEGER NOT NULL REFERENCES programas_estudio(id),
    codigo       TEXT NOT NULL,
    nombre       TEXT NOT NULL,
    fecha_inicio TEXT,
    fecha_fin    TEXT,
    estado       TEXT NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'CERRADO', 'INACTIVO')),
    created_at   TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at   TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE (programa_id, codigo)
);
CREATE INDEX idx_planes_estudio_programa ON planes_estudio(programa_id);

CREATE TABLE ciclos (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_estudio_id INTEGER NOT NULL REFERENCES planes_estudio(id),
    numero          INTEGER NOT NULL CHECK (numero > 0),
    nombre          TEXT NOT NULL,
    orden           INTEGER NOT NULL,
    created_at      TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at      TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE (plan_estudio_id, numero)
);
CREATE INDEX idx_ciclos_plan_estudio ON ciclos(plan_estudio_id);

CREATE TABLE cursos (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo     TEXT NOT NULL UNIQUE,
    nombre     TEXT NOT NULL,
    creditos   INTEGER NOT NULL CHECK (creditos > 0),
    horas      INTEGER CHECK (horas IS NULL OR horas >= 0),
    tipo       TEXT,
    activo     INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE plan_cursos (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_estudio_id INTEGER NOT NULL REFERENCES planes_estudio(id),
    ciclo_id        INTEGER NOT NULL REFERENCES ciclos(id),
    curso_id        INTEGER NOT NULL REFERENCES cursos(id),
    orden           INTEGER NOT NULL DEFAULT 0,
    obligatorio     INTEGER NOT NULL DEFAULT 1,
    created_at      TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at      TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE (plan_estudio_id, ciclo_id, curso_id)
);
CREATE INDEX idx_plan_cursos_ciclo ON plan_cursos(ciclo_id);
CREATE INDEX idx_plan_cursos_curso ON plan_cursos(curso_id);
