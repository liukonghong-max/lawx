import unittest

from scripts.import_law_docx import build_import_sql
from scripts.law_docx_parser import LawArticle, ParsedLawDocument


class ImportLawDocxTest(unittest.TestCase):
    def test_builds_import_sql_for_document_and_articles(self):
        document = ParsedLawDocument(
            title="测试法",
            law_type="法律",
            publish_date="2026-06-30",
            source_file_name="test.docx",
            articles=[
                LawArticle(
                    article_no="第一条",
                    article_order=1,
                    content="测试内容",
                    book_title="第一编 总则",
                    chapter_title="第一章 基本规定",
                    section_title=None,
                    full_path="测试法 > 第一编 总则 > 第一章 基本规定 > 第一条",
                    content_hash="abc123",
                )
            ],
        )

        sql = build_import_sql(document)

        self.assertIn("INSERT INTO knowledge_import_jobs", sql)
        self.assertIn("INSERT INTO law_documents", sql)
        self.assertIn("INSERT INTO law_articles", sql)
        self.assertIn("jsonb_to_recordset", sql)
        self.assertIn('"article_no": "第一条"', sql)
        self.assertIn('"content_hash": "abc123"', sql)


if __name__ == "__main__":
    unittest.main()
