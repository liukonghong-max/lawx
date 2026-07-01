package com.law4x.law.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.law4x.law.domain.model.LawArticleDetail;
import com.law4x.law.domain.model.LawDocumentArticleItem;
import com.law4x.law.domain.model.LawDocumentSummary;
import com.law4x.law.domain.model.LawArticleSearchResult;
import com.law4x.law.domain.repository.LawArticleRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetLawArticleDetailUseCaseTest {

    @Test
    void rejectsInvalidArticleId() {
        GetLawArticleDetailUseCase useCase = new GetLawArticleDetailUseCase(new FakeLawArticleRepository());

        assertThatThrownBy(() -> useCase.get("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("articleId must be a valid UUID");
    }

    @Test
    void returnsArticleDetail() {
        UUID articleId = UUID.randomUUID();
        FakeLawArticleRepository repository = new FakeLawArticleRepository();
        repository.detail = Optional.of(new LawArticleDetail(
                articleId,
                "中华人民共和国民法典",
                "法律",
                "全国人民代表大会",
                LocalDate.of(2020, 5, 28),
                LocalDate.of(2021, 1, 1),
                "effective",
                "https://example.test/civil-code",
                "第三编 合同",
                "第八章 违约责任",
                null,
                "第五百七十七条",
                577,
                "当事人一方不履行合同义务或者履行合同义务不符合约定的，应当承担违约责任。",
                "中华人民共和国民法典 > 第三编 合同 > 第八章 违约责任 > 第五百七十七条",
                "effective"
        ));
        GetLawArticleDetailUseCase useCase = new GetLawArticleDetailUseCase(repository);

        Optional<LawArticleDetail> detail = useCase.get(articleId.toString());

        assertThat(repository.lastArticleId).isEqualTo(articleId);
        assertThat(detail).isPresent();
        assertThat(detail.get().articleNo()).isEqualTo("第五百七十七条");
    }

    private static class FakeLawArticleRepository implements LawArticleRepository {
        private Optional<LawArticleDetail> detail = Optional.empty();
        private UUID lastArticleId;

        @Override
        public List<LawDocumentSummary> listEffectiveDocuments(int limit) {
            return List.of();
        }

        @Override
        public PagedResult<LawDocumentArticleItem> listDocumentArticles(UUID documentId, int page, int pageSize) {
            return new PagedResult<>(List.of(), page, pageSize, 0);
        }

        @Override
        public List<LawArticleSearchResult> searchEffectiveArticles(String query, int limit) {
            return List.of();
        }

        @Override
        public Optional<LawArticleDetail> findArticleDetail(UUID articleId) {
            lastArticleId = articleId;
            return detail;
        }
    }
}
