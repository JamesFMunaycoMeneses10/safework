ALTER TABLE usuarios ADD COLUMN version_acceso BIGINT NOT NULL DEFAULT 0;
CREATE TABLE recuperacion_claves (
    token_hash CHAR(64) NOT NULL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    creado_en DATETIME(6) NOT NULL,
    expira_en DATETIME(6) NOT NULL,
    KEY ix_recuperacion_usuario (usuario_id),
    CONSTRAINT fk_recuperacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;
