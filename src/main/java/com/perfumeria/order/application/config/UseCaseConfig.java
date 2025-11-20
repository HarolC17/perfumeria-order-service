package com.perfumeria.order.application.config;

import com.perfumeria.order.domain.model.gateway.*;
import com.perfumeria.order.domain.usecase.PagoUseCase;
import com.perfumeria.order.domain.usecase.PedidoUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public PedidoUseCase pedidoUseCase(PedidoGateway pedidoGateway,
                                       PagoUseCase pagoUseCase,
                                       CatalogoGateway catalogoGateway,
                                       UsuarioGateway usuarioGateway,
                                       NotificationGateway notificationGateway) {
        return new PedidoUseCase(pedidoGateway, pagoUseCase, catalogoGateway, usuarioGateway,  notificationGateway);
    }

    @Bean
    public PagoUseCase pagoUseCase(PagoGateway pagoGateway,
                                   PedidoGateway pedidoGateway,
                                   CatalogoGateway catalogoGateway,
                                   UsuarioGateway usuarioGateway,
                                   NotificationGateway notificationGateway) {
        return new PagoUseCase(pagoGateway, pedidoGateway, catalogoGateway,  usuarioGateway,   notificationGateway);
    }
}