package com.perfumeria.order.infrastructure.driver_adapters.jpa_repository.pago;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PagoDataJpaRepository extends JpaRepository<PagoData, Long> {

    // Buscar pago por el ID del pedido
    Optional<PagoData> findByPedido_IdPedido(Long idPedido);
}
