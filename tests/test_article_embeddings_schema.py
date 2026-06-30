from pathlib import Path
import unittest


class ArticleEmbeddingsSchemaTest(unittest.TestCase):
    def test_initial_schema_has_law_article_embeddings_table_for_pgvector(self):
        migration = Path("db/migrations/001_initial_schema.sql")

        sql = migration.read_text(encoding="utf-8")

        self.assertIn("CREATE TABLE law_article_embeddings", sql)
        self.assertIn("article_id UUID NOT NULL REFERENCES law_articles(id) ON DELETE CASCADE", sql)
        self.assertIn("embedding vector(1536) NOT NULL", sql)
        self.assertIn("embedding_model TEXT NOT NULL", sql)
        self.assertIn("content_hash TEXT NOT NULL", sql)
        self.assertIn("CREATE INDEX idx_law_article_embeddings_embedding", sql)
        self.assertIn("USING ivfflat (embedding vector_cosine_ops)", sql)


if __name__ == "__main__":
    unittest.main()
