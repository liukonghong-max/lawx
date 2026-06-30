CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE knowledge_import_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name TEXT NOT NULL,
    source_url TEXT,
    source_type TEXT NOT NULL DEFAULT 'local_upload',
    law_type TEXT,
    status TEXT NOT NULL DEFAULT 'uploaded',
    detected_title TEXT,
    detected_publish_date DATE,
    total_articles INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_knowledge_import_jobs_status CHECK (
        status IN ('uploaded', 'parsing', 'parsed', 'published', 'failed')
    )
);

CREATE TABLE law_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    import_job_id UUID REFERENCES knowledge_import_jobs(id) ON DELETE SET NULL,
    title TEXT NOT NULL,
    law_type TEXT NOT NULL,
    issuer TEXT,
    document_no TEXT,
    publish_date DATE,
    effective_date DATE,
    status TEXT NOT NULL DEFAULT 'effective',
    source_url TEXT,
    source_file_name TEXT,
    checksum TEXT,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_law_documents_status CHECK (
        status IN ('effective', 'amended', 'repealed', 'unknown')
    )
);

CREATE TABLE law_articles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES law_documents(id) ON DELETE CASCADE,
    book_title TEXT,
    chapter_title TEXT,
    section_title TEXT,
    article_no TEXT NOT NULL,
    article_order INTEGER NOT NULL,
    content TEXT NOT NULL,
    full_path TEXT,
    effective_status TEXT NOT NULL DEFAULT 'effective',
    source_anchor TEXT,
    content_hash TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_law_articles_effective_status CHECK (
        effective_status IN ('effective', 'amended', 'repealed', 'unknown')
    ),
    CONSTRAINT uq_law_articles_document_order UNIQUE (document_id, article_order),
    CONSTRAINT uq_law_articles_document_article_no UNIQUE (document_id, article_no)
);

CREATE TABLE law_article_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    article_id UUID NOT NULL REFERENCES law_articles(id) ON DELETE CASCADE,
    embedding_model TEXT NOT NULL,
    content_hash TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_law_article_embeddings_model UNIQUE (article_id, embedding_model)
);

CREATE TABLE chat_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id TEXT,
    mode TEXT NOT NULL DEFAULT 'public_consultation',
    title TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_chat_sessions_mode CHECK (
        mode IN ('public_consultation', 'professional_search')
    )
);

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role TEXT NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_chat_messages_role CHECK (
        role IN ('user', 'assistant', 'system', 'tool')
    )
);

CREATE TABLE citations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    article_id UUID NOT NULL REFERENCES law_articles(id) ON DELETE RESTRICT,
    document_title TEXT NOT NULL,
    article_no TEXT NOT NULL,
    quoted_text TEXT NOT NULL,
    source_url TEXT,
    confidence NUMERIC(5, 4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE rag_test_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    query TEXT NOT NULL,
    keyword_results JSONB NOT NULL DEFAULT '[]'::jsonb,
    vector_results JSONB NOT NULL DEFAULT '[]'::jsonb,
    rerank_results JSONB NOT NULL DEFAULT '[]'::jsonb,
    selected_article_ids UUID[] NOT NULL DEFAULT ARRAY[]::UUID[],
    parameters JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE eval_cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    question TEXT NOT NULL,
    expected_document_titles TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    expected_article_nos TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    forbidden_claims TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    expected_notes TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE eval_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    model_name TEXT,
    embedding_model TEXT,
    status TEXT NOT NULL DEFAULT 'running',
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at TIMESTAMPTZ,
    CONSTRAINT chk_eval_runs_status CHECK (
        status IN ('running', 'completed', 'failed')
    )
);

CREATE TABLE eval_run_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    eval_run_id UUID NOT NULL REFERENCES eval_runs(id) ON DELETE CASCADE,
    eval_case_id UUID NOT NULL REFERENCES eval_cases(id) ON DELETE CASCADE,
    answer TEXT,
    retrieved_article_ids UUID[] NOT NULL DEFAULT ARRAY[]::UUID[],
    citation_valid BOOLEAN,
    retrieval_score NUMERIC(5, 4),
    answer_score NUMERIC(5, 4),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID REFERENCES chat_messages(id) ON DELETE SET NULL,
    rating TEXT NOT NULL,
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_feedback_rating CHECK (
        rating IN ('helpful', 'not_helpful', 'unsafe', 'wrong_citation')
    )
);

CREATE INDEX idx_knowledge_import_jobs_status
ON knowledge_import_jobs(status);

CREATE INDEX idx_law_documents_title
ON law_documents(title);

CREATE INDEX idx_law_documents_status
ON law_documents(status);

CREATE INDEX idx_law_documents_law_type
ON law_documents(law_type);

CREATE INDEX idx_law_documents_title_trgm
ON law_documents USING gin (title gin_trgm_ops);

CREATE INDEX idx_law_articles_document_id
ON law_articles(document_id);

CREATE INDEX idx_law_articles_article_no
ON law_articles(article_no);

CREATE INDEX idx_law_articles_effective_status
ON law_articles(effective_status);

CREATE INDEX idx_law_articles_content_trgm
ON law_articles USING gin (content gin_trgm_ops);

CREATE INDEX idx_law_article_embeddings_article_id
ON law_article_embeddings(article_id);

CREATE INDEX idx_law_article_embeddings_embedding
ON law_article_embeddings
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

CREATE INDEX idx_chat_messages_session_id
ON chat_messages(session_id);

CREATE INDEX idx_citations_message_id
ON citations(message_id);

CREATE INDEX idx_citations_article_id
ON citations(article_id);
