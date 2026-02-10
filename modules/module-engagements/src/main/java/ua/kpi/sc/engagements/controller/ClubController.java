package ua.kpi.sc.engagements.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for student club CRUD operations and membership management.
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/v1/clubs")
@Tag(name = "Clubs", description = "Club management endpoints")
public class ClubController {
}
