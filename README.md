# Post-contenido — Unidad 6: Antipatrones de Diseño

## Descripción
Repositorio del post-contenido de la Unidad 6 de Patrones de Diseño de Software — Sexto Semestre. Un único proyecto Spring Boot (`pedidos-service/`) desarrollado en dos partes:
1. Diagnóstico y refactorización de antipatrones combinados (**God Object** y **Spaghetti Code**) en la clase `GestorPedidos`.
2. Diagnóstico y corrección de un segundo antipatrón (**Golden Hammer**) introducido durante un ciclo de crecimiento del sistema al incorporar tres campañas de descuento promocional.

---

## Decisiones de diseño

### Parte 1 — Diagnóstico y Refactorización de GestorPedidos

#### 1. Antipatrones Identificados con Evidencia Concreta del Código
En la versión inicial de `GestorPedidos.java`, se identificaron dos antipatrones fuertemente acoplados: **God Object** y **Spaghetti Code**.

* **God Object (Violación del Principio de Responsabilidad Única - SRP):**
  La clase `GestorPedidos` y su método principal `procesarPedido()` asumían el control total de seis (6) responsabilidades totalmente dispares:
  - **Líneas 34–46:** Validación de stock e inventario mediante consultas SQL directas contra la base de datos.
  - **Líneas 49–78:** Validación de existencia de cliente, consulta de facturas pendientes de mora y verificación de regla de horario de corte (20:00).
  - **Líneas 81–86:** Cálculo de subtotal mediante consultas SQL iterativas por cada ítem del pedido.
  - **Líneas 89–106:** Cálculo de porcentajes de descuento basados en tipo de cliente e historial de pedidos.
  - **Líneas 112–126:** Persistencia relacional directa vía `JdbcTemplate` (inserción en `pedidos`, consulta de identidad generada `CALL IDENTITY()`, inserción en `detalle_pedido` y actualización del inventario en `inventario` sin delimitación transaccional explícita).
  - **Líneas 129–142:** Formateo y construcción de cuerpo de correo electrónico en texto plano y despacho mediante `EmailService`.
  - **Líneas 149–188:** Seis métodos auxiliares privados (`obtenerHistorialCliente`, `formatearFactura`, `calcularImpuestoRegional`, `reintentarNotificacion`, `purgarPedidosVencidos`, `construirCuerpoCorreo`) que inflaban la clase a más de 190 líneas de responsabilidades no cohesivas.

* **Spaghetti Code (Mezcla de niveles de abstracción y alta complejidad ciclomática):**
  - **Múltiples niveles de anidamiento condicional:** El bloque de cálculo de descuento (líneas 89–106) alcanzaba hasta 3 niveles de anidamiento condicional (`if (tipoCliente.equals("VIP")) -> if (subtotal > 1_000_000) ... else if ...`), al igual que la validación de mora (líneas 60–77).
  - **Mezcla caótica de abstracciones:** En una sola secuencia de líneas coexistían sentencias SQL crudas (`SELECT`, `INSERT`, `UPDATE`), lógica de dominio puro (reglas de mora y porcentajes de descuento) y construcción de cadenas para presentación (`StringBuilder.append("Subtotal: $")...`).
  - **Rigidez ante el cambio:** Si se requería un nuevo tipo de cliente con reglas de descuento propias, se obligaba a modificar el método monolítico en múltiples puntos, con alto riesgo de regresiones colaterales.

#### 2. Patrones de Diseño Seleccionados y Justificación
Para resolver estos problemas se estructuró una arquitectura en cuatro capas cohesivas:

* **Chain of Responsibility (`ValidadorPedido`):**
  - **Por qué se aplica:** La secuencia de validaciones tiene una **dependencia real de orden y necesidad de corte anticipado (*short-circuit*)**: si el inventario no tiene stock suficiente (`ValidadorStock`), no tiene sentido consultar en base de datos la mora del cliente (`ValidadorCliente`).
  - **Alternativa descartada:** Una lista de predicados `List<Predicate<ContextoPedido>>`. Fue descartada porque evalúa predicados sin corte anticipado limpio, obligando a ejecutar todas las consultas aún si la primera falla.

