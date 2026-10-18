# Post-contenido — Unidad 6: Antipatrones de Diseño

## Descripción
Repositorio del post-contenido de la Unidad 6 de Patrones de Diseño de Software — Sexto Semestre. Un único proyecto Spring Boot (`pedidos-service/`) con dos partes: diagnóstico y refactorización de un antipatrón combinado en `GestorPedidos`, y diagnóstico y corrección de un segundo antipatrón introducido al hacer crecer el mismo proyecto con tres campañas de descuento.

---

## Decisiones de diseño

### Parte 1 — GestorPedidos

#### 1. Antipatrón identificado
**God Object** y **Spaghetti Code** combinados.  
El método `GestorPedidos.procesarPedido()` concentraba seis (6) responsabilidades completamente distintas en un único método de más de 100 líneas, dentro de una clase de más de 190 líneas de código con métodos auxiliares privados no cohesivos:
1. **Validación de stock e inventario (líneas 34–46):** Comprobación de lista de ítems e interacción directa con base de datos mediante SQL embebido (`SELECT stock FROM inventario WHERE producto_id = ?`).
2. **Validación de cliente y estado de mora (líneas 49–78):** Consulta SQL de clientes (`SELECT tipo_cliente FROM clientes WHERE id = ?`), verificación de deuda pendiente (`SELECT SUM(monto) FROM facturas...`) y regla de corte horario a las 20:00 con hasta 3 niveles de anidamiento condicional.
3. **Cálculo de subtotal (líneas 81–86):** Iteración sobre ítems ejecutando una consulta SQL individual por producto (`SELECT precio FROM productos WHERE id = ?`).
4. **Cálculo de descuento por tipo de cliente (líneas 89–106):** Bloque `if/else` fuertemente anidado (hasta 3 niveles de profundidad para clientes `VIP` y consulta SQL de pedidos previos para `FRECUENTE`), impidiendo modificar las reglas sin releer e intervenir el método completo.
5. **Persistencia directa vía JDBC sin transacción explícita (líneas 112–126):** Múltiples sentencias DML embebidas (`INSERT INTO pedidos`, consulta de identidad generada `CALL IDENTITY()`, `INSERT INTO detalle_pedido` y `UPDATE inventario`).
6. **Notificación y construcción de mensaje (líneas 129–142):** Concatenación manual de cadenas con `StringBuilder` para el cuerpo del correo y llamado a `EmailService`.
7. **Métodos privados no cohesivos (líneas 149–188):** Funciones accesorias (`obtenerHistorialCliente`, `formatearFactura`, `calcularImpuestoRegional`, `reintentarNotificacion`, `purgarPedidosVencidos`, `construirCuerpoCorreo`) que inflaban artificialmente la clase convirtiéndola en un monolito.

#### 2. Patrones aplicados
* **Chain of Responsibility para las validaciones:**
  Se implementó la clase abstracta `ValidadorPedido` y los eslabones concretos `ValidadorStock` y `ValidadorCliente`.  
  *Justificación:* Las validaciones poseen una **dependencia real de orden y corte anticipado (*short-circuit*)**: si no hay stock suficiente, no tiene justificación consultar la base de datos para verificar la mora del cliente.
  *Alternativa descartada:* Una lista de predicados booleanos (`List<Predicate<ContextoPedido>>`) invocados secuencialmente. Se descartó porque evalúa todos los predicados aunque el primero ya haya fallado, careciendo de un mecanismo de corte anticipado limpio.

* **Strategy para el cálculo de descuento por tipo de cliente:**
  Se definió la interfaz `EstrategiaDescuento` con las implementaciones `DescuentoVip`, `DescuentoFrecuente` y `DescuentoEstandar`, coordinadas por `SelectorEstrategiaDescuento`.  
  *Justificación:* El cálculo de descuento no tiene dependencia de orden ni necesita interrumpir el flujo del pedido. Se aplica exactamente una regla seleccionada mediante un mapa directo en tiempo constante $O(1)$, eliminando condicionales anidados.
  *Alternativa descartada:* Tratar el descuento como un eslabón adicional en la cadena de validación. Se descartó porque desvirtúa el contrato de validación (decidir si el pedido continúa o se rechaza) y genera efectos colaterales.

* **Separación de Persistencia y Notificación:**
  Se extrajeron las clases `@Repository PedidoRepository` y `@Service NotificacionPedidoService`, reduciendo a `GestorPedidos` a un orquestador delgado (~35 líneas) altamente legible y desacoplado.

---

### Parte 2 — Crecimiento del proyecto

