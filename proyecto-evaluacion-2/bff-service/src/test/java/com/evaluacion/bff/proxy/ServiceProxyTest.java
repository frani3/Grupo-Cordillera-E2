package com.evaluacion.bff.proxy;

import com.evaluacion.bff.model.DataResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Tests unitarios puros (sin Spring). Usan el constructor package-private
// de ServiceProxy para inyectar un IOrqService mock → aislamiento total.
@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceProxy — Patrón Proxy (tests unitarios)")
class ServiceProxyTest {

    @Mock private IOrqService mockRealSubject;
    private ServiceProxy proxy;

    @BeforeEach
    void setUp() { proxy = new ServiceProxy(mockRealSubject); }

    @Nested @DisplayName("Proxy de Protección — validación de token")
    class TokenValidation {
        @Test void lanza_SecurityException_cuando_token_es_null() {
            assertThrows(SecurityException.class, () -> proxy.fetchData("r1", null));
            verifyNoInteractions(mockRealSubject);
        }
        @Test void lanza_SecurityException_cuando_token_esta_en_blanco() {
            assertThrows(SecurityException.class, () -> proxy.fetchData("r1", "  "));
            verifyNoInteractions(mockRealSubject);
        }
        @Test void lanza_SecurityException_sin_esquema_Bearer() {
            assertThrows(SecurityException.class, () -> proxy.fetchData("r1", "Basic abc"));
            verifyNoInteractions(mockRealSubject);
        }
    }

    @Nested @DisplayName("Proxy de Delegación — forwarding al RealSubject")
    class Delegation {
        private static final String TOKEN = "Bearer eyJhbGciOiJIUzI1NiJ9.test";

        @Test void delega_al_RealSubject_con_token_valido() {
            DataResponse expected = DataResponse.ok("payload", "orq-service");
            when(mockRealSubject.fetchData("req-1", TOKEN)).thenReturn(expected);

            assertSame(expected, proxy.fetchData("req-1", TOKEN));
            verify(mockRealSubject, times(1)).fetchData("req-1", TOKEN);
        }

        @Test void retorna_error_si_RealSubject_lanza_excepcion() {
            when(mockRealSubject.fetchData(anyString(), anyString()))
                    .thenThrow(new RuntimeException("timeout"));

            DataResponse result = proxy.fetchData("req-fail", TOKEN);

            assertEquals("error", result.status());
        }

        @Test void no_llama_al_RealSubject_cuando_token_invalido() {
            try { proxy.fetchData("r", null); } catch (SecurityException ignored) {}
            verifyNoInteractions(mockRealSubject);
        }
    }

    @Test @DisplayName("ServiceProxy implementa IOrqService (transparencia del patrón)")
    void proxy_implementa_IOrqService() {
        assertInstanceOf(IOrqService.class, proxy);
    }
}
