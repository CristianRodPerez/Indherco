package com.indherco.postes.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indherco.postes.audit.AuditLog;
import com.indherco.postes.audit.AuditLogRepository;
import com.indherco.postes.auth.security.JwtService;
import com.indherco.postes.auth.security.SecurityUser;
import com.indherco.postes.products.Product;
import com.indherco.postes.products.ProductRepository;
import com.indherco.postes.shared.enums.BaseRole;
import com.indherco.postes.shared.enums.EntityType;
import com.indherco.postes.shared.enums.MovementType;
import com.indherco.postes.stockmovements.StockMovement;
import com.indherco.postes.stockmovements.StockMovementRepository;
import com.indherco.postes.supplies.Supply;
import com.indherco.postes.supplies.SupplyRepository;
import com.indherco.postes.users.User;
import com.indherco.postes.users.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CriticalBusinessFlowsIT {

    private static final String TEST_JWT_SECRET = "indherco-integration-test-secret-with-more-than-32-characters";
    private static final String PASSWORD = "ClaveSegura123";
    private static final List<String> TABLES_IN_DELETE_ORDER = List.of(
        "daily_alert_dismissals",
        "idempotency_records",
        "audit_logs",
        "alerts",
        "office_inventory_movements",
        "office_inventory_items",
        "daily_closings",
        "stock_movements",
        "supplies",
        "products",
        "users"
    );

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
        .withDatabaseName("indherco_integration")
        .withUsername("indherco_test")
        .withPassword("indherco_test_password");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("app.security.jwt-secret", () -> TEST_JWT_SECRET);
        registry.add("app.security.jwt-expiration-minutes", () -> 30);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtService jwtService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    SupplyRepository supplyRepository;

    @Autowired
    StockMovementRepository movementRepository;

    @Autowired
    AuditLogRepository auditLogRepository;

    @BeforeEach
    void cleanDatabase() {
        TABLES_IN_DELETE_ORDER.forEach(table -> jdbcTemplate.update("DELETE FROM " + table));
    }

    @Nested
    @DisplayName("Login y autenticacion")
    class AuthenticationScenarios {

        @Test
        @DisplayName("Given credenciales validas When inicia sesion Then retorna JWT y usuario autenticado")
        void validCredentialsReturnJwtAndAuthenticatedUser() throws Exception {
            // Given
            User user = createUser("operador.login", BaseRole.OPERADOR, true, false, false, true);

            // When
            MvcResult loginResult = login("operador.login", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("operador.login"))
                .andExpect(jsonPath("$.user.baseRole").value("OPERADOR"))
                .andReturn();

            // Then
            String token = responseJson(loginResult).get("token").asText();
            assertThat(token.split("\\.")).hasSize(3);
            mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.username").value("operador.login"));

            User persisted = userRepository.findById(user.getId()).orElseThrow();
            assertThat(persisted.getLastLoginAt()).isNotNull();
            assertThat(passwordEncoder.matches(PASSWORD, persisted.getPasswordHash())).isTrue();
        }

        @Test
        @DisplayName("Given contrasena incorrecta When inicia sesion Then responde 401 y registra el intento")
        void wrongPasswordReturnsUnauthorizedAndRecordsAttempt() throws Exception {
            // Given
            User user = createUser("operador.error", BaseRole.OPERADOR, true, false, false, true);

            // When / Then
            login("operador.error", "ClaveIncorrecta")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Usuario o contrasena incorrectos."));

            User persisted = userRepository.findById(user.getId()).orElseThrow();
            assertThat(persisted.getFailedLoginAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("Given usuario inexistente When inicia sesion Then responde 401 sin revelar su ausencia")
        void nonexistentUserReturnsUnauthorized() throws Exception {
            // Given
            assertThat(userRepository.findByUsername("no.existe")).isEmpty();

            // When / Then
            login("no.existe", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Usuario o contrasena incorrectos."));

            assertThat(userRepository.count()).isZero();
        }

        @Test
        @DisplayName("Given usuario deshabilitado When inicia sesion Then responde 401")
        void disabledUserCannotLogin() throws Exception {
            // Given
            createUser("operador.inactivo", BaseRole.OPERADOR, true, false, false, false);

            // When / Then
            login("operador.inactivo", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
        }

        @Test
        @DisplayName("Given cinco intentos fallidos When vuelve a iniciar sesion Then el bloqueo persiste")
        void repeatedFailuresPersistTemporaryLock() throws Exception {
            // Given
            User user = createUser("operador.bloqueado", BaseRole.OPERADOR, true, false, false, true);

            // When
            for (int attempt = 0; attempt < 5; attempt++) {
                login("operador.bloqueado", "ClaveIncorrecta")
                    .andExpect(status().isUnauthorized());
            }

            // Then
            User locked = userRepository.findById(user.getId()).orElseThrow();
            assertThat(locked.getFailedLoginAttempts()).isEqualTo(5);
            assertThat(locked.getLockedUntil()).isAfter(LocalDateTime.now());
            login("operador.bloqueado", PASSWORD)
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("USER_TEMPORARILY_LOCKED"));
        }
    }

    @Nested
    @DisplayName("Seguridad JWT y permisos")
    class SecurityScenarios {

        @Test
        @DisplayName("Given endpoint protegido When no envia token Then responde 401")
        void protectedEndpointWithoutTokenReturnsUnauthorized() throws Exception {
            // Given / When / Then
            mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Given token invalido When accede a endpoint protegido Then responde 401")
        void invalidTokenReturnsUnauthorized() throws Exception {
            // Given / When / Then
            mockMvc.perform(get("/api/auth/me")
                    .header(HttpHeaders.AUTHORIZATION, bearer("token-invalido")))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Given token expirado When accede a endpoint protegido Then responde 401")
        void expiredTokenReturnsUnauthorized() throws Exception {
            // Given
            User user = createUser("operador.expirado", BaseRole.OPERADOR, true, false, false, true);
            String expiredToken = expiredToken(user.getUsername());

            // When / Then
            mockMvc.perform(get("/api/auth/me")
                    .header(HttpHeaders.AUTHORIZATION, bearer(expiredToken)))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Given usuario sin permiso When registra produccion Then responde 403 y no cambia stock")
        void userWithWrongRoleReturnsForbidden() throws Exception {
            // Given
            User officeUser = createUser("oficina.sin.permiso", BaseRole.OFICINA, false, false, false, true);
            Product product = createProduct("Poste 8m", 10);

            // When / Then
            authenticatedPost("/api/movements/production", tokenFor(officeUser), Map.of(
                "movementDate", LocalDate.now(),
                "productId", product.getId(),
                "quantity", 2
            )).andExpect(status().isForbidden());

            assertThat(productRepository.findById(product.getId()).orElseThrow().getCurrentStock()).isEqualTo(10);
            assertThat(movementRepository.count()).isZero();
        }

        @Test
        @DisplayName("Given administrador autorizado When consulta usuarios Then accede correctamente")
        void authorizedAdminCanAccessRestrictedEndpoint() throws Exception {
            // Given
            User admin = createUser("admin.integracion", BaseRole.ADMIN_OFICINA, true, true, true, true);

            // When / Then
            mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin.integracion"));
        }
    }

    @Nested
    @DisplayName("Gestion de produccion")
    class ProductionScenarios {

        @Test
        @DisplayName("Given produccion valida When se registra Then persiste movimiento, stock y auditoria")
        void validProductionPersistsMovementStockUserDateAndAudit() throws Exception {
            // Given
            User operator = createUser("produccion.uno", BaseRole.OPERADOR, true, false, false, true);
            Product product = createProduct("Poste 9m", 10);
            LocalDate movementDate = LocalDate.now().minusDays(1);

            // When
            authenticatedPost("/api/movements/production", tokenFor(operator), Map.of(
                "movementDate", movementDate,
                "productId", product.getId(),
                "quantity", 7,
                "rejectedQuantity", 1,
                "observation", "Turno completado"
            ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movementType").value("PRODUCCION"))
                .andExpect(jsonPath("$.previousStock").value(10))
                .andExpect(jsonPath("$.newStock").value(17))
                .andExpect(jsonPath("$.registeredBy").value(operator.getName()))
                .andExpect(jsonPath("$.movementDate").value(movementDate.toString()));

            // Then
            List<StockMovement> movements = movementRepository.findAll();
            assertThat(movements).hasSize(1);
            StockMovement movement = movements.getFirst();
            assertThat(movement.getMovementType()).isEqualTo(MovementType.PRODUCCION);
            assertThat(movement.getEntityType()).isEqualTo(EntityType.PRODUCTO);
            assertThat(movement.getQuantity()).isEqualTo(7);
            assertThat(movement.getPreviousStock()).isEqualTo(10);
            assertThat(movement.getNewStock()).isEqualTo(17);
            assertThat(movement.getMovementDate()).isEqualTo(movementDate);
            assertThat(movement.getRegisteredAt()).isNotNull();
            assertThat(movement.getRegisteredBy().getId()).isEqualTo(operator.getId());
            assertThat(productRepository.findById(product.getId()).orElseThrow().getCurrentStock()).isEqualTo(17);
            assertAudit("CREATE_PRODUCTION", operator, "StockMovement", movement.getId());
        }

        @Test
        @DisplayName("Given cantidad negativa When registra produccion Then responde 400 sin persistir")
        void negativeProductionQuantityIsRejected() throws Exception {
            // Given
            User operator = createUser("produccion.negativa", BaseRole.OPERADOR, true, false, false, true);
            Product product = createProduct("Poste 6m", 8);

            // When / Then
            authenticatedPost("/api/movements/production", tokenFor(operator), Map.of(
                "movementDate", LocalDate.now(),
                "productId", product.getId(),
                "quantity", -2
            ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("quantity")));

            assertNoMovementAndStock(product, 8);
        }

        @Test
        @DisplayName("Given producto inexistente When registra produccion Then responde 404")
        void nonexistentProductIsRejected() throws Exception {
            // Given
            User operator = createUser("produccion.inexistente", BaseRole.OPERADOR, true, false, false, true);

            // When / Then
            authenticatedPost("/api/movements/production", tokenFor(operator), Map.of(
                "movementDate", LocalDate.now(),
                "productId", 999999L,
                "quantity", 3
            ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Producto no encontrado."));

            assertThat(movementRepository.count()).isZero();
        }

        @Test
        @DisplayName("Given producto obligatorio ausente When registra produccion Then responde 400")
        void missingProductIsRejectedByValidation() throws Exception {
            // Given
            User operator = createUser("produccion.sin.producto", BaseRole.OPERADOR, true, false, false, true);

            // When / Then
            authenticatedPost("/api/movements/production", tokenFor(operator), Map.of(
                "movementDate", LocalDate.now(),
                "quantity", 3
            ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("productId")));

            assertThat(movementRepository.count()).isZero();
        }

        @Test
        @DisplayName("Given cantidad obligatoria ausente When registra produccion Then responde 400")
        void missingQuantityIsRejectedByValidation() throws Exception {
            // Given
            User operator = createUser("produccion.sin.cantidad", BaseRole.OPERADOR, true, false, false, true);
            Product product = createProduct("Poste 11m", 5);

            // When / Then
            authenticatedPost("/api/movements/production", tokenFor(operator), Map.of(
                "movementDate", LocalDate.now(),
                "productId", product.getId()
            ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("quantity")));

            assertNoMovementAndStock(product, 5);
        }
    }

    @Nested
    @DisplayName("Gestion de consumos")
    class ConsumptionScenarios {

        @Test
        @DisplayName("Given consumo valido When se registra Then descuenta stock y genera auditoria")
        void validConsumptionPersistsMovementUpdatesStockAndAudit() throws Exception {
            // Given
            User operator = createUser("consumo.uno", BaseRole.OPERADOR, false, false, true, true);
            Supply supply = createSupply("Rollo de alambre", 20);

            // When
            authenticatedPost("/api/movements/consumption", tokenFor(operator), Map.of(
                "movementDate", LocalDate.now(),
                "supplyId", supply.getId(),
                "quantity", 6,
                "processArea", "Produccion"
            ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movementType").value("CONSUMO"))
                .andExpect(jsonPath("$.previousStock").value(20))
                .andExpect(jsonPath("$.newStock").value(14));

            // Then
            List<StockMovement> movements = movementRepository.findAll();
            assertThat(movements).hasSize(1);
            StockMovement movement = movements.getFirst();
            assertThat(movement.getMovementType()).isEqualTo(MovementType.CONSUMO);
            assertThat(movement.getRegisteredBy().getId()).isEqualTo(operator.getId());
            assertThat(supplyRepository.findById(supply.getId()).orElseThrow().getCurrentStock()).isEqualTo(14);
            assertAudit("CREATE_CONSUMPTION", operator, "StockMovement", movement.getId());
        }

        @Test
        @DisplayName("Given consumo mayor al stock When se registra Then rechaza y conserva inventario")
        void consumptionAboveStockIsRejectedWithoutInventoryChange() throws Exception {
            // Given
            User operator = createUser("consumo.exceso", BaseRole.OPERADOR, false, false, true, true);
            Supply supply = createSupply("Cemento", 4);

            // When / Then
            authenticatedPost("/api/movements/consumption", tokenFor(operator), Map.of(
                "movementDate", LocalDate.now(),
                "supplyId", supply.getId(),
                "quantity", 5
            ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No hay stock suficiente para consumir esta cantidad."));

            assertThat(supplyRepository.findById(supply.getId()).orElseThrow().getCurrentStock()).isEqualTo(4);
            assertThat(movementRepository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Gestion de despachos")
    class DispatchScenarios {

        @Test
        @DisplayName("Given despacho valido When se registra Then descuenta stock y genera auditoria")
        void validDispatchPersistsMovementUpdatesStockAndAudit() throws Exception {
            // Given
            User operator = createUser("despacho.uno", BaseRole.OPERADOR, false, true, false, true);
            Product product = createProduct("Poste 8m", 15);

            // When
            authenticatedPost("/api/movements/dispatch", tokenFor(operator), Map.of(
                "movementDate", LocalDate.now(),
                "productId", product.getId(),
                "quantity", 4,
                "clientOrDestination", "Cliente prueba"
            ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movementType").value("DESPACHO"))
                .andExpect(jsonPath("$.previousStock").value(15))
                .andExpect(jsonPath("$.newStock").value(11));

            // Then
            List<StockMovement> movements = movementRepository.findAll();
            assertThat(movements).hasSize(1);
            StockMovement movement = movements.getFirst();
            assertThat(movement.getMovementType()).isEqualTo(MovementType.DESPACHO);
            assertThat(movement.getRegisteredBy().getId()).isEqualTo(operator.getId());
            assertThat(productRepository.findById(product.getId()).orElseThrow().getCurrentStock()).isEqualTo(11);
            assertAudit("CREATE_DISPATCH", operator, "StockMovement", movement.getId());
        }

        @Test
        @DisplayName("Given despacho mayor al stock When se registra Then rechaza y conserva inventario")
        void dispatchAboveStockIsRejectedWithoutInventoryChange() throws Exception {
            // Given
            User operator = createUser("despacho.exceso", BaseRole.OPERADOR, false, true, false, true);
            Product product = createProduct("Poste 6m", 3);

            // When / Then
            authenticatedPost("/api/movements/dispatch", tokenFor(operator), Map.of(
                "movementDate", LocalDate.now(),
                "productId", product.getId(),
                "quantity", 4
            ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No hay stock suficiente para despachar esta cantidad."));

            assertNoMovementAndStock(product, 3);
        }
    }

    @Nested
    @DisplayName("Consulta y actualizacion de inventario")
    class InventoryScenarios {

        @Test
        @DisplayName("Given movimientos reales When consulta stock Then refleja produccion, despacho y consumo")
        void stockQueriesReflectAutomaticMovementUpdates() throws Exception {
            // Given
            User admin = createUser("admin.stock", BaseRole.ADMIN_OFICINA, true, true, true, true);
            Product product = createProduct("Poste 9m", 10);
            Supply supply = createSupply("Rollo galvanizado", 20);
            String token = tokenFor(admin);

            // When
            authenticatedPost("/api/movements/production", token, Map.of(
                "movementDate", LocalDate.now(),
                "productId", product.getId(),
                "quantity", 5
            )).andExpect(status().isOk());

            authenticatedPost("/api/movements/dispatch", token, Map.of(
                "movementDate", LocalDate.now(),
                "productId", product.getId(),
                "quantity", 3
            )).andExpect(status().isOk());

            authenticatedPost("/api/movements/consumption", token, Map.of(
                "movementDate", LocalDate.now(),
                "supplyId", supply.getId(),
                "quantity", 4
            )).andExpect(status().isOk());

            // Then
            mockMvc.perform(get("/api/products").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Poste 9m"))
                .andExpect(jsonPath("$[0].currentStock").value(12));

            mockMvc.perform(get("/api/supplies").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Rollo galvanizado"))
                .andExpect(jsonPath("$[0].currentStock").value(16));

            assertThat(movementRepository.count()).isEqualTo(3);
            assertThat(productRepository.findById(product.getId()).orElseThrow().getCurrentStock()).isEqualTo(12);
            assertThat(supplyRepository.findById(supply.getId()).orElseThrow().getCurrentStock()).isEqualTo(16);
        }
    }

    private ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password
            ))));
    }

    private ResultActions authenticatedPost(String path, String token, Object body) throws Exception {
        return mockMvc.perform(post(path)
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)));
    }

    private JsonNode responseJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String tokenFor(User user) {
        return jwtService.generateToken(new SecurityUser(user));
    }

    private String expiredToken(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(username)
            .issuedAt(Date.from(now.minusSeconds(120)))
            .expiration(Date.from(now.minusSeconds(60)))
            .signWith(Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();
    }

    private User createUser(
        String username,
        BaseRole role,
        boolean production,
        boolean dispatch,
        boolean consumption,
        boolean active
    ) {
        User user = new User();
        user.setName("Usuario " + username);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setBaseRole(role);
        user.setCanRegisterProduction(production);
        user.setCanRegisterDispatch(dispatch);
        user.setCanRegisterConsumption(consumption);
        user.setActive(active);
        user.setLastPasswordChangeAt(LocalDateTime.now());
        return userRepository.saveAndFlush(user);
    }

    private Product createProduct(String name, int currentStock) {
        Product product = new Product();
        product.setName(name);
        product.setType("Poste");
        product.setUnitOfMeasure("UNIDAD");
        product.setCurrentStock(currentStock);
        product.setMinimumStock(0);
        product.setActive(true);
        return productRepository.saveAndFlush(product);
    }

    private Supply createSupply(String name, int currentStock) {
        Supply supply = new Supply();
        supply.setName(name);
        supply.setCategory("Produccion");
        supply.setUnitOfMeasure("UNIDAD");
        supply.setCurrentStock(currentStock);
        supply.setMinimumStock(0);
        supply.setActive(true);
        return supplyRepository.saveAndFlush(supply);
    }

    private void assertNoMovementAndStock(Product product, int expectedStock) {
        assertThat(productRepository.findById(product.getId()).orElseThrow().getCurrentStock()).isEqualTo(expectedStock);
        assertThat(movementRepository.count()).isZero();
    }

    private void assertAudit(String action, User user, String entity, Long entityId) {
        AuditLog audit = auditLogRepository.findAll().stream()
            .filter(candidate -> action.equals(candidate.getAction()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No se encontro auditoria para " + action));

        assertThat(audit.getUserId()).isEqualTo(user.getId());
        assertThat(audit.getUsername()).isEqualTo(user.getUsername());
        assertThat(audit.getAction()).isEqualTo(action);
        assertThat(audit.getEntity()).isEqualTo(entity);
        assertThat(audit.getEntityId()).isEqualTo(entityId);
        assertThat(audit.getOccurredAt()).isNotNull();
    }
}
