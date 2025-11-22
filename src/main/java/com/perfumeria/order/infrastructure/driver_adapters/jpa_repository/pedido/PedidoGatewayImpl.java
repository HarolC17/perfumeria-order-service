package com.perfumeria.order.infrastructure.driver_adapters.jpa_repository.pedido;

import com.perfumeria.order.domain.exception.PedidoNoEncontradoException;
import com.perfumeria.order.domain.exception.PedidoPersistenciaException;
import com.perfumeria.order.domain.model.Pedido;
import com.perfumeria.order.domain.model.gateway.PedidoGateway;
import com.perfumeria.order.infrastructure.mapper.MapperPedido;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PedidoGatewayImpl implements PedidoGateway {

    private final PedidoDataJpaRepository pedidoRepository;
    private final MapperPedido mapperPedido;

    @Override
    public Pedido guardarPedido(Pedido pedido) {
        try {
            return mapperPedido.toDomain(
                    pedidoRepository.save(mapperPedido.toData(pedido))
            );
        } catch (Exception e) {
            throw new PedidoPersistenciaException("Error al guardar el pedido en la base de datos: " + e.getMessage());
        }
    }

    @Override
    public Pedido buscarPedidoPorId(Long id) {
        return pedidoRepository.findById(id)
                .map(mapperPedido::toDomain)
                .orElseThrow(() -> new PedidoNoEncontradoException("No se encontró el pedido con ID: " + id));
    }

    @Override
    public List<Pedido> obtenerPedidosPorUsuario(Long usuarioId) {
        try {
            List<PedidoData> pedidos = pedidoRepository.findByUsuarioId(usuarioId);

            // Quita el throw aquí: permite devolver lista vacía
            if (pedidos == null) {
                return List.of(); // Devuelve lista vacía si es null
            }

            return pedidos.stream()
                    .map(mapperPedido::toDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new PedidoPersistenciaException("Error al consultar los pedidos del usuario: " + e.getMessage());
        }
    }

    @Override
    public void eliminarPedido(Long id) {
        if (!pedidoRepository.existsById(id)) {
            throw new PedidoNoEncontradoException("No se puede eliminar: pedido con ID " + id + " no existe.");
        }

        try {
            pedidoRepository.deleteById(id);
        } catch (Exception e) {
            throw new PedidoPersistenciaException("Error al eliminar el pedido: " + e.getMessage());
        }
    }

    @Override
    public Pedido actualizarEstadoPedido(Long pedidoId, String nuevoEstado) {
        PedidoData pedidoData = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new PedidoNoEncontradoException("No se encontró el pedido con ID: " + pedidoId));

        try {
            pedidoData.setEstado(nuevoEstado);
            PedidoData actualizado = pedidoRepository.save(pedidoData);
            return mapperPedido.toDomain(actualizado);
        } catch (Exception e) {
            throw new PedidoPersistenciaException("Error al actualizar el estado del pedido: " + e.getMessage());
        }
    }
}
