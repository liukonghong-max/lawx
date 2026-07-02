package com.law4x.agui.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CitationValidationServiceTest {

    @Test
    void validatesCitationIdsAgainstAllowedIds() {
        CitationValidationService service = new CitationValidationService();

        CitationValidationService.ValidationResult result = service.validate(
                List.of(
                        "11111111-1111-1111-1111-111111111111",
                        "not-a-uuid",
                        "33333333-3333-3333-3333-333333333333"
                ),
                List.of(
                        "11111111-1111-1111-1111-111111111111",
                        "22222222-2222-2222-2222-222222222222"
                )
        );

        assertThat(result.valid()).isFalse();
        assertThat(result.validCitationIds()).containsExactly("11111111-1111-1111-1111-111111111111");
        assertThat(result.invalidCitationIds()).containsExactly("not-a-uuid");
        assertThat(result.unsupportedCitationIds()).containsExactly("33333333-3333-3333-3333-333333333333");
        assertThat(result.missingAllowedCitationIds()).containsExactly("22222222-2222-2222-2222-222222222222");
    }
}
