package me.mmtr.userservice.assembler;


import me.mmtr.userservice.model.dto.response.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserModelAssemblerTests {
    private final UserModelAssembler userModelAssembler = new UserModelAssembler();

    @Test
    void shouldCreateEntityModelWithCorrectLinks() {
        UserResponse userResponse = new UserResponse(
                1L,
                "user",
                "user@example.com",
                "USER"
        );

        EntityModel<UserResponse> entityModel =
                userModelAssembler.toModel(userResponse);

        assertNotNull(entityModel);
        assertEquals(entityModel.getContent(), userResponse);

        Optional<Link> selfLink = entityModel.getLink("self");
        assertTrue(selfLink.isPresent());
        assertTrue(selfLink.get().getHref().endsWith("/api/users/1"));

        Optional<Link> usersLink = entityModel.getLink("users");
        assertTrue(usersLink.isPresent());
        assertTrue(usersLink.get().getHref().endsWith("/api/users"));

        Optional<Link> deleteLink = entityModel.getLink("delete");
        assertTrue(deleteLink.isPresent());
        assertTrue(deleteLink.get().getHref().endsWith("/api/users/1"));
    }
}
