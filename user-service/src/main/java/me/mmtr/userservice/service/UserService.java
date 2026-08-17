package me.mmtr.userservice.service;

import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.User;
import me.mmtr.userservice.model.dto.response.UserResponse;
import me.mmtr.userservice.model.exception.InsufficientPrivilegesException;
import me.mmtr.userservice.model.exception.UserNotFoundException;
import me.mmtr.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getById(Long id) {
        return UserResponse.fromUser(getUserById(id));
    }

    public List<UserResponse> listUsers() {
        return userRepository
                .findAll()
                .stream()
                .map(UserResponse::fromUser)
                .toList();
    }

    public void deleteUser(Long id, User requester) {
        User target = getUserById(id);

        if (!canDelete(requester, target)) {
            throw new InsufficientPrivilegesException();
        }
        userRepository.delete(target);
    }

    private User getUserById(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private boolean canDelete(User requester, User target) {
        if (target.getRole().equals(Role.SUPER_ADMIN)) {
            return false;
        }

        return switch (requester.getRole()) {
            case SUPER_ADMIN -> true;
            case ADMIN -> target.getRole() == Role.USER;
            case USER -> false;
        };
    }
}
