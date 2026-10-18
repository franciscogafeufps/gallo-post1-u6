# Post-contenido — Unidad 6: Antipatrones de Diseño

## Descripción
Repositorio del post-contenido de la Unidad 6 de Patrones de Diseño de Software — Sexto Semestre. Un único proyecto Spring Boot (`pedidos-service/`) desarrollado en dos partes:
1. Diagnóstico y refactorización de antipatrones combinados (**God Object** y **Spaghetti Code**) en la clase `GestorPedidos`.
2. Diagnóstico y corrección de un segundo antipatrón (**Golden Hammer**) introducido durante un ciclo de crecimiento del sistema al incorporar tres campañas de descuento promocional.

---

## Decisiones de diseño

### Parte 1 — Diagnóstico de GestorPedidos

#### 1. Antipatrones Identificados con Evidencia Concreta del Código
En la versión inicial de `GestorPedidos.java`, se identificaron dos antipatrones fuertemente acoplados: **God Object** y **Spaghetti Code**.

* **God Object (Violación del Principio de Responsabilidad Única - SRP):**
  La clase `GestorPedidos` y su método principal `procesarPedido()` asumen el control total de seis (6) responsabilidades totalmente dispares:
  - **Líneas 34–46:** Validación de stock e inventario mediante consultas SQL directas contra la base de datos.
  - **Líneas 49–78:** Validación de existencia de cliente, consulta de facturas pendientes de mora y verificación de regla de horario de corte (20:00).
  - **Líneas 81–86:** Cálculo de subtotal mediante consultas SQL iterativas por cada ítem del pedido.
  - **Líneas 89–106:** Cálculo de porcentajes de descuento basados en tipo de cliente e historial de pedidos.
  - **Líneas 112–126:** Persistencia relacional directa vía `JdbcTemplate` (inserción en `pedidos`, consulta de identidad generada `CALL IDENTITY()`, inserción en `detalle_pedido` y actualización del inventario en `inventario` sin delimitación transaccional explícita).
  - **Líneas 129–142:** Formateo y construcción de cuerpo de correo electrónico en texto plano y despacho mediante `EmailService`.
  - **Líneas 149–188:** Seis métodos auxiliares privados (`obtenerHistorialCliente`, `formatearFactura`, `calcularImpuestoRegional`, `reintentarNotificacion`, `purgarPedidosVencidos`, `construirCuerpoCorreo`) que inflan la clase a más de 190 líneas de responsabilidades no cohesivas.

* **Spaghetti Code (Mezcla de niveles de abstracción y alta complejidad ciclomática):**
  - **Múltiples niveles de anidamiento condicional:** El bloque de cálculo de descuento (líneas 89–106) alcanza hasta 3 niveles de anidamiento condicional (`if (tipoCliente.equals("VIP")) -> if (subtotal > 1_000_000) ... else if ...`), al igual que la validación de mora (líneas 60–77).
  - **Mezcla caótica de abstracciones:** En una sola secuencia de líneas coexisten sentencias SQL crudas (`SELECT`, `INSERT`, `UPDATE`), lógica de dominio puro (reglas de mora y porcentajes de descuento) y construcción de cadenas para presentación (`StringBuilder.append("Subtotal: $")...`).
  - **Rigidez ante el cambio:** Si se necesitara agregar un nuevo tipo de cliente con reglas de descuento propias (por ejemplo, cliente `CORPORATIVO`), se requeriría modificar directamente el método `procesarPedido` en múltiples puntos (líneas 49–78 y líneas 89–106), aumentando drásticamente el riesgo de introducir regresiones.

#### 2. Patrones de Diseño Seleccionados y Justificación
Para resolver estos problemas se estructuró una arquitectura en capas cohesivas:

* **Chain of Responsibility (`ValidadorPedido`):**
  - **Por qué se aplica:** La secuencia de validaciones tiene una **dependencia real de orden y necesidad de corte anticipado (*short-circuit*)**: si el inventario de un producto no tiene stock suficiente (`ValidadorStock`), no tiene sentido computacional ni de red consultar en base de datos la mora del cliente (`ValidadorCliente`).
  - **Alternativa descartada:** Se consideró implementar un método `validarTodo()` con una lista de predicados `List<Predicate<ContextoPedido>>`. Sin embargo, fue descartada porque evalúa predicados sin un mecanismo limpio de corte anticipado y obligaría a recorrer toda la lista aún cuando la primera validación haya fallado de manera crítica.

* **Strategy (`EstrategiaDescuento`):**
  - **Por qué se aplica:** A diferencia de las validaciones, el cálculo de descuento **no depende de un orden de ejecución ni necesita cortar el flujo del pedido**. Para un pedido dado, aplica exactamente una estrategia en función del tipo de cliente (`DescuentoVip`, `DescuentoFrecuente`, `DescuentoEstandar`), seleccionada en $O(1)$ a través de `SelectorEstrategiaDescuento` sin anidamientos condicionales.
  - **Alternativa descartada:** Modelar el descuento como un eslabón adicional en la cadena de validación. Se descartó porque obligaría a desvirtuar el contrato de la cadena (que es decidir si el pedido continúa o se rechaza) y requeriría mecanismos artificiales para evitar que múltiples eslabones alteren el valor.

* **Separación de Persistencia y Notificación:**
  - `PedidoRepository`: Encapsula todas las operaciones SQL y transacciones de persistencia.
  - `NotificacionPedidoService`: Encapsula la construcción de plantillas de texto y el despacho de correos.
  - `GestorPedidos`: Pasa a ser un **orquestador delgado** que coordina la cadena de validación, la estrategia de descuento, el repositorio y el servicio de notificación.

---

### Parte 2 — Crecimiento del proyecto
*(Se completará durante la Parte 2)*

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