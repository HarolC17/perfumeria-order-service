package com.perfumeria.order.domain.usecase;

import com.perfumeria.order.domain.exception.*;
import com.perfumeria.order.domain.model.Notificacion;
import com.perfumeria.order.domain.model.Pago;
import com.perfumeria.order.domain.model.Pedido;
import com.perfumeria.order.domain.model.UserInfo;
import com.perfumeria.order.domain.model.gateway.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class PagoUseCase {

    private final PagoGateway pagoGateway;
    private final PedidoGateway pedidoGateway;
    private final CatalogoGateway catalogoGateway;
    private final UsuarioGateway usuarioGateway;
    private final NotificationGateway notificationGateway; // 🔔 Descomentar cuando se implemente SNS

    /**
     * Registrar un nuevo pago
     */
    public Pago registrarPago(Pago pago) {
        if (pago == null) {
            throw new CampoObligatorioException("El pago no puede ser nulo.");
        }
        if (pago.getPedidoId() == null || pago.getPedidoId() <= 0) {
            throw new CampoObligatorioException("El ID del pedido es obligatorio para registrar un pago.");
        }

        pago.setFechaPago(LocalDateTime.now());
        pago.setEstadoPago("PENDIENTE");

        try {
            return pagoGateway.guardarPago(pago);
        } catch (Exception e) {
            throw new PagoPersistenciaException("Error al guardar el pago: " + e.getMessage());
        }
    }

    /**
     * Actualiza el estado del pago (CONFIRMADO o RECHAZADO)
     */
    public Pago actualizarEstadoPago(Long idPago, String nuevoEstado) {
        if (idPago == null || idPago <= 0) {
            throw new CampoObligatorioException("El ID del pago es obligatorio.");
        }
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            throw new CampoObligatorioException("El nuevo estado no puede estar vacío.");
        }

        Pago pago = pagoGateway.buscarPagoPorId(idPago);
        if (pago == null) {
            throw new PagoNoEncontradoException("No se encontró el pago con ID: " + idPago);
        }

        pago.setEstadoPago(nuevoEstado.toUpperCase());
        pago.setFechaPago(LocalDateTime.now());

        try {
            pago = pagoGateway.guardarPago(pago);

            if ("CONFIRMADO".equalsIgnoreCase(nuevoEstado)) {
                pedidoGateway.actualizarEstadoPedido(pago.getPedidoId(), "ENVIADO");
            }

            if ("RECHAZADO".equalsIgnoreCase(nuevoEstado)) {
                Pedido pedido = pedidoGateway.buscarPedidoPorId(pago.getPedidoId());
                if (pedido != null && pedido.getItems() != null) {
                    pedido.getItems().forEach(item -> {
                        try {
                            catalogoGateway.reponerStock(item.getProductoId(), item.getCantidad());
                        } catch (Exception ex) {
                            throw new RuntimeException("Error al reponer stock del producto: " + item.getProductoId(), ex);
                        }
                    });
                }
                pedidoGateway.actualizarEstadoPedido(pago.getPedidoId(), "CANCELADO");
            }

            return pago;
        } catch (Exception e) {
            throw new PagoPersistenciaException("Error al actualizar el pago: " + e.getMessage());
        }
    }

    /**
     * Registrar referencia de transacción (guía de envío)
     */
    public Pago registrarReferenciaTransaccion(Long idPago, String referencia) {
        if (idPago == null || idPago <= 0) {
            throw new CampoObligatorioException("El ID del pago es obligatorio.");
        }
        if (referencia == null || referencia.isBlank()) {
            throw new CampoObligatorioException("La referencia de transacción es obligatoria.");
        }

        Pago pago = pagoGateway.buscarPagoPorId(idPago);
        if (pago == null) {
            throw new PagoNoEncontradoException("No se encontró el pago con ID: " + idPago);
        }

        pago.setReferenciaTransaccion(referencia.trim());
        pago.setEstadoPago("CONFIRMADO");
        pago.setFechaPago(LocalDateTime.now());

        try {
            // Obtener el pedido para sacar el usuarioId
            Pedido pedido = pedidoGateway.buscarPedidoPorId(pago.getPedidoId());

            // Obtener información del usuario
            UserInfo usuario = usuarioGateway.obtenerUsuario(pedido.getUsuarioId());

            // Actualizar estado del pedido
            pedidoGateway.actualizarEstadoPedido(pago.getPedidoId(), "ENVIADO");

            // Guardar el pago
            Pago pagoGuardado = pagoGateway.guardarPago(pago);

            // 🔔 NOTIFICACIÓN SMS
             try {
                 Notificacion mensaje = Notificacion.builder()
                         .tipo("Pago confirmado")
                         .email(usuario.getEmail())
                         .numeroTelefono(usuario.getNumeroTelefono())
                         .mensaje("¡Hola " + usuario.getNombre() + "! Tu pago ha sido confirmado. " +
                                 "Referencia: " + referencia + ". Tu pedido #" + pago.getPedidoId() +
                                 " será enviado pronto.")
                         .build();

                 notificationGateway.enviarMensaje(mensaje);
             } catch (Exception e) {
                 System.err.println("Error al enviar notificación: " + e.getMessage());
             }

            return pagoGuardado;

        } catch (Exception e) {
            throw new PagoPersistenciaException("Error al registrar la referencia del pago: " + e.getMessage());
        }
    }

    /**
     * Buscar pago por su ID
     */
    public Pago buscarPagoPorId(Long idPago) {
        if (idPago == null || idPago <= 0) {
            throw new CampoObligatorioException("El ID del pago es obligatorio.");
        }

        Pago pago = pagoGateway.buscarPagoPorId(idPago);
        if (pago == null) {
            throw new PagoNoEncontradoException("Pago no encontrado con ID: " + idPago);
        }

        return pago;
    }
}