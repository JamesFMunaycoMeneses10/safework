package com.safework.safework;

import static org.assertj.core.api.Assertions.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.ActiveProfiles;
import com.safework.safework.model.*;
import com.safework.safework.repository.*;
import com.safework.safework.tenancy.EmpresaContexto;

/** Integración real. Solo escribe en una BD de pruebas, nunca en safework_db. */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmpresaAislamientoTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired PlatformTransactionManager manager;
    @Autowired AreaRepository areas;
    @Autowired TrabajadorRepository trabajadores;
    @Autowired ArchivoAdjuntoRepository archivos;
    private Long empresaA, empresaB;
    private Map<Class<?>, Object> idsA, idsB;

    static List<Class<? extends EmpresaEntidad>> entidades() {
        return List.of(Area.class, Usuario.class, Trabajador.class, Riesgo.class, Incidente.class,
                Inspeccion.class, HallazgoInspeccion.class, AccionCorrectiva.class,
                AccionCorrectivaEvento.class, AuditoriaEvento.class, ArchivoAdjunto.class, EntregaEpp.class);
    }

    @BeforeAll void preparar() {
        String catalogo = jdbc.execute((ConnectionCallback<String>) connection -> connection.getCatalog());
        assertThat(catalogo).as("Usar una BD desechable con sufijo _ci o nombre que contenga _test")
                .satisfies(c -> assertThat(c.endsWith("_ci") || c.contains("_test")).isTrue());
        String sufijo = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        empresaA = crearEmpresa("aislamiento-a-" + sufijo);
        empresaB = crearEmpresa("aislamiento-b-" + sufijo);
        idsA = crearDatos(empresaA, "A" + sufijo);
        idsB = crearDatos(empresaB, "B" + sufijo);
    }

    private Long crearEmpresa(String codigo) {
        jdbc.update("INSERT INTO empresas(nombre,codigo,estado,fecha_registro) VALUES(?,?,'ACTIVO',CURRENT_TIMESTAMP)", codigo, codigo);
        return jdbc.queryForObject("SELECT id FROM empresas WHERE codigo=?", Long.class, codigo);
    }

    private <T> T enEmpresa(Long id, Supplier<T> operacion) {
        return EmpresaContexto.ejecutar(id, () -> new TransactionTemplate(manager).execute(status -> operacion.get()));
    }

    private Map<Class<?>, Object> crearDatos(Long empresa, String marca) {
        return enEmpresa(empresa, () -> {
            Map<Class<?>, Object> result = new HashMap<>();
            Area area = guardar(new Area("Área " + marca, "Prueba aislada"), result);
            Usuario user = new Usuario("usuario" + marca, "$2a$10$hashSoloParaPersistenciaDePrueba", "TRABAJADOR", "Nombre", marca, null);
            guardar(user, result);
            Trabajador trabajador = new Trabajador("87654321", "Nombre", marca, "Cargo " + marca, area);
            trabajador.setUsuario(user);
            guardar(trabajador, result);
            Riesgo riesgo = guardar(new Riesgo("Peligro " + marca, "Descripción", 2, 2, 4, "Control", area), result);
            Incidente incidente = guardar(new Incidente(LocalDateTime.now(), "Incidente", area, trabajador, "Descripción", "Leve", "Reportado"), result);
            Inspeccion inspeccion = guardar(new Inspeccion(LocalDate.now(), "Seguridad", area, trabajador, "Programada", "Observación"), result);
            HallazgoInspeccion hallazgo = new HallazgoInspeccion();
            hallazgo.setDescripcion("Hallazgo " + marca); hallazgo.setInspeccion(inspeccion); hallazgo.setRiesgo(riesgo);
            guardar(hallazgo, result);
            AccionCorrectiva accion = guardar(new AccionCorrectiva("Acción " + marca, LocalDate.now(), LocalDate.now().plusDays(7),
                    "Pendiente", "Alta", trabajador, riesgo, incidente), result);
            guardar(new AccionCorrectivaEvento(accion, "ENTREGADA", user.getUsername(), LocalDateTime.now(), "Detalle", null, null, null), result);
            guardar(new AuditoriaEvento("RIESGOS", riesgo.getId(), "CREADO", user.getUsername(), LocalDateTime.now(), "Detalle " + marca), result);
            guardar(new ArchivoAdjunto("archivo-" + marca + ".pdf", "evidencia.pdf", "application/pdf", new byte[]{1,2,3}), result);
            EntregaEpp entrega = new EntregaEpp(); entrega.setTrabajador(trabajador); entrega.setRegistradoPor(user);
            entrega.setTipoEpp("Casco"); entrega.setFechaEntrega(LocalDate.now()); entrega.setCantidad(1);
            guardar(entrega, result);
            return result;
        });
    }

    private <T extends EmpresaEntidad> T guardar(T entity, Map<Class<?>, Object> ids) {
        em.persist(entity); em.flush();
        ids.put(entity.getClass(), emf.getPersistenceUnitUtil().getIdentifier(entity));
        return entity;
    }

    @ParameterizedTest @MethodSource("entidades")
    void listadosYConteosDeLosDoceModelosSoloMuestranLaEmpresaActual(Class<? extends EmpresaEntidad> tipo) {
        enEmpresa(empresaA, () -> {
            var rows = em.createQuery("from " + tipo.getSimpleName(), tipo).getResultList();
            assertThat(rows).hasSize(1).allSatisfy(row -> assertThat(row.getEmpresaId()).isEqualTo(empresaA));
            assertThat(em.createQuery("select count(e) from " + tipo.getSimpleName() + " e", Long.class).getSingleResult()).isEqualTo(1);
            return null;
        });
    }

    @ParameterizedTest @MethodSource("entidades")
    void busquedaDirectaPorIdNoRevelaRegistrosDeOtraEmpresa(Class<? extends EmpresaEntidad> tipo) {
        enEmpresa(empresaA, () -> {
            assertThat(em.find(tipo, idsA.get(tipo))).isNotNull();
            assertThat(em.find(tipo, idsB.get(tipo))).isNull();
            return null;
        });
    }

    @Test void filtrosEscalaresYArchivosTambienEstanAislados() {
        enEmpresa(empresaA, () -> {
            assertThat(trabajadores.listarCargos()).hasSize(1).allSatisfy(c -> assertThat(c).startsWith("Cargo A"));
            assertThat(trabajadores.buscarConFiltros(null, null, null)).hasSize(1);
            assertThat(archivos.findById((String) idsB.get(ArchivoAdjunto.class))).isEmpty();
            return null;
        });
    }

    @Test void guardarNoAceptaUnAreaDeOtraEmpresa() {
        assertThatThrownBy(() -> enEmpresa(empresaA, () -> {
            Area ajena = new Area(); ajena.setId((Long) idsB.get(Area.class));
            em.persist(new Riesgo("Peligro", "Descripción", 1, 1, 1, "Control", ajena));
            em.flush(); return null;
        })).isInstanceOf(RuntimeException.class);
    }

    @Test void noHayEmpresaPredeterminadaParaVisitantes() {
        SecurityContextHolder.clearContext();
        assertThat(EmpresaContexto.empresaActualId()).isEqualTo(-1L);
        assertThat(areas.findAll()).isEmpty();
        assertThatThrownBy(() -> areas.saveAndFlush(new Area("Sin empresa", "No permitido"))).isInstanceOf(RuntimeException.class);
    }

    @Test void contextoInternoSeLimpiaInclusoCuandoHayUnError() {
        assertThatThrownBy(() -> EmpresaContexto.ejecutar(empresaA, () -> { throw new IllegalArgumentException("Prueba"); }))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(EmpresaContexto.empresaActualId()).isEqualTo(-1L);
    }

    @Test void deleteByIdAjenoNoEliminaLaFila() {
        enEmpresa(empresaA, () -> { areas.deleteById((Long) idsB.get(Area.class)); return null; });
        assertThat(enEmpresa(empresaB, () -> areas.existsById((Long) idsB.get(Area.class)))).isTrue();
    }

    @Test void mergeConIdAjenoNoActualizaOtraEmpresa() {
        String nombreOriginal = enEmpresa(empresaB, () -> areas.findById((Long) idsB.get(Area.class)).orElseThrow().getNombre());
        assertThatThrownBy(() -> enEmpresa(empresaA, () -> {
            Area manipulada = new Area("Nombre manipulado", "No permitido");
            manipulada.setId((Long) idsB.get(Area.class));
            em.merge(manipulada); em.flush(); return null;
        })).isInstanceOf(RuntimeException.class);
        assertThat(enEmpresa(empresaB, () -> areas.findById((Long) idsB.get(Area.class)).orElseThrow().getNombre())).isEqualTo(nombreOriginal);
    }

    @Test void referenciaLazyNoRevelaContenidoDeUnaEmpresaAjena() {
        assertThatThrownBy(() -> enEmpresa(empresaA, () ->
                em.getReference(Area.class, idsB.get(Area.class)).getNombre()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test void dniSePuedeRepetirEntreEmpresasPeroNoDentroDeLaMisma() {
        assertThat(enEmpresa(empresaA, () -> trabajadores.existsByDni("87654321"))).isTrue();
        assertThat(enEmpresa(empresaB, () -> trabajadores.existsByDni("87654321"))).isTrue();
        assertThatThrownBy(() -> enEmpresa(empresaA, () -> {
            Area area = areas.findById((Long) idsA.get(Area.class)).orElseThrow();
            em.persist(new Trabajador("87654321", "Duplicado", "No permitido", "Cargo", area));
            em.flush(); return null;
        })).isInstanceOf(RuntimeException.class);
    }

    @AfterAll void limpiarSoloLosDatosCreadosPorEstaClase() {
        if (empresaA == null || empresaB == null) return;
        for (String tabla : List.of("entregas_epp", "acciones_correctivas_historial", "hallazgos_inspeccion",
                "acciones_correctivas", "auditoria_eventos", "archivos_adjuntos", "incidentes", "inspecciones",
                "riesgos", "trabajadores", "usuarios", "areas"))
            jdbc.update("DELETE FROM " + tabla + " WHERE empresa_id IN (?,?)", empresaA, empresaB);
        jdbc.update("DELETE FROM empresas WHERE id IN (?,?)", empresaA, empresaB);
    }
}
