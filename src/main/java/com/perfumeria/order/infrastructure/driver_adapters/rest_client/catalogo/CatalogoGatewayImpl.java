package com.perfumeria.order.infrastructure.driver_adapters.rest_client.catalogo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.perfumeria.order.domain.exception.StockInsuficienteException;
import com.perfumeria.order.domain.model.ItemPedido;
import com.perfumeria.order.domain.model.gateway.CatalogoGateway;
import com.perfumeria.order.infrastructure.entry_points.dto.ReponerStockDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CatalogoGatewayImpl implements CatalogoGateway {

    private final RestTemplate restTemplate;

    @Value("${catalog.service.url}")
    private String catalogServiceUrl;

    @Override
    public List<ItemPedido> obtenerItemsDeCarrito(Long usuarioId) {
        try {
            ResponseEntity<CarritoResponse> response = restTemplate.getForEntity(
                    catalogServiceUrl + "/api/perfumeria/carrito/ver?usuarioId=" + usuarioId,
                    CarritoResponse.class
            );

            CarritoResponse carrito = response.getBody();
            if (carrito == null || carrito.getItems() == null) {
                return List.of();
            }

            // Convertir los items del carrito en items de pedido
            return carrito.getItems().stream()
                    .map(item -> {
                        ItemPedido nuevo = new ItemPedido();
                        nuevo.setProductoId(item.getProductoId());
                        nuevo.setNombreProducto(item.getNombreProducto());
                        nuevo.setPrecioUnitario(item.getPrecioUnitario());
                        nuevo.setCantidad(item.getCantidad());
                        nuevo.setSubtotal(item.getSubtotal());
                        return nuevo;
                    })
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("Error al consultar el carrito del usuario", e);
        }
    }

    @Override
    public void venderCarrito(Long usuarioId) {
        try {
            restTemplate.postForEntity(
                    catalogServiceUrl + "/api/perfumeria/carrito/vender/" + usuarioId,
                    null,
                    Void.class
            );
        } catch (HttpClientErrorException.BadRequest ex) {
            // Error 400: normalmente por stock insuficiente
            try {
                // Extrae el campo "mensaje" del body JSON de error
                String body = ex.getResponseBodyAsString();
                ObjectMapper mapper = new ObjectMapper();
                String mensaje = mapper.readTree(body).get("mensaje").asText();
                throw new StockInsuficienteException(mensaje);
            } catch (Exception parse) {
                throw new StockInsuficienteException("Stock insuficiente para completar el pedido.");
            }
        } catch (HttpClientErrorException ex) {
            // Otros errores HTTP, reprocesa o lanza excepción más general
            throw new RuntimeException("Error HTTP en catálogo: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            // Otros errores generales
            throw new RuntimeException("Error al procesar la venta en catálogo", ex);
        }
    }
    // Reponer stock

    @Override
    public void reponerStock(Long productoId, Integer cantidad) {
        try {
            ReponerStockDTO dto = new ReponerStockDTO(productoId, cantidad);
            restTemplate.put(
                    catalogServiceUrl + "/api/perfumeria/producto/reponer-stock",
                    dto
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al reponer stock del producto " + productoId, e);
        }
    }
}