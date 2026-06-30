import unittest

from scripts.search_law_articles import build_search_sql


class SearchLawArticlesTest(unittest.TestCase):
    def test_builds_keyword_search_sql(self):
        sql = build_search_sql("违约责任", limit=5)

        self.assertIn("FROM law_articles a", sql)
        self.assertIn("JOIN law_documents d", sql)
        self.assertIn("a.effective_status = 'effective'", sql)
        self.assertIn("d.status = 'effective'", sql)
        self.assertIn("similarity(a.content, '违约责任')", sql)
        self.assertIn("LIMIT 5", sql)

    def test_escapes_single_quotes_in_query(self):
        sql = build_search_sql("John's claim", limit=3)

        self.assertIn("'John''s claim'", sql)
        self.assertIn("LIMIT 3", sql)

    def test_rejects_invalid_limit(self):
        with self.assertRaises(ValueError):
            build_search_sql("违约责任", limit=0)


if __name__ == "__main__":
    unittest.main()
