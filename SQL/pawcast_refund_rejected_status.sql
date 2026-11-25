-- RefundService.rejectRefund persists REJECTED; include it in existing PostgreSQL schemas.
-- This migration is safe to rerun: it replaces the refund status check constraint by name/content.
DO $$
DECLARE
    old_status_constraint text;
BEGIN
    SELECT constraint_row.conname
      INTO old_status_constraint
      FROM pg_constraint AS constraint_row
     WHERE constraint_row.conrelid = 'refunds'::regclass
       AND constraint_row.contype = 'c'
       AND pg_get_constraintdef(constraint_row.oid) ILIKE '%status%'
     ORDER BY constraint_row.conname
     LIMIT 1;

    IF old_status_constraint IS NOT NULL THEN
        EXECUTE format(
            'ALTER TABLE refunds DROP CONSTRAINT %I',
            old_status_constraint
        );
    END IF;

    ALTER TABLE refunds
        ADD CONSTRAINT refunds_status_check
        CHECK (status IN ('PENDING', 'COMPLETED', 'REJECTED', 'FAILED'));
END
$$;
