package me.mmtr.ticketservice.repository;

import me.mmtr.ticketservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByUserIdAndEventId(Long userId, Long eventId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    List<Order> findAllByUserId(Long userId);

    List<Order> findAllByEventId(Long eventId);

    void deleteAllByEventId(Long eventId);
}
