package me.mmtr.userservice.service;

import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.User;
import me.mmtr.userservice.model.dto.messaging.UserDeletedEvent;
import me.mmtr.userservice.model.dto.response.UserResponse;
import me.mmtr.userservice.model.exception.InsufficientPrivilegesException;
import me.mmtr.userservice.model.exception.UserNotFoundException;
import me.mmtr.userservice.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;

    public UserService(
            UserRepository userRepository,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.userRepository = userRepository;
        this.publisher = applicationEventPublisher;
    }

    public UserResponse getById(Long id) {
        return UserResponse.fromUser(getUserById(id));
    }

    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserResponse::fromUser);
    }

    @Transactional
    public void deleteUser(Long id, User requester) {
        User target = getUserById(id);

        if (!canDelete(requester, target)) {
            throw new InsufficientPrivilegesException();
        }
        userRepository.delete(target);
        publisher.publishEvent(new UserDeletedEvent(
                target.getId(),
                target.getEmail()
        ));
    }

    private User getUserById(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private boolean canDelete(User requester, User target) {
        if (
                target.getRole().equals(Role.SUPER_ADMIN) ||
                requester.getId().equals(target.getId())
        ) {
            return false;
        }

        return switch (requester.getRole()) {
            case SUPER_ADMIN -> true;
            case ADMIN -> target.getRole() == Role.USER;
            case USER -> false;
        };
    }
}
