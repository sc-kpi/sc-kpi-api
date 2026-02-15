package ua.kpi.sc.document.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.kpi.sc.common.featureflag.FeatureFlag;

/**
 * REST controller for document upload, retrieval, and lifecycle management.
 *
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documents", description = "Document management endpoints")
@FeatureFlag("documents.management")
public class DocumentController {
}
