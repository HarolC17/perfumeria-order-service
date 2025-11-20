package com.perfumeria.order.infrastructure.driver_adapters.rest_client.usuario;

import com.perfumeria.order.domain.exception.UsuarioNoEncontradoException;
import com.perfumeria.order.domain.model.UserInfo;
import com.perfumeria.order.domain.model.gateway.UsuarioGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
@Component
public class UsuarioGatewayImpl implements UsuarioGateway {

    private final RestTemplate restTemplate;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Override
    public UserInfo obtenerUsuario(Long usuarioId) {
        try {
            ResponseEntity<UserInfo> response = restTemplate.getForEntity(
                    authServiceUrl + "/api/perfumeria/usuario/" + usuarioId,
                    UserInfo.class
            );

            return response.getBody();

        } catch (HttpClientErrorException.NotFound e) {
            throw new UsuarioNoEncontradoException("Usuario no encontrado con ID: " + usuarioId);
        } catch (Exception error) {
            throw new RuntimeException("Error al consultar el servicio de autenticación", error);
        }
    }
}