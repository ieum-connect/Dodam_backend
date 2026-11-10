package com.ieum.dodam.medical_document.api.controller;

import com.ieum.dodam.medical_document.query.application.OriginalDocumentQueryService;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.user.query.application.UserQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.bind.support.WebDataBinderFactory;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** MVC response contract only; authorization is covered by service/integration tests. */
class OriginalDocumentControllerTest {
    private final OriginalDocumentQueryService originals = mock(OriginalDocumentQueryService.class);
    private final UserQueryService users = mock(UserQueryService.class);
    private final OAuth2User principal = mock(OAuth2User.class);
    private final User actor = new User();
    private final UUID documentId = UUID.randomUUID();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        when(principal.getAttributes()).thenReturn(Map.of("id", 12345L));
        when(users.requireByKakaoId("12345")).thenReturn(actor);
        mvc = MockMvcBuilders.standaloneSetup(new OriginalDocumentController(originals, users))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType() == OAuth2User.class;
                    }
                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                                  NativeWebRequest request, WebDataBinderFactory factory) {
                        return principal;
                    }
                }).build();
    }

    @ParameterizedTest
    @CsvSource({
            "application/pdf,application/pdf,inline",
            "image/png,image/png,inline",
            "image/jpeg,image/jpeg,inline",
            "text/html,application/octet-stream,attachment",
            "application/octet-stream,application/octet-stream,attachment"
    })
    void originalResponsePreservesBytesAndSafeHeaders(String storedType, String responseType,
                                                     String dispositionType) throws Exception {
        byte[] bytes = new byte[]{0, 1, 2, 127, -1};
        String filename = "아이 검진 결과.pdf";
        when(originals.get(actor, documentId)).thenReturn(
                new OriginalDocumentQueryService.Original(bytes, storedType, filename));

        var response = mvc.perform(get("/api/documents/{documentId}/original", documentId))
                .andExpect(status().isOk())
                .andExpect(content().bytes(bytes))
                .andExpect(content().contentType(responseType))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn().getResponse();

        var disposition = ContentDisposition.parse(response.getHeader(HttpHeaders.CONTENT_DISPOSITION));
        assertThat(disposition.getType()).isEqualTo(dispositionType);
        assertThat(disposition.getFilename()).isEqualTo(filename);
        verify(users).requireByKakaoId("12345");
        verify(originals).get(actor, documentId);
    }
}
