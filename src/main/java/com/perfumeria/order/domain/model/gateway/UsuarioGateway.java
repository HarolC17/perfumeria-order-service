package com.perfumeria.order.domain.model.gateway;

import com.perfumeria.order.domain.model.UserInfo;

public interface UsuarioGateway {

    UserInfo obtenerUsuario(Long usuarioId);

}
