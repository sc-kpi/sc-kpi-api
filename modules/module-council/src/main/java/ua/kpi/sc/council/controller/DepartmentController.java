package ua.kpi.sc.council.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for student council department management and member operations.
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/v1/departments")
@Tag(name = "Departments", description = "Department endpoints")
public class DepartmentController {
}