* **Strategy (`EstrategiaDescuento`):**
  - **Por qué se aplica:** El cálculo de descuento **no depende de un orden de ejecución ni necesita cortar el flujo del pedido**. Para un pedido dado, aplica exactamente una estrategia en función del tipo de cliente (`DescuentoVip`, `DescuentoFrecuente`, `DescuentoEstandar`), seleccionada en $O(1)$ a través de `SelectorEstrategiaDescuento`.
  - **Alternativa descartada:** Modelar el descuento como eslabón de la cadena de validación. Descartado porque desvirtúa el contrato de la cadena (que es validar/rechazar) y requiere mecanismos artificiales para evitar colisiones.

* **Separación de Persistencia y Notificación:**
  - `PedidoRepository`: Encapsula operaciones SQL y transacciones de persistencia.
  - `NotificacionPedidoService`: Encapsula la construcción de mensajes y despacho de correos.
  - `GestorPedidos`: Pasa a ser un **orquestador delgado** (~35 líneas) que coordina las cuatro capas.

---

### Parte 2 — Diagnóstico de Golden Hammer en el Crecimiento del Sistema

#### 1. Antipatrón Identificado: Golden Hammer (Martillo Dorado)
Al solicitarse tres nuevas campañas promocionales (`BLACK_FRIDAY`, `CORPORATIVO` y `VOLUMEN`), se cometió el antipatrón **Golden Hammer**: reutilizar indiscriminadamente una solución conocida (**Chain of Responsibility**) simplemente porque "ya existía y funcionó en la Parte 1", sin evaluar si el nuevo problema tenía la misma estructura.

#### 2. Evidencia Concreta del Código
* **Ausencia total de dependencia de orden:**
  Las clases `PromocionBlackFriday`, `PromocionCorporativo` y `PromocionVolumen` no tienen ninguna relación de precedencia entre sí, ni con los validadores reales (`ValidadorStock` y `ValidadorCliente`). Ejecutar `PromocionVolumen` antes o después de `PromocionCorporativo` es totalmente irrelevante para el resultado, a diferencia de `ValidadorStock`, que obligatoriamente debe ejecutarse antes que `ValidadorCliente`.
* **Violación flagrante del contrato de la abstracción:**
  La clase base se llama `ValidadorPedido`, y su contrato explícito es: *"decidir si el pedido continúa o se rechaza"*. Las clases de promoción **nunca rechazan nada**; simplemente se "engancharon" parasitariamente a la cadena para mutar un estado.
* **Corrupción del contexto con estado mutable compartido:**
  Se agregó el campo `descuentoCampana` a `ContextoPedido`. Las promociones compiten por mutar esta variable (`if (valor > this.descuentoCampana) this.descuentoCampana = valor;`). Si en el futuro las campañas necesitaran combinarse de otra forma (por ejemplo, acumularse en lugar de tomar el máximo), la cadena introduce ambigüedades incontrolables y acoplamiento temporal oculto.

#### 3. Plan de Corrección y Prevención de Lava Flow
* **Corrección:** Modelar las campañas como implementaciones de `EstrategiaDescuento` (`DescuentoBlackFriday`, `DescuentoCorporativo`, `DescuentoVolumen`) e introducir `CalculadorDescuentoFinal`.
* **Prevención de Lava Flow:** Las clases `Promocion*` y el campo `descuentoCampana` deben ser **eliminados por completo del código fuente activo mediante `git rm`**, en lugar de dejarse comentados "por si acaso". El historial de Git es el repositorio de la evolución arquitectónica, no el código activo.

---

## Cómo ejecutar

```bash
# Ejecutar la aplicación Spring Boot
mvn spring-boot:run

# Ejecutar la suite completa de pruebas
mvn test
```

## Herramientas utilizadas
- Java 17
- Spring Boot 3.2.5 (Spring Web, Spring Data JDBC)
- H2 Database (modo embebido en memoria)
- Maven 3.9+
- JUnit 5