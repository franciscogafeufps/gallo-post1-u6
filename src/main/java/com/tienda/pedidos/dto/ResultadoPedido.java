package com.tienda.pedidos.dto;

public class ResultadoPedido {
    private boolean confirmado;
    private Long pedidoId;
    private double total;
    private String motivoRechazo;

    public ResultadoPedido() {}

    public static ResultadoPedido confirmado(Long pedidoId, double total) {
        ResultadoPedido r = new ResultadoPedido();
        r.confirmado = true;
        r.pedidoId = pedidoId;
        r.total = total;
        return r;
    }

    public static ResultadoPedido rechazado(String motivo) {
        ResultadoPedido r = new ResultadoPedido();
        r.confirmado = false;
        r.motivoRechazo = motivo;
        return r;
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public void setConfirmado(boolean confirmado) {
        this.confirmado = confirmado;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(Long pedidoId) {
        this.pedidoId = pedidoId;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }
}