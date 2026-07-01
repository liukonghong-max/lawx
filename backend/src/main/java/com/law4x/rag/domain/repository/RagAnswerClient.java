package com.law4x.rag.domain.repository;

import com.law4x.rag.domain.model.RagAnswer;
import com.law4x.rag.domain.model.RagSearchResult;
import java.util.List;

public interface RagAnswerClient {

    RagAnswerPayload answer(String question, List<RagSearchResult> evidence);

    record RagAnswerPayload(
            String answer,
            List<RagAnswer.AnswerSegment> answerSegments
    ) {
        public RagAnswerPayload(String answer) {
            this(answer, List.of());
        }
    }
}