#### 1. Antipatrón identificado
**Golden Hammer (Martillo Dorado).**  
Al solicitarse tres nuevas campañas de descuento (`BLACK_FRIDAY`, `CORPORATIVO` y `VOLUMEN`), se implementaron erróneamente como eslabones adicionales de la cadena de validación (`PromocionBlackFriday`, `PromocionCorporativo`, `PromocionVolumen`), reutilizando Chain of Responsibility únicamente porque "ya existía y había funcionado en la Parte 1", sin evaluar la naturaleza del nuevo problema.

#### 2. Evidencia concreta del código
* **Inexistencia de dependencia de orden o corte:** Las tres promociones no guardan ninguna relación de precedencia entre sí ni con las validaciones de stock y cliente; el orden en que se ejecutan no altera el cálculo.
* **Violación del contrato de `ValidadorPedido`:** La abstracción fue diseñada para decidir si un pedido se acepta o se rechaza; las clases de promoción nunca rechazan nada, solo se engancharon para alterar el estado del contexto.
* **Corrupción del contexto mutable:** Se añadió el campo mutable `descuentoCampana` a `ContextoPedido`, forzando a las promociones a competir por mutar la variable (`if (valor > this.descuentoCampana)`), introduciendo acoplamiento temporal e impidiendo componer descuentos de forma alternativa.

#### 3. Patrón aplicado y prevención de Lava Flow
* **Strategy extendido:** Las campañas se modelaron como implementaciones limpias de `EstrategiaDescuento` (`DescuentoBlackFriday`, `DescuentoCorporativo` y `DescuentoVolumen`), evaluadas y consolidadas a través de `CalculadorDescuentoFinal`.
* **Eliminación absoluta de código descartado:** Las clases `PromocionBlackFriday`, `PromocionCorporativo`, `PromocionVolumen` y el campo `descuentoCampana` fueron eliminados definitivamente mediante `git rm` y no dejados como código comentado. Dejar código obsoleto comentado "por si acaso" es el origen directo del antipatrón **Lava Flow**, donde el código muerto permanece indefinidamente por temor a borrarlo. El historial de Git es el mecanismo formal para auditar la evolución del diseño.

---

## Comparación Antes vs. Después

| Aspecto | Versión Inicial (Antipatrones) | Versión Refactorizada (Patrones de Diseño) |
| :--- | :--- | :--- |
| **Líneas en `GestorPedidos`** | ~190 líneas con métodos auxiliares y 6 responsabilidades | ~40 líneas como orquestador delgado |
| **Validaciones** | Consultas SQL y condicionales anidados dentro del método principal | **Chain of Responsibility** (`ValidadorStock` $\rightarrow$ `ValidadorCliente`) con corte anticipado |
| **Cálculo de Descuento** | `if/else` anidados hasta 3 niveles según tipo de cliente | **Strategy** (`EstrategiaDescuento`) y `CalculadorDescuentoFinal` sin condicionales |
| **Persistencia** | SQL embebido directo (`INSERT`, `UPDATE`) sin encapsular | `@Repository PedidoRepository` desacoplado |
| **Notificación** | `StringBuilder` manual acoplado al flujo de negocio | `@Service NotificacionPedidoService` independiente |
| **Extensibilidad (OCP)** | Agregar cliente o promoción obligaba a editar el método central | Nuevas estrategias se agregan creando clases sin modificar el orquestador |

---

## Cómo ejecutar

### Prerrequisitos
- Java JDK 17 o superior
- Apache Maven 3.8+

```bash
# Compilar el proyecto
mvn compile

# Ejecutar las pruebas unitarias y de integración (8 pruebas pasando al 100%)
mvn test

# Iniciar la aplicación Spring Boot
mvn spring-boot:run
```

## Herramientas utilizadas
- **Lenguaje:** Java 17
- **Framework:** Spring Boot 3.2.5 (Spring Web, Spring Data JDBC)
- **Base de Datos:** H2 Database (embebida en memoria)
- **Gestor de Construcción:** Apache Maven
- **Control de Versiones:** Git, GitHub
- **Pruebas:** JUnit 5, Spring Boot Test

---

## Conclusiones
La refactorización realizada demostró que el principio de responsabilidad única (SRP) no es meramente estético, sino un habilitador esencial para la mantenibilidad y estabilidad del software. Separar las responsabilidades de validación, cálculo, persistencia y notificación permitió transformar un *God Object* monolítico y frágil en una arquitectura modular, extensible y fácilmente testeable. Asimismo, el diagnóstico del antipatrón *Golden Hammer* evidenció el peligro de forzar patrones familiares donde no corresponden, confirmando que cada patrón de diseño debe responder a la forma estructural del problema y que el código descartado debe removerse completamente para evitar caer en el antipatrón *Lava Flow*.