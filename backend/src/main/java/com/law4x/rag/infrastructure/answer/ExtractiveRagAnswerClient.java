package com.law4x.rag.infrastructure.answer;

import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.domain.repository.RagAnswerClient;
import java.util.List;

public class ExtractiveRagAnswerClient implements RagAnswerClient {

    @Override
    public String answer(String question, List<RagSearchResult> evidence) {
        if (evidence.isEmpty()) {
            return "暂未检索到足够相关的法条依据，建议补充借款事实、证据材料和争议金额后再查询。";
        }
        RagSearchResult top = evidence.get(0);
        return "根据检索到的相关法条，建议先围绕证据证明借款关系和还款期限；"
                + "如对方逾期未还，可结合"
                + top.documentTitle()
                + top.articleNo()
                + "主张相应权利。"
                + top.preview();
    }
}
