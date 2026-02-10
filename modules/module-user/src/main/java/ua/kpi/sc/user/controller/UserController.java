package ua.kpi.sc.user.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user profile and account management.
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {
}
