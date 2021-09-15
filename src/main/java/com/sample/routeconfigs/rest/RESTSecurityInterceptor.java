package com.sample.routeconfigs.rest;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * The JWT security interceptor for the publication to REST endpoints.
 * If the JWT security fails, this interceptor does not interrupt the publication. The JWT
 * headers would simply not be present, in which case, the target system is expected to fail with an
 * HTTP 401/403 response.
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RESTSecurityInterceptor implements ClientHttpRequestInterceptor {

    private final JWTAuthenticator authenticator;
    private final Lock lock = new ReentrantLock(true);

    @Value("${jwt.domain}")
    private String jwtDomain;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpHeaders headers = request.getHeaders();
        if (authenticator.isJwtEnabled()) {
            sign(headers);
        }
        Map<String, Collection<String>> newHeaders = headers
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> (Collection<String>) entry.getValue()));
        log.debug("Set JWT headers={}", newHeaders.toString());
        return execution.execute(request, body);
    }

    public void sign(Object headers) throws IOException {
        try {
            log.info("Signing JWT for HTTP");
            if (authenticator.isJwtEnabled()) {
                ensureJWTAuthenticatorConfig();
                authenticator.sign(headers, jwtDomain);
                log.info("JWT successfully signed.");
            } else {
                log.info("JWT disabled for local testing.");
            }
        } catch (samplecdt.auth.clientsdk.exceptions.sampleAuthException exception) {
            // In local test profile when test.cerberus.local=true, set test.cerberus.credentials.popup=true for credentials popup.
            // Remember to set credentials popup back to false for merge to master, or build will hand in Jenkins.
            handleAuthenticatorException(exception);
        } catch (Exception exception) {
            handleAuthenticatorException(exception);
        }
    }

    private void handleAuthenticatorException(Exception exception) throws IOException {
        log.error("ClientHttpResponse: RESTRESTSecurityInterceptor unable to JWT-secure headers. exception={} localizemessage={}", exception, exception.getLocalizedMessage());
        throw new IOException();

    }

    /**
     * Throws an exception if the JWT Autenticator config does not yet exist.
     * <p>
     * The catch block creates the config in this case.
     *
     * @throws Exception
     */
    private void ensureJWTAuthenticatorConfig() throws Exception {
        log.info("Ensuring the existence of sample JWT Authenticator Config for JWT signing.");
        lock.lock();
        try {
            sampleJWTClientAuth.getFullInstanceID(authenticator.getConfigId());
            log.info("JWT Authenticator config exists. Proceeding with signing.");
        } catch (sampleAuthException e) {
            log.warn("sample JWT Authenticator config does not exist at JWT signing time.");
            authenticator.configureWithDefaultSettings();
            log.info("sample JWT Authenticator config was successfully created for JWT signing");
        } finally {
            lock.unlock();
        }
    }
}
