import unittest
from pathlib import Path

from scripts.law_docx_parser import parse_law_docx


FIXTURE = Path(__file__).resolve().parents[1] / "中华人民共和国民法典_20200528.docx"


class LawDocxParserTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.document = parse_law_docx(FIXTURE)

    def test_parses_civil_code_metadata(self):
        self.assertEqual(self.document.title, "中华人民共和国民法典")
        self.assertEqual(self.document.law_type, "法律")
        self.assertEqual(self.document.publish_date, "2020-05-28")
        self.assertEqual(self.document.source_file_name, "中华人民共和国民法典_20200528.docx")

    def test_parses_all_civil_code_articles(self):
        self.assertEqual(len(self.document.articles), 1260)
        self.assertEqual(self.document.articles[0].article_no, "第一条")
        self.assertEqual(self.document.articles[-1].article_no, "第一千二百六十条")

    def test_keeps_hierarchy_for_first_article(self):
        article = self.document.articles[0]
        self.assertEqual(article.book_title, "第一编 总则")
        self.assertEqual(article.chapter_title, "第一章 基本规定")
        self.assertIsNone(article.section_title)
        self.assertEqual(article.full_path, "中华人民共和国民法典 > 第一编 总则 > 第一章 基本规定 > 第一条")
        self.assertIn("为了保护民事主体的合法权益", article.content)

    def test_parses_contract_liability_article(self):
        article = self.document.article_by_no("第五百七十七条")
        self.assertEqual(article.book_title, "第三编 合同")
        self.assertEqual(article.chapter_title, "第八章 违约责任")
        self.assertIn("不履行合同义务", article.content)
        self.assertIn("违约责任", article.content)

    def test_merges_continuation_paragraphs_into_article(self):
        article = self.document.article_by_no("第一千二百五十四条")
        self.assertIn("禁止从建筑物中抛掷物品", article.content)
        self.assertIn("物业服务企业等建筑物管理人", article.content)
        self.assertIn("公安等机关应当依法及时调查", article.content)


if __name__ == "__main__":
    unittest.main()
