ALTER TABLE documents DROP CONSTRAINT documents_declared_doc_type_check;

ALTER TABLE documents ADD CONSTRAINT documents_declared_doc_type_check CHECK (
    declared_doc_type IS NULL OR declared_doc_type IN (
        'MEDICAL', 'LAB_RESULT', 'PRESCRIPTION', 'DIAGNOSIS', 'DISCHARGE_GUIDE', 'UNKNOWN'
    )
);
