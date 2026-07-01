package com.law4x.rag.domain.repository;

import com.law4x.rag.domain.model.RagTestRun;

public interface RagTestRunRepository {

    RagTestRun save(RagTestRun testRun);
}
