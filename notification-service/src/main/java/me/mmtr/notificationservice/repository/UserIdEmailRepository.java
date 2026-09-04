package me.mmtr.notificationservice.repository;

import me.mmtr.notificationservice.model.UserIdEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserIdEmailRepository extends JpaRepository<UserIdEmail, Long> {
    List<UserIdEmail> findAllByUserIdIn(List<Long> userIds);

    Optional<UserIdEmail> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
