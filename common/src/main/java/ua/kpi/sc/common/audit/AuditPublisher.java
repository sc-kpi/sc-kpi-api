package ua.kpi.sc.common.audit;

/**
 * Port interface for publishing audit events.
 * Modules inject this to emit audit records without depending on the audit module directly.
 *
 * @since 0.4.0
 */
public interface AuditPublisher {

    /**
     * Publishes an audit event for persistence.
     * Implementations must persist in an independent transaction.
     *
     * @param event the audit event to publish
     */
    void publish(AuditEvent event);
}
