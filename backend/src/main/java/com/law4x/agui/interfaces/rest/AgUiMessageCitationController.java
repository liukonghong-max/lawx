package com.law4x.agui.interfaces.rest;

import com.law4x.agui.application.service.AgUiMessageCitationService;
import com.law4x.common.interfaces.rest.ApiErrorCode;
import com.law4x.common.interfaces.rest.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ag-ui/messages")
public class AgUiMessageCitationController {

    private final AgUiMessageCitationService agUiMessageCitationService;

    public AgUiMessageCitationController(AgUiMessageCitationService agUiMessageCitationService) {
        this.agUiMessageCitationService = agUiMessageCitationService;
    }

    @GetMapping("/{messageId}/citations")
    public ResponseEntity<ApiResponse<MessageCitationResponse>> getCitations(@PathVariable String messageId) {
        return agUiMessageCitationService.findByMessageId(messageId)
                .map(payload -> new MessageCitationResponse(
                        payload.messageId(),
                        payload.citations(),
                        payload.allowedCitationIds()
                ))
                .map(ApiResponse::success)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity
                        .status(404)
                        .body(ApiResponse.error(ApiErrorCode.FAILURE, "message citations not found")));
    }

    public record MessageCitationResponse(
            String messageId,
            List<Map<String, Object>> citations,
            List<String> allowedCitationIds
    ) {
    }
}
