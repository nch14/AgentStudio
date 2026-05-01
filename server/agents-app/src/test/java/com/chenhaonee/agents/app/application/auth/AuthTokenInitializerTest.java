package com.chenhaonee.agents.app.application.auth;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chenhaonee.agents.app.infrastructure.security.AuthProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AuthTokenInitializerTest {

    @Test
    void shouldInitializeFromBootstrapConfigWhenNoCurrentTokenExists() {
        AuthTokenApplicationService service = Mockito.mock(AuthTokenApplicationService.class);
        AuthProperties properties = new AuthProperties();
        properties.setInitialToken("bootstrap-token");
        when(service.hasCurrentToken()).thenReturn(false);

        new AuthTokenInitializer(service, properties).run(null);

        verify(service).initializeFromBootstrap("bootstrap-token");
    }

    @Test
    void shouldNotOverrideExistingToken() {
        AuthTokenApplicationService service = Mockito.mock(AuthTokenApplicationService.class);
        AuthProperties properties = new AuthProperties();
        properties.setInitialToken("bootstrap-token");
        when(service.hasCurrentToken()).thenReturn(true);

        new AuthTokenInitializer(service, properties).run(null);

        verify(service, never()).initializeFromBootstrap("bootstrap-token");
    }

    @Test
    void shouldSkipWhenBootstrapTokenIsBlank() {
        AuthTokenApplicationService service = Mockito.mock(AuthTokenApplicationService.class);
        AuthProperties properties = new AuthProperties();
        properties.setInitialToken(" ");
        when(service.hasCurrentToken()).thenReturn(false);

        new AuthTokenInitializer(service, properties).run(null);

        verify(service, never()).initializeFromBootstrap(" ");
    }
}
