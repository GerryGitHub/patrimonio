package com.patrimonio.inversiones.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class ReconstruirPosicionRequestTest {

    private final ObjectMapper mapper = JsonMapper.builder().build();

    @Test
    void deserializaCuerpoVacio() {
        ReconstruirPosicionRequest request = mapper.readValue(
            "{}", ReconstruirPosicionRequest.class);
        assertThat(request.debeReparar()).isFalse();
    }
}
