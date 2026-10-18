package com.tienda.pedidos;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.GestorPedidos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GestorPedidosTest {

    @Autowired
    private GestorPedidos gestorPedidos;

    @Test
    @DisplayName("Ruta 1: Debe rechazar el pedido si no hay stock suficiente")
    void testRechazoPorStockInsuficiente() {
        // Producto 4 tiene stock 0 en inventario
        PedidoRequest request = new PedidoRequest(1L, "cliente@test.com",
                List.of(new ItemPedido(4L, 2)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertNotNull(resultado);
        assertFalse(resultado.isConfirmado());
        assertTrue(resultado.getMotivoRechazo().contains("Stock insuficiente"));
    }

    @Test
    @DisplayName("Ruta 2: Debe rechazar el pedido si el cliente no esta registrado")
    void testRechazoPorClienteInexistente() {
        PedidoRequest request = new PedidoRequest(9999L, "desconocido@test.com",
                List.of(new ItemPedido(1L, 1)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertNotNull(resultado);
        assertFalse(resultado.isConfirmado());
        assertEquals("Cliente no registrado", resultado.getMotivoRechazo());
    }

    @Test
    @DisplayName("Ruta 3: Debe validar cliente moroso con deuda pendiente")
    void testClienteMoroso() {
        // Cliente 4 es MOROSO con factura impaga de 350.000
        PedidoRequest request = new PedidoRequest(4L, "moroso@test.com",
                List.of(new ItemPedido(1L, 1)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertNotNull(resultado);
        // Si se ejecuta antes de las 20:00 se rechaza; si es después se permite excepcionalmente
        if (java.time.LocalTime.now().isBefore(java.time.LocalTime.of(20, 0))) {
            assertFalse(resultado.isConfirmado());
            assertTrue(resultado.getMotivoRechazo().contains("Cliente con deuda pendiente"));
        } else {
            assertTrue(resultado.isConfirmado());
        }
    }

    @Test
    @DisplayName("Ruta 4: Debe confirmar pedido para cliente VIP aplicando descuento del 15% (subtotal > 1.000.000)")
    void testDescuentoClienteVip() {
        // Cliente 1 (VIP) compra 1 Laptop Profesional (1.200.000)
        PedidoRequest request = new PedidoRequest(1L, "vip@empresa.com",
                List.of(new ItemPedido(3L, 1)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertNotNull(resultado);
        assertTrue(resultado.isConfirmado());
        assertNotNull(resultado.getPedidoId());

        // Subtotal = 1.200.000
        // Descuento VIP (> 1M) = 15% = 180.000 -> Base = 1.020.000
        // Impuesto (19%) = 193.800
        // Total esperado = 1.213.800
        assertEquals(1213800.0, resultado.getTotal(), 0.01);
    }

    @Test
    @DisplayName("Ruta 5: Debe confirmar pedido para cliente FRECUENTE con 4 pedidos previos aplicando 4% de descuento")
    void testDescuentoClienteFrecuente() {
        // Cliente 2 (FRECUENTE, 4 pedidos en BD) compra 1 Teclado (150.000)
        PedidoRequest request = new PedidoRequest(2L, "frecuente@test.com",
                List.of(new ItemPedido(1L, 1)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertNotNull(resultado);
        assertTrue(resultado.isConfirmado());
        assertNotNull(resultado.getPedidoId());

        // Subtotal = 150.000
        // Descuento FRECUENTE (> 3 pedidos) = 4% = 6.000 -> Base = 144.000
        // Impuesto (19%) = 27.360
        // Total esperado = 171.360
        assertEquals(171360.0, resultado.getTotal(), 0.01);
    }
}