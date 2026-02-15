INSERT INTO audit_events (actor_id, action, entity_type, entity_id, entity_name,
                          field_name, old_value, new_value, details, source_module, created_at)
SELECT changed_by, action, 'FEATURE_FLAG', flag_id, flag_key,
       field_name, old_value, new_value, reason, 'feature-flag', changed_at
FROM feature_flag_audit_log;

DROP TABLE feature_flag_audit_log;
