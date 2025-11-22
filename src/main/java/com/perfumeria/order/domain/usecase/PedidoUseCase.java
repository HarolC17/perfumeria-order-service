package com.perfumeria.order.domain.usecase;


import com.perfumeria.order.domain.exception.*;
import com.perfumeria.order.domain.model.*;
import com.perfumeria.order.domain.model.gateway.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@RequiredArgsConstructor
public class PedidoUseCase {

    private final PedidoGateway pedidoGateway;
    private final PagoUseCase pagoUseCase;
    private final CatalogoGateway catalogGateway;
    private final UsuarioGateway usuarioGateway;
    private final NotificationGateway notificationGateway; // ✅ Agregar

    /**
     * Crea un nuevo pedido desde el carrito de un usuario.
     * Valida existencia de usuario, datos obligatorios y contenido del carrito.
     */
    public Pedido crearPedido(Long usuarioId, String direccionEnvio) {

        // 1️⃣ Validar usuario (obtiene datos completos)
        if (usuarioId == null || usuarioId <= 0) {
            throw new CampoObligatorioException("El ID del usuario es obligatorio.");
        }

        UserInfo usuario = usuarioGateway.obtenerUsuario(usuarioId); // ✅ Cambiado

        // 2️⃣ Validar dirección
        if (direccionEnvio == null || direccionEnvio.isBlank()) {
            throw new CampoObligatorioException("La dirección de envío es obligatoria.");
        }

        // 3️⃣ Obtener productos del carrito
        List<ItemPedido> items;
        try {
            items = catalogGateway.obtenerItemsDeCarrito(usuarioId);
        } catch (Exception e) {
            throw new ErrorDeComunicacionException("Error al obtener el carrito del usuario.");
        }

        if (items == null || items.isEmpty()) {
            throw new CarritoVacioException("El carrito está vacío o no existe para el usuario " + usuarioId);
        }

        // 4️⃣ Calcular total
        double total = items.stream()
                .mapToDouble(ItemPedido::getSubtotal)
                .sum();

        if (total <= 0) {
            throw new TotalInvalidoException("El total del pedido no puede ser 0 o negativo.");
        }

        // ✅ 5️⃣ VALIDAR Y DESCONTAR STOCK ANTES DE CREAR EL PEDIDO
        try {
            catalogGateway.venderCarrito(usuarioId);
        } catch (StockInsuficienteException e) {
            throw e; // Propagar excepción de stock
        } catch (Exception e) {
            throw new ErrorDeComunicacionException("Error al procesar el carrito: " + e.getMessage());
        }

        // 6️⃣ Crear el pedido (ahora sabemos que había stock)
        Pedido pedido = new Pedido();
        pedido.setUsuarioId(usuarioId);
        pedido.setDireccionEnvio(direccionEnvio);
        pedido.setFechaPedido(LocalDateTime.now());
        pedido.setEstado("PENDIENTE");
        pedido.setTipoPago("CONTRA_ENTREGA");
        pedido.setTotal(total);
        pedido.setItems(items);

        // 7️⃣ Guardar pedido
        Pedido pedidoGuardado;
        try {
            pedidoGuardado = pedidoGateway.guardarPedido(pedido);
        } catch (Exception e) {
            throw new PedidoPersistenciaException("Error al guardar el pedido: " + e.getMessage());
        }

        // 8️⃣ Crear pago simulado
        try {
            Pago pago = new Pago();
            pago.setPedidoId(pedidoGuardado.getId());
            pago.setReferenciaTransaccion(null);
            pagoUseCase.registrarPago(pago);
        } catch (Exception e) {
            throw new PagoPersistenciaException("Error al registrar el pago del pedido: " + e.getMessage());
        }

//        // ✅ 9️⃣ Enviar notificación AL FINAL (cuando todo salió bien)
        try {
            Notificacion mensaje = Notificacion.builder()
                    .tipo("Pedido Confirmado")
                    .email(usuario.getEmail())
                    .numeroTelefono(usuario.getNumeroTelefono())
                    .mensaje("¡Hola " + usuario.getNombre() + "! Tu pedido #" + pedidoGuardado.getId() +
                            " ha sido confirmado. Estado: " + pedidoGuardado.getEstado() +
                            ". Tipo de pago: " + pedidoGuardado.getTipoPago() +
                            ". Se enviará a: " + pedidoGuardado.getDireccionEnvio() +
                            ". Total: $" + total)
                    .build();

            notificationGateway.enviarMensaje(mensaje);
        } catch (Exception e) {
            // No lanzar excepción si falla la notificación
            System.err.println("Error al enviar notificación: " + e.getMessage());
        }

        return pedidoGuardado;
    }

    /**
     * Buscar un pedido por su ID.
     */
    public Pedido buscarPedidoPorId(Long id) {
        if (id == null || id <= 0) {
            throw new CampoObligatorioException("El ID del pedido es obligatorio.");
        }

        Pedido pedido = pedidoGateway.buscarPedidoPorId(id);
        if (pedido == null) {
            throw new PedidoNoEncontradoException("No se encontró el pedido con ID: " + id);
        }

        return pedido;
    }

    /**
     * Obtener todos los pedidos de un usuario.
     */
    public List<Pedido> obtenerPedidosPorUsuario(Long usuarioId) {
        if (usuarioId == null || usuarioId <= 0) {
            throw new CampoObligatorioException("El ID del usuario es obligatorio.");
        }

        UserInfo usuario = usuarioGateway.obtenerUsuario(usuarioId);

        // Si el usuario sí existe, simplemente retorna la lista (puede ser vacía, está bien)
        List<Pedido> pedidos = pedidoGateway.obtenerPedidosPorUsuario(usuarioId);
        return pedidos; // <-- ¡No lances excepción si la lista está vacía!
    }
}
