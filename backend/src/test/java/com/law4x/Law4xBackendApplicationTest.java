package com.law4x;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

class Law4xBackendApplicationTest {

    @Test
    void exposesSpringBootApplicationEntryPoint() {
        assertThat(Law4xBackendApplication.class)
                .hasDeclaredMethods("main");
        assertThat(SpringApplication.class).isNotNull();
    }
}
