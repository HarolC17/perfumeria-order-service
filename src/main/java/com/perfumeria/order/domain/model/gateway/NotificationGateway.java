package com.perfumeria.order.domain.model.gateway;

import com.perfumeria.order.domain.model.Notificacion;

public interface NotificationGateway {

    void enviarMensaje(Notificacion mensajeJson);
}