/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBM Corporation 2016, 2018
 */

package com.ibm.atlas.utilities.client;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.Cookie;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ibm.atlas.utilities.JsonUtils;
import com.ibm.websphere.security.web.WebSecurityHelper;

public class HTTPClient implements AutoCloseable {
	private static final Logger log = Logger.getLogger(HTTPClient.class.getName());

	private static final String SERVER_SCHEME = "https";
	private static final String SERVER_HOST;
	private static final String LTPA_TOKEN = "LtpaToken2";
	private static final String HEADER_COOKIE = "Cookie";

	private Client client;
	private URI baseUri;
	private String sso;

	static {
		SERVER_HOST = System.getenv("ZOSMF_HOST") != null ? System.getenv("ZOSMF_HOST") : "localhost";
	}

	public HTTPClient(UriInfo hostUriInfo) throws KeyManagementException, NoSuchAlgorithmException {
		this.baseUri = hostUriInfo.getBaseUri();

		Cookie cookie = null;
		try {
			cookie = WebSecurityHelper.getSSOCookieFromSSOToken();
			if (cookie != null) {
				sso = LTPA_TOKEN + "=" + cookie.getValue();
			}
		} catch (Exception e) {
			String error = "Exception while extracting SSO Cookie, failed to send request";
			log.log(Level.SEVERE, error, e);
		}

		this.client = createWebClient();
	}

	/**
	 * Create a WebTarget relative to the current server, based on the provided context root
	 * 
	 * @param contextRoot The context root used by the target microservice
	 * @return The WebTarget for making requests to the microservice
	 */
	public WebTarget createTarget(String contextRoot) {
		URI uri = UriBuilder.fromPath(String.format("%s://%s:%d/%s", baseUri.getScheme(), SERVER_HOST, baseUri.getPort(), contextRoot)).build();
		return client.target(uri);
	}

	/**
	 * Create a WebTarget relative to the current server, based on the provided port (if different) and context root
	 * 
	 * @param port The port that the microservice is running on
	 * @param contextRoot The context root used by the target microservice
	 * @return The WebTarget for making requests to the microservice
	 */
	public WebTarget createTarget(int port, String contextRoot) {
		URI uri = UriBuilder.fromPath(String.format("%s://%s:%d/%s", SERVER_SCHEME, SERVER_HOST, port, contextRoot)).build();
		return client.target(uri);
	}

	/**
	 * Send the request using the specified HTTP method. Attaches the SSO LTPA token for auth.
	 * 
	 * @param request The request to send
	 * @param method The HTTP method to use
	 * @return The response from the microservice or a server error if sending failed
	 */
	public Response sendRequest(Builder request, String method) {

		if (sso != null) {
			return request.header(HEADER_COOKIE, sso).method(method);
		}

		String error = "SSO Cookie from SSO Token was null, failed to send request";
		log.log(Level.SEVERE, error);
		return Response.serverError().entity(error).build();
	}

	public Response putRequestWithContent(Builder request, Object content, MediaType contentType) throws JsonProcessingException {
		String body = JsonUtils.convertToJsonString(content);
		return putRequestWithContent(request, body, contentType);
	}

	/**
	 * Send the request using the specified HTTP method. Attaches the SSO LTPA token for auth.
	 * 
	 * @param request The request to send
	 * @param method The HTTP method to use
	 * @return The response from the microservice or a server error if sending failed
	 */
	public Response putRequestWithContent(Builder request, String content, MediaType contentType) {

		if (sso != null) {
			return request.header(HEADER_COOKIE, sso).put(Entity.entity(content, contentType));
		}

		String error = "SSO Cookie from SSO Token was null, failed to send request";
		log.log(Level.SEVERE, error);
		return Response.serverError().entity(error).build();
	}

	public Response postRequestWithContent(Builder request, Object content, MediaType contentType) throws JsonProcessingException {
		String body = JsonUtils.convertToJsonString(content);
		return postRequestWithContent(request, body, contentType);
	}

	public Response postRequestWithContent(Builder request, String content, MediaType contentType) {

		if (sso != null) {
			return request.header(HEADER_COOKIE, sso).post(Entity.entity(content, contentType));
		}

		String error = "SSO Cookie from SSO Token was null, failed to send request";
		log.log(Level.SEVERE, error);
		return Response.serverError().entity(error).build();
	}

	/*
	 * Ignore potential self-signed certificate complaints
	 */
	private static Client createWebClient() throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext sslContext = SSLContext.getInstance("TLS");
		TrustManager[] trustAll = new TrustManager[] { new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
				// Trust all clients
			}

			@Override
			public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
				// Trust all servers
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}
		} };
		sslContext.init(null, trustAll, null);

		return ClientBuilder.newBuilder().sslContext(sslContext).hostnameVerifier(new HostnameVerifier() {

			@Override
			public boolean verify(String s1, SSLSession s2) {
				return true;
			}

		}).build();
	}

	@Override
	public void close() {
		if (client != null) {
			client.close();
		}
	}
}
