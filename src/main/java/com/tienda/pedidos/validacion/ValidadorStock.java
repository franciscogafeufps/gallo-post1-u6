package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.ItemPedido;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ValidadorStock extends ValidadorPedido {
    private final JdbcTemplate jdbcTemplate;

    public ValidadorStock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void ejecutar(ContextoPedido contexto) {
        if (contexto.getRequest().getItems() == null || contexto.getRequest().getItems().isEmpty()) {
            contexto.rechazar("El pedido no contiene items");
            return;
        }
        for (ItemPedido item : contexto.getRequest().getItems()) {
            Integer stock;
            try {
                stock = jdbcTemplate.queryForObject(
                    "SELECT stock FROM inventario WHERE producto_id = ?",
                    Integer.class, item.getProductoId());
            } catch (Exception e) {
                stock = null;
            }
            if (stock == null || stock < item.getCantidad()) {
                contexto.rechazar("Stock insuficiente: producto " + item.getProductoId());
                return;
            }
        }
    }
}