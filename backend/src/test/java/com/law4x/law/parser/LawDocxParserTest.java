package com.law4x.law.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LawDocxParserTest {

    private static ParsedLawDocument document;

    @BeforeAll
    static void parseDocument() throws Exception {
        document = new LawDocxParser().parse(Path.of("..", "中华人民共和国民法典_20200528.docx"));
    }

    @Test
    void parsesCivilCodeMetadata() {
        assertThat(document.title()).isEqualTo("中华人民共和国民法典");
        assertThat(document.lawType()).isEqualTo("法律");
        assertThat(document.publishDate()).isEqualTo("2020-05-28");
        assertThat(document.sourceFileName()).isEqualTo("中华人民共和国民法典_20200528.docx");
    }

    @Test
    void parsesAllCivilCodeArticles() {
        assertThat(document.articles()).hasSize(1260);
        assertThat(document.articles().get(0).articleNo()).isEqualTo("第一条");
        assertThat(document.articles().get(document.articles().size() - 1).articleNo()).isEqualTo("第一千二百六十条");
    }

    @Test
    void keepsHierarchyForFirstArticle() {
        LawArticle article = document.articleByNo("第一条");

        assertThat(article.bookTitle()).isEqualTo("第一编 总则");
        assertThat(article.chapterTitle()).isEqualTo("第一章 基本规定");
        assertThat(article.sectionTitle()).isNull();
        assertThat(article.fullPath()).isEqualTo("中华人民共和国民法典 > 第一编 总则 > 第一章 基本规定 > 第一条");
        assertThat(article.content()).contains("为了保护民事主体的合法权益");
    }

    @Test
    void parsesContractLiabilityArticle() {
        LawArticle article = document.articleByNo("第五百七十七条");

        assertThat(article.bookTitle()).isEqualTo("第三编 合同");
        assertThat(article.chapterTitle()).isEqualTo("第八章 违约责任");
        assertThat(article.content()).contains("不履行合同义务");
        assertThat(article.content()).contains("违约责任");
    }

    @Test
    void mergesContinuationParagraphsIntoArticle() {
        LawArticle article = document.articleByNo("第一千二百五十四条");

        assertThat(article.content()).contains("禁止从建筑物中抛掷物品");
        assertThat(article.content()).contains("物业服务企业等建筑物管理人");
        assertThat(article.content()).contains("公安等机关应当依法及时调查");
    }
}
