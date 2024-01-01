package com.simwong.simonsgpt.api;

import com.simwong.simonsgpt.domain.User;
import com.simwong.simonsgpt.domain.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Validated
@Tag(name = "user", description = "User management operations")
@RequestMapping("/users")
public interface UserApi {

    /**
     * POST /users/register : Register a new user
     *
     * @param user (required) User data for registration
     * @return Successful user registration (status code 200)
     * or Bad request (status code 400)
     */
    @Operation(
            operationId = "registerUser",
            summary = "Register a new user",
            tags = {"user"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful user registration", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))
                    }),
                    @ApiResponse(responseCode = "400", description = "Bad request")
            }
    )
    @RequestMapping(
            method = RequestMethod.POST,
            value = "/register",
            produces = {"application/json"},
            consumes = {"application/json"}
    )
    default Mono<ResponseEntity<UserDTO>> _registerUser(
            @Parameter(name = "user", description = "User data for registration", required = true) @Valid @RequestBody User user,
            @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        return registerUser(user, exchange);
    }

    // Override this method in the controller implementation
    default Mono<ResponseEntity<UserDTO>> registerUser(User user, final ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    }

    /**
     * POST /users/login : Login a user
     *
     * @return Successful login with a JWT (status code 200)
     * or Unauthorized if not logged in (status code 401)
     */
    @Operation(
            operationId = "loginUser",
            summary = "Login a user",
            tags = {"user"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful login with a JWT"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized if not logged in")
            }
    )
    @RequestMapping(
            method = RequestMethod.POST,
            value = "/login",
            produces = {"text/plain"}
    )
    default Mono<ResponseEntity<String>> _loginUser(
            @Parameter(name = "user", description = "User data for login", required = true) @Valid @RequestBody User user,
            @Parameter(hidden = true) final ServerWebExchange exchange
    ) {
        return loginUser(user, exchange);
    }

    // Override this method in the controller implementation
    default Mono<ResponseEntity<String>> loginUser(User user, final ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        return Mono.error(new UnsupportedOperationException("Not implemented"));
    }

}
