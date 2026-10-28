-- Esquema anterior a SaaS, extraído sin datos personales. No elimina tablas.
-- Las instalaciones existentes se basan en versión 1 tras un respaldo verificado.
CREATE TABLE `acciones_correctivas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `descripcion` varchar(255) NOT NULL,
  `estado` varchar(255) NOT NULL,
  `fecha_limite` date NOT NULL,
  `fecha_registro` date NOT NULL,
  `prioridad` varchar(255) NOT NULL,
  `incidente_id` bigint DEFAULT NULL,
  `trabajador_id` bigint DEFAULT NULL,
  `riesgo_id` bigint DEFAULT NULL,
  `evidencia_cierre` varchar(1000) DEFAULT NULL,
  `fecha_cierre` date DEFAULT NULL,
  `archivo_evidencia` varchar(40) DEFAULT NULL,
  `nombre_evidencia` varchar(255) DEFAULT NULL,
  `tipo_evidencia` varchar(50) DEFAULT NULL,
  `fecha_envio_revision` datetime(6) DEFAULT NULL,
  `fecha_revision` datetime(6) DEFAULT NULL,
  `observacion_revision` varchar(1000) DEFAULT NULL,
  `enviada_por_id` bigint DEFAULT NULL,
  `revisada_por_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKk7al5y3pv0xgb7a8htwa95iqh` (`incidente_id`),
  KEY `FKka7wls76rrh41j8j9pstmap1n` (`trabajador_id`),
  KEY `FK5piaj35ux9t3sh0jj01qdawrj` (`riesgo_id`),
  KEY `FKsg429aid6k7whsduhm5yilcbj` (`enviada_por_id`),
  KEY `FKlw4iqtw6dwbilbh15t2p6xuk9` (`revisada_por_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `acciones_correctivas_historial` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `archivo_evidencia` varchar(40) DEFAULT NULL,
  `detalle` varchar(1000) DEFAULT NULL,
  `fecha` datetime(6) NOT NULL,
  `nombre_evidencia` varchar(255) DEFAULT NULL,
  `tipo` varchar(20) NOT NULL,
  `tipo_evidencia` varchar(50) DEFAULT NULL,
  `usuario` varchar(255) NOT NULL,
  `accion_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbh5c9eq5sp0tgpu4lnamarlug` (`accion_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `archivos_adjuntos` (
  `nombre` varchar(40) NOT NULL,
  `contenido` longblob NOT NULL,
  `nombre_original` varchar(255) NOT NULL,
  `tipo_contenido` varchar(50) NOT NULL,
  PRIMARY KEY (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `areas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `descripcion` varchar(255) DEFAULT NULL,
  `nombre` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `auditoria_eventos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `detalle` varchar(2000) DEFAULT NULL,
  `fecha` datetime(6) NOT NULL,
  `modulo` varchar(40) NOT NULL,
  `operacion` varchar(20) NOT NULL,
  `registro_id` bigint NOT NULL,
  `usuario` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `entregas_epp` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cantidad` int NOT NULL,
  `fecha_entrega` date NOT NULL,
  `observaciones` varchar(500) DEFAULT NULL,
  `tipo_epp` varchar(100) NOT NULL,
  `registrado_por_id` bigint NOT NULL,
  `trabajador_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKb1kik27rhoo1a30wccno800fi` (`registrado_por_id`),
  KEY `FK3pyo4gccser5i9qo2va8f8vuo` (`trabajador_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `hallazgos_inspeccion` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `descripcion` varchar(500) NOT NULL,
  `inspeccion_id` bigint NOT NULL,
  `riesgo_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKlbusr1cnbupnt0m25cwyav6h8` (`inspeccion_id`),
  KEY `FKt4u9i1ixkrl63lun34q5d6otn` (`riesgo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `incidentes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `descripcion` varchar(500) NOT NULL,
  `estado` varchar(30) NOT NULL,
  `fecha_hora` datetime(6) NOT NULL,
  `gravedad` varchar(30) NOT NULL,
  `tipo` varchar(30) NOT NULL,
  `area_id` bigint NOT NULL,
  `trabajador_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK7hoqfl45j0y5xj0y7qga0gf35` (`area_id`),
  KEY `FK1tn39ktvljfvx6dtmtbwcnisl` (`trabajador_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `inspecciones` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `estado` varchar(30) NOT NULL,
  `fecha` date NOT NULL,
  `observaciones` varchar(500) DEFAULT NULL,
  `tipo` varchar(100) NOT NULL,
  `area_id` bigint NOT NULL,
  `responsable_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKks321xxuf6v8mw4mv3ndla3iv` (`area_id`),
  KEY `FK9eg0mmw84rkd3ocqjdyv2wvwd` (`responsable_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `riesgos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `descripcion` varchar(255) NOT NULL,
  `medida_control` varchar(255) DEFAULT NULL,
  `nivel_riesgo` int NOT NULL,
  `peligro` varchar(150) NOT NULL,
  `probabilidad` int NOT NULL,
  `severidad` int NOT NULL,
  `area_id` bigint NOT NULL,
  `foto_archivo` varchar(40) DEFAULT NULL,
  `foto_nombre` varchar(255) DEFAULT NULL,
  `foto_tipo` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKgk2w68bhfty1gxmt95wqua9x6` (`area_id`),
  CONSTRAINT `riesgos_chk_1` CHECK (((`probabilidad` <= 5) and (`probabilidad` >= 1))),
  CONSTRAINT `riesgos_chk_2` CHECK (((`severidad` >= 1) and (`severidad` <= 5)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `trabajadores` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `apellidos` varchar(100) NOT NULL,
  `cargo` varchar(100) NOT NULL,
  `dni` varchar(8) NOT NULL,
  `nombres` varchar(100) NOT NULL,
  `area_id` bigint NOT NULL,
  `usuario_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6li210jqt10hev0wj6qj482xs` (`dni`),
  UNIQUE KEY `UKsp6s9q5m9hl964prvcc4mdmwd` (`usuario_id`),
  KEY `FKsmdgs98xy2vch03559pwmjs2x` (`area_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `usuarios` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `password` varchar(255) NOT NULL,
  `rol` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  `apellido` varchar(255) DEFAULT NULL,
  `correo` varchar(255) DEFAULT NULL,
  `estado` varchar(255) DEFAULT NULL,
  `fecha_registro` datetime(6) DEFAULT NULL,
  `nombre` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKm2dvbwfge291euvmk6vkkocao` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE `acciones_correctivas` ADD CONSTRAINT `FK5piaj35ux9t3sh0jj01qdawrj` FOREIGN KEY (`riesgo_id`) REFERENCES `riesgos` (`id`);
ALTER TABLE `acciones_correctivas` ADD CONSTRAINT `FKk7al5y3pv0xgb7a8htwa95iqh` FOREIGN KEY (`incidente_id`) REFERENCES `incidentes` (`id`);
ALTER TABLE `acciones_correctivas` ADD CONSTRAINT `FKka7wls76rrh41j8j9pstmap1n` FOREIGN KEY (`trabajador_id`) REFERENCES `trabajadores` (`id`);
ALTER TABLE `acciones_correctivas` ADD CONSTRAINT `FKlw4iqtw6dwbilbh15t2p6xuk9` FOREIGN KEY (`revisada_por_id`) REFERENCES `usuarios` (`id`);
ALTER TABLE `acciones_correctivas` ADD CONSTRAINT `FKsg429aid6k7whsduhm5yilcbj` FOREIGN KEY (`enviada_por_id`) REFERENCES `usuarios` (`id`);
ALTER TABLE `acciones_correctivas_historial` ADD CONSTRAINT `FKbh5c9eq5sp0tgpu4lnamarlug` FOREIGN KEY (`accion_id`) REFERENCES `acciones_correctivas` (`id`);
ALTER TABLE `entregas_epp` ADD CONSTRAINT `FK3pyo4gccser5i9qo2va8f8vuo` FOREIGN KEY (`trabajador_id`) REFERENCES `trabajadores` (`id`);
ALTER TABLE `entregas_epp` ADD CONSTRAINT `FKb1kik27rhoo1a30wccno800fi` FOREIGN KEY (`registrado_por_id`) REFERENCES `usuarios` (`id`);
ALTER TABLE `hallazgos_inspeccion` ADD CONSTRAINT `FKlbusr1cnbupnt0m25cwyav6h8` FOREIGN KEY (`inspeccion_id`) REFERENCES `inspecciones` (`id`);
ALTER TABLE `hallazgos_inspeccion` ADD CONSTRAINT `FKt4u9i1ixkrl63lun34q5d6otn` FOREIGN KEY (`riesgo_id`) REFERENCES `riesgos` (`id`);
ALTER TABLE `incidentes` ADD CONSTRAINT `FK1tn39ktvljfvx6dtmtbwcnisl` FOREIGN KEY (`trabajador_id`) REFERENCES `trabajadores` (`id`);
ALTER TABLE `incidentes` ADD CONSTRAINT `FK7hoqfl45j0y5xj0y7qga0gf35` FOREIGN KEY (`area_id`) REFERENCES `areas` (`id`);
ALTER TABLE `inspecciones` ADD CONSTRAINT `FK9eg0mmw84rkd3ocqjdyv2wvwd` FOREIGN KEY (`responsable_id`) REFERENCES `trabajadores` (`id`);
ALTER TABLE `inspecciones` ADD CONSTRAINT `FKks321xxuf6v8mw4mv3ndla3iv` FOREIGN KEY (`area_id`) REFERENCES `areas` (`id`);
ALTER TABLE `riesgos` ADD CONSTRAINT `FKgk2w68bhfty1gxmt95wqua9x6` FOREIGN KEY (`area_id`) REFERENCES `areas` (`id`);
ALTER TABLE `trabajadores` ADD CONSTRAINT `FKbj2tq4b166h22cigjb8r3vm3t` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`);
ALTER TABLE `trabajadores` ADD CONSTRAINT `FKsmdgs98xy2vch03559pwmjs2x` FOREIGN KEY (`area_id`) REFERENCES `areas` (`id`);
