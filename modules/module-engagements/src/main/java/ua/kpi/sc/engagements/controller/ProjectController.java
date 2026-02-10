package ua.kpi.sc.engagements.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for student council project CRUD operations and team management.
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "Project management endpoints")
public class ProjectController {
}
