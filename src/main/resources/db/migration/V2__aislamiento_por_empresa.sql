-- La empresa inicial conserva todos los registros anteriores y sus claves.
CREATE TABLE empresas (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 nombre VARCHAR(150) NOT NULL,
 codigo VARCHAR(50) NOT NULL,
 estado VARCHAR(20) NOT NULL,
 fecha_registro DATETIME(6) NOT NULL,
 CONSTRAINT uk_empresa_codigo UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
INSERT INTO empresas(id,nombre,codigo,estado,fecha_registro)
VALUES(1,'Empresa principal','PRINCIPAL','ACTIVO',CURRENT_TIMESTAMP(6));
ALTER TABLE `acciones_correctivas` ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE `acciones_correctivas` ADD CONSTRAINT `fk_acciones_correctivas_empresa` FOREIGN KEY(empresa_id) REFERENCES empresas(id);
ALTER TABLE `acciones_correctivas` ADD CONSTRAINT `uk_acciones_correctivas_empresa` UNIQUE (`id`,empresa_id);
ALTER TABLE `acciones_correctivas` ADD INDEX `ix_acciones_correctivas_empresa` (empresa_id);
ALTER TABLE `acciones_correctivas` ALTER COLUMN empresa_id DROP DEFAULT;

ALTER TABLE `acciones_correctivas_historial` ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE `acciones_correctivas_historial` ADD CONSTRAINT `fk_acciones_correctivas_historial_empresa` FOREIGN KEY(empresa_id) REFERENCES empresas(id);
ALTER TABLE `acciones_correctivas_historial` ADD CONSTRAINT `uk_acciones_correctivas_historial_empresa` UNIQUE (`id`,empresa_id);
ALTER TABLE `acciones_correctivas_historial` ADD INDEX `ix_acciones_correctivas_historial_empresa` (empresa_id);
ALTER TABLE `acciones_correctivas_historial` ALTER COLUMN empresa_id DROP DEFAULT;

ALTER TABLE `archivos_adjuntos` ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE `archivos_adjuntos` ADD CONSTRAINT `fk_archivos_adjuntos_empresa` FOREIGN KEY(empresa_id) REFERENCES empresas(id);
ALTER TABLE `archivos_adjuntos` ADD CONSTRAINT `uk_archivos_adjuntos_empresa` UNIQUE (`nombre`,empresa_id);
ALTER TABLE `archivos_adjuntos` ADD INDEX `ix_archivos_adjuntos_empresa` (empresa_id);
ALTER TABLE `archivos_adjuntos` ALTER COLUMN empresa_id DROP DEFAULT;

ALTER TABLE `areas` ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE `areas` ADD CONSTRAINT `fk_areas_empresa` FOREIGN KEY(empresa_id) REFERENCES empresas(id);
ALTER TABLE `areas` ADD CONSTRAINT `uk_areas_empresa` UNIQUE (`id`,empresa_id);
ALTER TABLE `areas` ADD INDEX `ix_areas_empresa` (empresa_id);
ALTER TABLE `areas` ALTER COLUMN empresa_id DROP DEFAULT;

ALTER TABLE `auditoria_eventos` ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE `auditoria_eventos` ADD CONSTRAINT `fk_auditoria_eventos_empresa` FOREIGN KEY(empresa_id) REFERENCES empresas(id);
ALTER TABLE `auditoria_eventos` ADD CONSTRAINT `uk_auditoria_eventos_empresa` UNIQUE (`id`,empresa_id);
ALTER TABLE `auditoria_eventos` ADD INDEX `ix_auditoria_eventos_empresa` (empresa_id);
ALTER TABLE `auditoria_eventos` ALTER COLUMN empresa_id DROP DEFAULT;

ALTER TABLE `entregas_epp` ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE `entregas_epp` ADD CONSTRAINT `fk_entregas_epp_empresa` FOREIGN KEY(empresa_id) REFERENCES empresas(id);
ALTER TABLE `entregas_epp` ADD CONSTRAINT `uk_entregas_epp_empresa` UNIQUE (`id`,empresa_id);
ALTER TABLE `entregas_epp` ADD INDEX `ix_entregas_epp_empresa` (empresa_id);
ALTER TABLE `entregas_epp` ALTER COLUMN empresa_id DROP DEFAULT;

ALTER TABLE `hallazgos_inspeccion` ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE `hallazgos_inspeccion` ADD CONSTRAINT `fk_hallazgos_inspeccion_empresa` FOREIGN KEY(empresa_id) REFERENCES empresas(id);
ALTER TABLE `hallazgos_inspeccion` ADD CONSTRAINT `uk_hallazgos_inspeccion_empresa` UNIQUE (`id`,empresa_id);
ALTER TABLE `hallazgos_inspeccion` ADD INDEX `ix_hallazgos_inspeccion_empresa` (empresa_id);
ALTER TABLE `hallazgos_inspeccion` ALTER COLUMN empresa_id DROP DEFAULT;

ALTER TABLE `incidentes` ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE `incidentes` ADD CONSTRAINT `fk_incidentes_empresa` FOREIGN KEY(empresa_id) REFERENCES empresas(id);
ALTER TABLE `incidentes` ADD CONSTRAINT `uk_incidentes_empresa` UNIQUE (`id`,empresa_id);
ALTER TABLE `incidentes` ADD INDEX `ix_incidentes_empresa` (empresa_id);
ALTER TABLE `incidentes` ALTER COLUMN empresa_id DROP DEFAULT;

ALTER TABLE `inspecciones` ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE `inspecciones` ADD CONSTRAINT `fk_inspecciones_empresa` FOREIGN KEY(empresa_id) REFERENCES empresas(id);
ALTER TABLE `inspecciones` ADD CONSTRAINT `uk_inspecciones_empresa` UNIQUE (`id`,empresa_id);
ALTER TABLE `inspecciones` ADD INDEX `ix_inspecciones_empresa` (empresa_id);
ALTER TABLE `inspecciones` ALTER COLUMN empresa_id DROP DEFAULT;

ALTER TABLE `riesgos` ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE `riesgos` ADD CONSTRAINT `fk_riesgos_empresa` FOREIGN KEY(empresa_id) REFERENCES empresas(id);
ALTER TABLE `riesgos` ADD CONSTRAINT `uk_riesgos_empresa` UNIQUE (`id`,empresa_id);
ALTER TABLE `riesgos` ADD INDEX `ix_riesgos_empresa` (empresa_id);
ALTER TABLE `riesgos` ALTER COLUMN empresa_id DROP DEFAULT;

ALTER TABLE `trabajadores` ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE `trabajadores` ADD CONSTRAINT `fk_trabajadores_empresa` FOREIGN KEY(empresa_id) REFERENCES empresas(id);
ALTER TABLE `trabajadores` ADD CONSTRAINT `uk_trabajadores_empresa` UNIQUE (`id`,empresa_id);
ALTER TABLE `trabajadores` ADD INDEX `ix_trabajadores_empresa` (empresa_id);
ALTER TABLE `trabajadores` ALTER COLUMN empresa_id DROP DEFAULT;

ALTER TABLE `usuarios` ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE `usuarios` ADD CONSTRAINT `fk_usuarios_empresa` FOREIGN KEY(empresa_id) REFERENCES empresas(id);
ALTER TABLE `usuarios` ADD CONSTRAINT `uk_usuarios_empresa` UNIQUE (`id`,empresa_id);
ALTER TABLE `usuarios` ADD INDEX `ix_usuarios_empresa` (empresa_id);
ALTER TABLE `usuarios` ALTER COLUMN empresa_id DROP DEFAULT;

-- Defensa adicional en MySQL contra asociaciones entre empresas.
ALTER TABLE `acciones_correctivas` ADD CONSTRAINT `fk_empresa_rel_1` FOREIGN KEY(`riesgo_id`,empresa_id) REFERENCES `riesgos`(`id`,empresa_id);
ALTER TABLE `acciones_correctivas` ADD CONSTRAINT `fk_empresa_rel_2` FOREIGN KEY(`incidente_id`,empresa_id) REFERENCES `incidentes`(`id`,empresa_id);
ALTER TABLE `acciones_correctivas` ADD CONSTRAINT `fk_empresa_rel_3` FOREIGN KEY(`trabajador_id`,empresa_id) REFERENCES `trabajadores`(`id`,empresa_id);
ALTER TABLE `acciones_correctivas` ADD CONSTRAINT `fk_empresa_rel_4` FOREIGN KEY(`revisada_por_id`,empresa_id) REFERENCES `usuarios`(`id`,empresa_id);
ALTER TABLE `acciones_correctivas` ADD CONSTRAINT `fk_empresa_rel_5` FOREIGN KEY(`enviada_por_id`,empresa_id) REFERENCES `usuarios`(`id`,empresa_id);
ALTER TABLE `acciones_correctivas_historial` ADD CONSTRAINT `fk_empresa_rel_6` FOREIGN KEY(`accion_id`,empresa_id) REFERENCES `acciones_correctivas`(`id`,empresa_id);
ALTER TABLE `entregas_epp` ADD CONSTRAINT `fk_empresa_rel_7` FOREIGN KEY(`trabajador_id`,empresa_id) REFERENCES `trabajadores`(`id`,empresa_id);
ALTER TABLE `entregas_epp` ADD CONSTRAINT `fk_empresa_rel_8` FOREIGN KEY(`registrado_por_id`,empresa_id) REFERENCES `usuarios`(`id`,empresa_id);
ALTER TABLE `hallazgos_inspeccion` ADD CONSTRAINT `fk_empresa_rel_9` FOREIGN KEY(`inspeccion_id`,empresa_id) REFERENCES `inspecciones`(`id`,empresa_id);
ALTER TABLE `hallazgos_inspeccion` ADD CONSTRAINT `fk_empresa_rel_10` FOREIGN KEY(`riesgo_id`,empresa_id) REFERENCES `riesgos`(`id`,empresa_id);
ALTER TABLE `incidentes` ADD CONSTRAINT `fk_empresa_rel_11` FOREIGN KEY(`trabajador_id`,empresa_id) REFERENCES `trabajadores`(`id`,empresa_id);
ALTER TABLE `incidentes` ADD CONSTRAINT `fk_empresa_rel_12` FOREIGN KEY(`area_id`,empresa_id) REFERENCES `areas`(`id`,empresa_id);
ALTER TABLE `inspecciones` ADD CONSTRAINT `fk_empresa_rel_13` FOREIGN KEY(`responsable_id`,empresa_id) REFERENCES `trabajadores`(`id`,empresa_id);
ALTER TABLE `inspecciones` ADD CONSTRAINT `fk_empresa_rel_14` FOREIGN KEY(`area_id`,empresa_id) REFERENCES `areas`(`id`,empresa_id);
ALTER TABLE `riesgos` ADD CONSTRAINT `fk_empresa_rel_15` FOREIGN KEY(`area_id`,empresa_id) REFERENCES `areas`(`id`,empresa_id);
ALTER TABLE `trabajadores` ADD CONSTRAINT `fk_empresa_rel_16` FOREIGN KEY(`usuario_id`,empresa_id) REFERENCES `usuarios`(`id`,empresa_id);
ALTER TABLE `trabajadores` ADD CONSTRAINT `fk_empresa_rel_17` FOREIGN KEY(`area_id`,empresa_id) REFERENCES `areas`(`id`,empresa_id);

-- Un mismo DNI puede existir en empresas distintas; el usuario de login es globalmente único.
ALTER TABLE trabajadores DROP INDEX UK6li210jqt10hev0wj6qj482xs;
ALTER TABLE trabajadores ADD CONSTRAINT uk_trabajador_empresa_dni UNIQUE(empresa_id,dni);