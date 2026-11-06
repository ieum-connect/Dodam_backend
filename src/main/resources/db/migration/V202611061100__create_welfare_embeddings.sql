CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE welfare_embeddings (
    id UUID PRIMARY KEY,
    program_id UUID NOT NULL REFERENCES welfare_programs(id) ON DELETE CASCADE,
    chunk_type VARCHAR(40) NOT NULL,
    source_field VARCHAR(100) NOT NULL,
    chunk_text TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    embedding vector(1536) NOT NULL,
    embedding_model VARCHAR(100) NOT NULL,
    source_modified_at TIMESTAMPTZ(6),
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT welfare_embeddings_program_chunk_key UNIQUE (program_id, chunk_type, source_field),
    CONSTRAINT welfare_embeddings_hash_key UNIQUE (program_id, chunk_type, source_field, content_hash)
);

CREATE INDEX welfare_embeddings_program_idx ON welfare_embeddings(program_id);
CREATE INDEX welfare_embeddings_vector_idx ON welfare_embeddings USING hnsw (embedding vector_cosine_ops);
