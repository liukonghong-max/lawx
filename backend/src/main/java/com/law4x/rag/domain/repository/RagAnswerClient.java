package com.law4x.rag.domain.repository;

import com.law4x.rag.domain.model.RagSearchResult;
import java.util.List;

public interface RagAnswerClient {

    String answer(String question, List<RagSearchResult> evidence);
}
