CREATE TABLE capacitaciones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    tema VARCHAR(150) NOT NULL,
    objetivo VARCHAR(1000) NOT NULL,
    instructor VARCHAR(150) NOT NULL,
    fecha_hora DATETIME(6) NOT NULL,
    duracion_minutos INT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    area_id BIGINT NOT NULL,
    registrado_por_id BIGINT NOT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_capacitacion_empresa (id, empresa_id),
    KEY ix_capacitacion_fecha (empresa_id, fecha_hora, id),
    CONSTRAINT fk_capacitacion_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_capacitacion_area FOREIGN KEY (area_id, empresa_id) REFERENCES areas(id, empresa_id),
    CONSTRAINT fk_capacitacion_actor FOREIGN KEY (registrado_por_id, empresa_id) REFERENCES usuarios(id, empresa_id),
    CONSTRAINT ck_capacitacion_duracion CHECK (duracion_minutos BETWEEN 5 AND 1440),
    CONSTRAINT ck_capacitacion_estado CHECK (estado IN ('PROGRAMADA','REALIZADA','CANCELADA'))
) ENGINE=InnoDB;

CREATE TABLE capacitacion_participantes (
    id BIGINT NOT NULL AUTO_INCREMENT, empresa_id BIGINT NOT NULL,
    capacitacion_id BIGINT NOT NULL, trabajador_id BIGINT NOT NULL,
    asistencia VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    PRIMARY KEY (id), UNIQUE KEY uk_capacitacion_trabajador (capacitacion_id, trabajador_id),
    CONSTRAINT fk_participante_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_participante_capacitacion FOREIGN KEY (capacitacion_id, empresa_id) REFERENCES capacitaciones(id, empresa_id),
    CONSTRAINT fk_participante_trabajador FOREIGN KEY (trabajador_id, empresa_id) REFERENCES trabajadores(id, empresa_id),
    CONSTRAINT ck_participante_asistencia CHECK (asistencia IN ('PENDIENTE','ASISTIO','FALTO'))
) ENGINE=InnoDB;
