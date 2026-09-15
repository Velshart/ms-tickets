package me.mmtr.userservice.assembler;


import me.mmtr.userservice.model.dto.response.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserModelAssemblerTests {
    private UserModelAssembler userModelAssembler;

    @BeforeEach
    public void setUp() {
        userModelAssembler = new UserModelAssembler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("localhost");
        request.setServerPort(8080);
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(request)
        );
    }

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
