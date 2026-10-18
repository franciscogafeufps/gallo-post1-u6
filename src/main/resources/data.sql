-- Clientes de prueba
INSERT INTO clientes (id, nombre, tipo_cliente, nit) VALUES (1, 'Empresa VIP S.A.S.', 'VIP', '900123456-1');
INSERT INTO clientes (id, nombre, tipo_cliente, nit) VALUES (2, 'Comprador Frecuente', 'FRECUENTE', '900987654-2');
INSERT INTO clientes (id, nombre, tipo_cliente, nit) VALUES (3, 'Cliente Regular', 'ESTANDAR', NULL);
INSERT INTO clientes (id, nombre, tipo_cliente, nit) VALUES (4, 'Cliente Moroso con Deuda', 'MOROSO', NULL);
INSERT INTO clientes (id, nombre, tipo_cliente, nit) VALUES (5, 'Cliente Corporativo', 'ESTANDAR', '800555444-3');

-- Productos e Inventario
INSERT INTO productos (id, nombre, precio) VALUES (1, 'Teclado Mecanico', 150000.0);
INSERT INTO inventario (producto_id, stock) VALUES (1, 100);

INSERT INTO productos (id, nombre, precio) VALUES (2, 'Monitor 27 Pulgadas 4K', 600000.0);
INSERT INTO inventario (producto_id, stock) VALUES (2, 20);

INSERT INTO productos (id, nombre, precio) VALUES (3, 'Laptop Profesional i7', 1200000.0);
INSERT INTO inventario (producto_id, stock) VALUES (3, 5);

INSERT INTO productos (id, nombre, precio) VALUES (4, 'Mouse Ergonomico', 50000.0);
INSERT INTO inventario (producto_id, stock) VALUES (4, 0); -- Sin stock para pruebas

-- Facturas pendientes para moroso
INSERT INTO facturas (id, cliente_id, monto, pagada) VALUES (1, 4, 350000.0, FALSE);

-- Pedidos historicos previos para cliente frecuente (id=2) para activar descuento por historial
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (2, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (2, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (2, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (2, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');