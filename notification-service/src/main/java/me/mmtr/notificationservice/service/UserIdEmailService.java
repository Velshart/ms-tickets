package me.mmtr.notificationservice.service;

import jakarta.transaction.Transactional;
import me.mmtr.notificationservice.exception.UserEmailNotFoundException;
import me.mmtr.notificationservice.model.UserIdEmail;
import me.mmtr.notificationservice.model.dto.UserIdEmailDto;
import me.mmtr.notificationservice.repository.UserIdEmailRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserIdEmailService {
    private final UserIdEmailRepository userIdEmailRepository;

    public UserIdEmailService(UserIdEmailRepository userIdEmailRepository) {
        this.userIdEmailRepository = userIdEmailRepository;
    }

    public void create(Long userId, String email) {
        if (!userIdEmailRepository.existsByUserId(userId)) {
            userIdEmailRepository.save(new UserIdEmail(userId, email));
        }
    }

    public UserIdEmailDto findByUserId(Long userId) {
        return UserIdEmailDto.from(
                userIdEmailRepository.findByUserId(userId).orElseThrow(
                        () -> new UserEmailNotFoundException(userId)
                ));
    }

    public List<UserIdEmailDto> findAllByUserIdIn(List<Long> userIds) {
        return userIdEmailRepository
                .findAllByUserIdIn(userIds)
                .stream()
                .map(UserIdEmailDto::from)
                .toList();
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        userIdEmailRepository.deleteByUserId(userId);
    }
}
