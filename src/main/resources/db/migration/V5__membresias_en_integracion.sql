CREATE TABLE planes_membresia (
    codigo VARCHAR(30) NOT NULL PRIMARY KEY, nombre VARCHAR(100) NOT NULL,
    precio DECIMAL(12,2) NOT NULL, moneda CHAR(3) NOT NULL, dias INT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_plan_precio CHECK (precio > 0), CONSTRAINT ck_plan_dias CHECK (dias BETWEEN 1 AND 366)
) ENGINE=InnoDB;
-- Tarifa ficticia para la integracion academica. No habilita cobros reales.
INSERT INTO planes_membresia(codigo,nombre,precio,moneda,dias) VALUES('SST_MENSUAL','SafeWork SST mensual',49.00,'PEN',30);

CREATE TABLE pagos_membresia (
    referencia CHAR(36) NOT NULL PRIMARY KEY, empresa_id BIGINT NOT NULL,
    solicitud_id CHAR(36) NOT NULL, plan_codigo VARCHAR(30) NOT NULL,
    importe DECIMAL(12,2) NOT NULL, moneda CHAR(3) NOT NULL, dias INT NOT NULL,
    estado VARCHAR(30) NOT NULL, preferencia_id VARCHAR(150), checkout_url VARCHAR(1000),
    pago_externo VARCHAR(100), creado_en DATETIME(6) NOT NULL, aprobado_en DATETIME(6),
    UNIQUE KEY uk_solicitud_empresa (empresa_id,solicitud_id),
    UNIQUE KEY uk_pago_externo (pago_externo),
    KEY ix_pagos_empresa (empresa_id,creado_en),
    CONSTRAINT fk_pago_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_pago_plan FOREIGN KEY (plan_codigo) REFERENCES planes_membresia(codigo)
) ENGINE=InnoDB;
CREATE TABLE suscripciones_empresa (
    empresa_id BIGINT NOT NULL PRIMARY KEY, vigente_hasta DATETIME(6),
    actualizado_en DATETIME(6) NOT NULL,
    CONSTRAINT fk_suscripcion_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id)
) ENGINE=InnoDB;
CREATE TABLE eventos_pago (
    clave CHAR(64) NOT NULL PRIMARY KEY, referencia CHAR(36) NOT NULL,
    estado VARCHAR(30) NOT NULL, recibido_en DATETIME(6) NOT NULL,
    CONSTRAINT fk_evento_pago FOREIGN KEY (referencia) REFERENCES pagos_membresia(referencia)
) ENGINE=InnoDB;
