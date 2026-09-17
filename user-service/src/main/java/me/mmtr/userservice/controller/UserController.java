package me.mmtr.userservice.controller;

import jakarta.validation.Valid;
import me.mmtr.userservice.assembler.UserModelAssembler;
import me.mmtr.userservice.model.Role;
import me.mmtr.userservice.model.User;
import me.mmtr.userservice.model.dto.request.RegisterRequest;
import me.mmtr.userservice.model.dto.response.UserResponse;
import me.mmtr.userservice.service.AuthenticationService;
import me.mmtr.userservice.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final UserModelAssembler modelAssembler;

    public UserController(
            AuthenticationService authenticationService,
            UserService userService,
            UserModelAssembler userModelAssembler
    ) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.modelAssembler = userModelAssembler;
    }

    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<UserResponse>>> getAllUsers(
            Pageable pageable,
            PagedResourcesAssembler<UserResponse> pagedResourcesAssembler
    ) {
        Page<UserResponse> userPage = userService.listUsers(pageable);

        PagedModel<EntityModel<UserResponse>> pagedModel =
                pagedResourcesAssembler.toModel(userPage, modelAssembler);

        return ResponseEntity.ok(pagedModel);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(modelAssembler.toModel(userService.getById(id)));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<EntityModel<UserResponse>> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(modelAssembler.toModel(userService.getByUsername(username)));
    }

    @PostMapping("/register-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<EntityModel<UserResponse>> registerAdmin(
            @Valid @RequestBody RegisterRequest registerRequest) {
        UserResponse createdAdmin = authenticationService.register(
                registerRequest, Role.ADMIN
        );
        EntityModel<UserResponse> entityModel = modelAssembler.toModel(createdAdmin);

        return ResponseEntity.created(entityModel.getRequiredLink("self")
                .toUri()).body(entityModel);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        userService.deleteUser(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}