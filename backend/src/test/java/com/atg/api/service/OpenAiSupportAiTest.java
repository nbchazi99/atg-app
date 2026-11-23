package com.atg.api.service;

import com.atg.api.model.SupportMessage;
import com.atg.api.web.SupportDtos;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenAiSupportAiTest {

    @Test
    void answersGeneralQuestionsWhenNoAtgKnowledgeMatches() throws Exception {
        ObjectMapper json = new ObjectMapper();
        RestClient client = mock(RestClient.class);
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(client.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any(MediaType.class))).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);

        JsonNode response = json.readTree("""
                {
                  "choices": [{
                    "message": {
                      "content": "{\\"message\\":\\"Paris is the capital of France.\\",\\"category\\":\\"GENERAL\\",\\"priority\\":\\"NORMAL\\",\\"needsHuman\\":false}"
                    }
                  }]
                }
                """);
        when(responseSpec.body(JsonNode.class)).thenReturn(response);

        SupportMessage question = new SupportMessage();
        question.setText("What is the capital of France?");
        OpenAiSupportAi assistant = new OpenAiSupportAi(json, "test-key", "test-model", client);

        SupportDtos.AiReply reply = assistant.answer(List.of(question), List.of());

        assertEquals("Paris is the capital of France.", reply.message());
        assertFalse(reply.needsHuman());
        var request = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(bodySpec).body(request.capture());
        JsonNode requestJson = json.valueToTree(request.getValue());
        assertTrue(requestJson.path("messages").path(0).path("content").asText()
                .contains("Answer general-knowledge questions too"));
        assertEquals("What is the capital of France?",
                requestJson.path("messages").path(1).path("content").asText());
    }
}
