package com.law4x.agui.application.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AgUiMessageCitationService {

    private final Map<String, PendingCitationContext> pendingByRunKey = new ConcurrentHashMap<>();
    private final Map<String, MessageCitationPayload> citationsByMessageId = new ConcurrentHashMap<>();

    public void reserve(String threadId, String runId, Map<String, Object> state) {
        pendingByRunKey.put(runKey(threadId, runId), PendingCitationContext.from(state));
    }

    public void bindAssistantMessage(String threadId, String runId, String messageId) {
        PendingCitationContext pending = pendingByRunKey.remove(runKey(threadId, runId));
        if (pending == null || messageId == null || messageId.isBlank()) {
            return;
        }
        citationsByMessageId.put(messageId, new MessageCitationPayload(
                messageId,
                pending.citations(),
                pending.allowedCitationIds()
        ));
    }

    public Optional<MessageCitationPayload> findByMessageId(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(citationsByMessageId.get(messageId));
    }

    private String runKey(String threadId, String runId) {
        return (threadId == null ? "" : threadId) + ":" + (runId == null ? "" : runId);
    }

    public record MessageCitationPayload(
            String messageId,
            List<Map<String, Object>> citations,
            List<String> allowedCitationIds
    ) {
    }

    private record PendingCitationContext(
            List<Map<String, Object>> citations,
            List<String> allowedCitationIds
    ) {
        private static PendingCitationContext from(Map<String, Object> state) {
            List<Map<String, Object>> citations = new ArrayList<>();
            List<String> allowedCitationIds = new ArrayList<>();
            if (state != null) {
                Object citationValue = state.get("citations");
                if (citationValue instanceof List<?> citationList) {
                    for (Object item : citationList) {
                        if (item instanceof Map<?, ?> citationMap) {
                            LinkedHashMap<String, Object> normalized = new LinkedHashMap<>();
                            normalized.put("articleId", stringValue(citationMap.get("articleId")));
                            normalized.put("documentTitle", stringValue(citationMap.get("documentTitle")));
                            normalized.put("articleNo", stringValue(citationMap.get("articleNo")));
                            normalized.put("fullPath", stringValue(citationMap.get("fullPath")));
                            normalized.put("quotedText", stringValue(citationMap.get("quotedText")));
                            citations.add(normalized);
                        }
                    }
                }
                Object allowedValue = state.get("allowedCitationIds");
                if (allowedValue instanceof List<?> allowedList) {
                    for (Object item : allowedList) {
                        String value = stringValue(item);
                        if (!value.isBlank()) {
                            allowedCitationIds.add(value);
                        }
                    }
                }
            }
            return new PendingCitationContext(List.copyOf(citations), List.copyOf(allowedCitationIds));
        }

        private static String stringValue(Object value) {
            return value == null ? "" : String.valueOf(value);
        }
    }
}
