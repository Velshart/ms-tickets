package me.mmtr.userservice.assembler;

import me.mmtr.userservice.controller.UserController;
import me.mmtr.userservice.model.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class UserModelAssembler implements RepresentationModelAssembler<UserResponse, EntityModel<UserResponse>> {
    @Override
    public EntityModel<UserResponse> toModel(UserResponse user) {
        return EntityModel.of(
                user,
                linkTo(methodOn(UserController.class).getUserById(user.id())).withSelfRel(),
                linkTo(methodOn(UserController.class).getAllUsers(Pageable.unpaged(), null)).withRel("users"),
                linkTo(methodOn(UserController.class).deleteUser(user.id(), null)).withRel("delete")
        );
    }
}
