package com.law4x.rag.infrastructure.answer;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "law4x.answer.openai")
public class OpenAiCompatibleAnswerProperties {

    private boolean enabled = false;
    private String apiKey;
    private String modelName = "ark-code-latest";
    private String baseUrl = "https://ark.cn-beijing.volces.com/api/coding/v3";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
