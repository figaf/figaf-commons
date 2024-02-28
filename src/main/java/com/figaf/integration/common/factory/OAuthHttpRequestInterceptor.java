package com.figaf.integration.common.factory;

import com.sap.cloud.security.xsuaa.client.OAuth2TokenResponse;
import com.sap.cloud.security.xsuaa.tokenflows.TokenFlowException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

import java.time.Instant;

/**
 * @author Arsenii Istlentev
 */
@Slf4j
class OAuthHttpRequestInterceptor implements HttpRequestInterceptor {

    private final CloudConnectorParameters cloudConnectorParameters;
    private final String locationId;
    private OAuth2TokenResponse accessToken;

    public OAuthHttpRequestInterceptor(CloudConnectorParameters cloudConnectorParameters, String locationId) {
        this.cloudConnectorParameters = cloudConnectorParameters;
        this.locationId = locationId;
    }

    @Override
    public void process(HttpRequest request, HttpContext context) throws TokenFlowException {
        if (cloudConnectorParameters == null) {
            return;
        }

        synchronized (this) {
            if (accessToken == null || (accessToken.getExpiredAt() != null && accessToken.getExpiredAt().isBefore(Instant.now()))) {
                CloudConnectorAccessTokenProvider cloudConnectorAccessTokenProvider = new CloudConnectorAccessTokenProvider();
                accessToken = cloudConnectorAccessTokenProvider.getToken(cloudConnectorParameters);
            }
            request.addHeader("Proxy-Authorization", String.format("%s %s", accessToken.getTokenType(), accessToken.getAccessToken()));
            if (StringUtils.isNotEmpty(locationId)) {
                request.addHeader("SAP-Connectivity-SCC-Location_ID", locationId);
            }
        }
    }
}
