/*
 * Copyright 2014-2023 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jkoolcloud.jesl.net.http;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.io.ConnectionEndpoint;
import org.apache.hc.client5.http.io.LeaseRequest;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.HttpClientConnection;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.util.Timeout;

import com.jkoolcloud.jesl.net.http.apache.HttpRequestImpl;
import com.jkoolcloud.jesl.net.http.apache.HttpResponseImpl;
import com.jkoolcloud.jesl.net.security.AccessResponse;
import com.jkoolcloud.jesl.net.security.AuthUtils;
import com.jkoolcloud.jesl.net.ssl.SSLContextFactory;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.utils.Utils;

/**
 * This class provides HTTP[S] connection to the specified JESL server based on given URL.
 *
 * @version $Revision: 3 $
 */
public class HttpClient implements HttpStream {
	private static final String PRAGMA_VALUE_NO_RESPONSE = "no-response";
	private static final String PRAGMA_VALUE_PING = "ping";
	private static final String X_API_KEY = "X-API-Key";

	private static final String SCHEME_HTTP = "http";
	private static final String SCHEME_HTTPS = "https";
	private static final String LOCAL_HOST = "localhost";

	public static final long DEFAULT_CONN_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

	protected URI uri;
	protected EventSink logger;
	protected String host;
	protected int port;
	protected long connTimeout = DEFAULT_CONN_TIMEOUT;
	protected String uriPath;
	protected String sslKeystore;
	protected String sslKeystorePwd;
	protected HttpHost httpHost;
	protected HttpHost httpProxy;
	protected String proxyCredentials;
	protected BasicHttpClientConnectionManager connMgr;
	protected HttpClientConnection connection;
	protected boolean secure = false;

	/**
	 * Create JESL HTTP[S] client stream with given attributes
	 *
	 * @param urlStr
	 *            connection string to specified JESL server
	 * @param logger
	 *            event sink used for logging, null if none
	 * @throws URISyntaxException
	 *             if invalid connection string
	 */
	public HttpClient(String urlStr, EventSink logger) throws URISyntaxException {
		this(urlStr, null, 0, null, logger);
	}

	/**
	 * Create JESL HTTP[S] client stream with given attributes
	 *
	 * @param urlStr
	 *            connection string to specified JESL server
	 * @param proxyHost
	 *            proxy host name if any, null if none
	 * @param proxyPort
	 *            proxy port number if any, 0 of none
	 * @param proxyScheme
	 *            proxy communication scheme
	 * @param logger
	 *            event sink used for logging, null if none
	 * @throws URISyntaxException
	 *             if invalid connection string
	 */
	public HttpClient(String urlStr, String proxyHost, int proxyPort, String proxyScheme, EventSink logger)
			throws URISyntaxException {
		this(urlStr, DEFAULT_CONN_TIMEOUT, proxyHost, proxyPort, proxyScheme, logger);
	}

	/**
	 * Create JESL HTTP[S] client stream with given attributes
	 *
	 * @param urlStr
	 *            connection string to specified JESL server
	 * @param connTimeout
	 *            connection timeout in milliseconds
	 * @param proxyHost
	 *            proxy host name if any, null if none
	 * @param proxyPort
	 *            proxy port number if any, 0 of none
	 * @param proxyScheme
	 *            proxy communication scheme
	 * @param logger
	 *            event sink used for logging, null if none
	 * @throws URISyntaxException
	 *             if invalid connection string
	 */
	public HttpClient(String urlStr, long connTimeout, String proxyHost, int proxyPort, String proxyScheme,
			EventSink logger) throws URISyntaxException {
		this(urlStr, connTimeout, proxyHost, proxyPort, proxyScheme, null, null, logger);
	}

	/**
	 * Create JESL HTTP[S] client stream with given attributes
	 *
	 * @param urlStr
	 *            connection string to specified JESL server
	 * @param connTimeout
	 *            connection timeout in milliseconds
	 * @param proxyHost
	 *            proxy host name if any, null if none
	 * @param proxyPort
	 *            proxy port number if any, 0 of none
	 * @param proxyScheme
	 *            proxy communication scheme
	 * @param proxyUser
	 *            proxy authentication user
	 * @param proxyPass
	 *            proxy authentication password
	 * @param logger
	 *            event sink used for logging, null if none
	 * @throws URISyntaxException
	 *             if invalid connection string
	 */
	public HttpClient(String urlStr, long connTimeout, String proxyHost, int proxyPort, String proxyScheme,
			String proxyUser, String proxyPass, EventSink logger) throws URISyntaxException {
		uri = new URI(urlStr);
		String scheme = uri.getScheme();
		secure = SCHEME_HTTPS.equalsIgnoreCase(scheme);
		host = uri.getHost();
		if (host == null) {
			host = LOCAL_HOST;
		}
		port = uri.getPort();
		if (port <= 0) {
			port = (secure ? 443 : 80);
		}
		init(host, port, uri.getPath(), secure, connTimeout, proxyHost, proxyPort, proxyScheme, proxyUser, proxyPass,
				logger);
	}

	/**
	 * Initializes client properties.
	 */
	private void init(String host, int port, String uriPath, boolean secure, long timeout, String proxyHost,
			int proxyPort, String proxyScheme, String proxyUser, String proxyPass, EventSink logger) {
		this.host = host;
		this.port = port;
		this.uriPath = uriPath;
		this.secure = secure;
		this.connTimeout = timeout;
		this.logger = (logger != null ? logger : DefaultEventSinkFactory.defaultEventSink(HttpClient.class));

		String scheme = secure ? SCHEME_HTTPS : SCHEME_HTTP;

		httpHost = new HttpHost(scheme, host, port);
		if (!StringUtils.isEmpty(proxyHost)) {
			httpProxy = new HttpHost(proxyScheme, proxyHost, proxyPort);
		}
		if (StringUtils.isNotEmpty(proxyUser)) {
			proxyCredentials = proxyUser.trim() + ":" + proxyPass == null ? "" : proxyPass;
			proxyCredentials = Base64.encodeBase64String(proxyCredentials.getBytes());
		}
	}

	/**
	 * Sets keystore and password for obtaining SSL certificate for server.
	 *
	 * @param sslKeystore
	 *            path to keystore
	 * @param sslKeystorePwd
	 *            password for keystore
	 */
	public void setSslKeystore(String sslKeystore, String sslKeystorePwd) {
		this.sslKeystore = sslKeystore;
		this.sslKeystorePwd = sslKeystorePwd;
	}

	/**
	 * Initialize HTTP connection manager context should be called on connect.
	 * 
	 * @param startTime
	 *            timestamp in milliseconds when connection initiation started
	 */
	private synchronized void initHttpConnMgr(long startTime) throws NoSuchAlgorithmException {
		Registry<ConnectionSocketFactory> schemeReg = null;
		if (secure) {
			SSLContext sslCtx;
			if (!StringUtils.isEmpty(sslKeystore)) {
				SSLContextFactory scf = new SSLContextFactory(sslKeystore, sslKeystorePwd, sslKeystorePwd);
				sslCtx = scf.getSslContext(true);
			} else {
				sslCtx = SSLContext.getDefault();
			}

			String[] supportedProtocols = sslCtx.getSupportedSSLParameters().getProtocols();
			supportedProtocols = TLS.excludeWeak(supportedProtocols);

			SSLConnectionSocketFactory ssf = new SSLConnectionSocketFactory(sslCtx, supportedProtocols, null,
					NoopHostnameVerifier.INSTANCE);
			RegistryBuilder<ConnectionSocketFactory> rcf = RegistryBuilder.create();
			schemeReg = rcf.register(SCHEME_HTTPS, ssf).build();
		}
		connMgr = schemeReg != null ? new BasicHttpClientConnectionManager(schemeReg)
				: new BasicHttpClientConnectionManager();
		logger.log(OpLevel.DEBUG, "Created connection manager uri={0}, secure={1}, elapsed.ms={2}", uri, secure,
				(System.currentTimeMillis() - startTime));
	}

	/**
	 * Open HTTP connection
	 * 
	 * @param startTime
	 *            timestamp in milliseconds when connection initiation started
	 */
	private synchronized void openHttpConn(long startTime) throws Exception {
		HttpRoute route = (httpProxy != null) ? new HttpRoute(httpHost, null, httpProxy, secure)
				: new HttpRoute(httpHost, null, secure);
		Timeout timeout = Timeout.of(connTimeout, TimeUnit.MILLISECONDS);
		LeaseRequest connLeaseReq = connMgr.lease(null, route, timeout, null);
		ConnectionEndpoint connectionEndpoint = connLeaseReq.get(timeout);

		// httpclient5 API hides connection instance and handling internally within connection manager/endpoint.
		// To make as little changes as possible for now will use reflection to get connection instance.
		Method connMethod = connectionEndpoint.getClass().getDeclaredMethod("getConnection");
		connMethod.setAccessible(true);
		connection = (HttpClientConnection) connMethod.invoke(connectionEndpoint);

		HttpClientContext context = HttpClientContext.create();

		if (!connection.isOpen()) {
			connMgr.connect(connectionEndpoint, timeout, context);
			connectionEndpoint.setSocketTimeout(timeout);
		}
		logger.log(OpLevel.DEBUG, "Connected to uri={0}, proxy={1}, elapsed.ms={2}, timeout.ms={3}", uri,
				(httpProxy != null ? httpProxy : "(no-proxy)"), (System.currentTimeMillis() - startTime), connTimeout);
	}

	@Override
	public synchronized void connect() throws IOException {
		long startTime = System.currentTimeMillis();
		try {
			if (isConnected()) {
				return;
			}
			initHttpConnMgr(startTime);
			openHttpConn(startTime);
		} catch (Throwable e) {
			String errMsg = "Failed to connect to uri=" + uri //
					+ ", http.host=" + httpHost //
					+ ", timeout.ms=" + connTimeout //
					+ ", elapsed.ms=" + (System.currentTimeMillis() - startTime) //
					+ ", reason=" + e.getMessage();
			logger.log(OpLevel.ERROR, errMsg, e);
			_close();
			throw new IOException(errMsg, e);
		}
	}

	@Override
	public synchronized void connect(String token) throws IOException {
		connect();
		if (!StringUtils.isEmpty(token)) {
			try {
				logger.log(OpLevel.DEBUG, "Authenticating connection={0}, token={1}", this, Utils.hide(token, "x", 4));
				AuthUtils.authenticate(this, token);
				logger.log(OpLevel.DEBUG, "Authenticated connection={0}, token={1}", this, Utils.hide(token, "x", 4));
			} catch (SecurityException exc) {
				_close();
				throw new IOException("Connect failed to complete", exc);
			}
		}
	}

	@Override
	public synchronized void sendRequest(HttpRequest request, boolean wantResponse) throws IOException {
		if (!(request instanceof ClassicHttpRequest)) {
			throw new IllegalArgumentException(
					"request must be an instance of org.apache.hc.core5.http.ClassicHttpRequest");
		}

		try {
			if (!wantResponse) {
				request.setHeader(HttpHeaders.PRAGMA, PRAGMA_VALUE_NO_RESPONSE);
			}
			if (StringUtils.isNotEmpty(proxyCredentials)) {
				request.setHeader(HttpHeaders.PROXY_AUTHORIZATION, "Basic " + proxyCredentials);
			}
			if (request.getVersion() == null || request.getVersion().greaterEquals(HttpVersion.HTTP_1_1)) {
				request.setHeader(HttpHeaders.HOST, host + ":" + port);
			}
			logger.log(OpLevel.TRACE, "Sending to {0}: {1}", uri, request);
			ClassicHttpRequest httpRequest = (ClassicHttpRequest) request;

			checkState(request.getHeaderStr(X_API_KEY));

			connection.sendRequestHeader(httpRequest);
			if (request.hasContent()) {
				connection.sendRequestEntity(httpRequest);
			}
			connection.flush();
		} catch (HttpException he) {
			throw new IOException(he.getMessage(), he);
		}
	}

	@Override
	public void sendRequest(String token, String method, String reqUri, String contentType, String content,
			boolean wantResponse) throws IOException {
		try {
			HttpRequest req = newRequest(method, reqUri);
			if (!StringUtils.isEmpty(content)) {
				req.setContent(contentType, content, Utils.UTF8);
			}
			if (StringUtils.isNotEmpty(token)) {
				req.setHeader(X_API_KEY, token);
			}
			sendRequest(req, wantResponse);
		} catch (IllegalStateException ise) {
			close();
			throw ise;
		} catch (IOException ioe) {
			close();
			throw ioe;
		}
	}

	@Override
	public void send(String token, String msg, boolean wantResponse) throws IOException {
		String contentType = (msg != null && msg.startsWith("<?xml") ? "text/xml" : "text/plain");
		sendRequest(token, "POST", uriPath, contentType, msg, wantResponse);
	}

	@Override
	public synchronized HttpResponse getResponse() throws IOException {
		HttpResponseImpl resp = null;
		try {
			checkState();

			resp = new HttpResponseImpl(connection.receiveResponseHeader());
			String contentLenStr = resp.getHeaderStr(HttpHeaders.CONTENT_LENGTH);
			String contentType = resp.getHeaderStr(HttpHeaders.CONTENT_TYPE);
			int contentLen = (StringUtils.isEmpty(contentLenStr) ? 0 : Integer.parseInt(contentLenStr));
			if (contentLen > 0 || !StringUtils.isEmpty(contentType)) {
				connection.receiveResponseEntity(resp);
			}
			return resp;
		} catch (Throwable ex) {
			if (resp != null) {
				EntityUtils.consumeQuietly(resp.getEntity());
				Utils.close(resp);
			}
			throw new IOException(ex.getMessage(), ex);
		}
	}

	@Override
	public synchronized String read() throws IOException, ParseException {
		HttpResponseImpl resp = null;
		String content = "";
		try {
			resp = (HttpResponseImpl) getResponse();
			content = resp.getContentString();
			int status = resp.getStatus();

			logger.log(OpLevel.TRACE, "Received response from={0}: code={1}, msg={2}", uri, status, content);
			if (status >= HttpStatus.SC_CLIENT_ERROR) {
				if (AccessResponse.isAccessResponse(content)) {
					_close();
					AccessResponse accessResp = AccessResponse.parseMsg(content);
					String reason = accessResp.getReason();
					if (StringUtils.isEmpty(reason)) {
						reason = "Access Denied";
					}
					throw new SecurityException(reason);
				} else {
					throw new HttpRequestException(status, content);
				}
			}
		} finally {
			if (resp != null) {
				EntityUtils.consumeQuietly(resp.getEntity());
				Utils.close(resp);
			}
		}

		return content;
	}

	protected synchronized void _close() {
		if (connection == null) {
			return;
		}

		// Utils.close(connection);
		Utils.close(connMgr);
		logger.log(OpLevel.DEBUG, "Closed connection to {0}", uri);

		connection = null;
	}

	@Override
	public synchronized void close() {
		if (connection == null) {
			return;
		}

		ensureAllRequestsSent();
		_close();
	}

	/**
	 * Ensures that all connection (socket output stream buffer) contained data has been sent by issuing PING message
	 * and waiting for response.
	 */
	protected void ensureAllRequestsSent() {
		try {
			logger.log(OpLevel.DEBUG, "Ping {0}: ensure all requests have been sent to {1}", this, uri);
			HttpRequest pingReq = newDefaultRequest();
			pingReq.setHeader(HttpHeaders.PRAGMA, PRAGMA_VALUE_PING);
			sendRequest(pingReq, true);
			HttpResponseImpl pingResp = null;
			try {
				pingResp = (HttpResponseImpl) getResponse();
				logger.log(OpLevel.DEBUG, "Ping {0}: response received from {1}, response={2}", this, uri, pingResp);
			} finally {
				if (pingResp != null) {
					EntityUtils.consumeQuietly(pingResp.getEntity());
					Utils.close(pingResp);
				}
			}
		} catch (Throwable exc) {
		}
	}

	@Override
	public String getHost() {
		return (httpHost != null ? httpHost.getHostName() : null);
	}

	@Override
	public int getPort() {
		return (httpHost != null ? httpHost.getPort() : 0);
	}

	@Override
	public String getProxyHost() {
		return (httpProxy != null ? httpProxy.getHostName() : null);
	}

	@Override
	public int getProxyPort() {
		return (httpProxy != null ? httpProxy.getPort() : 0);
	}

	@Override
	public boolean isSecure() {
		return (httpHost != null && SCHEME_HTTPS.equals(httpHost.getSchemeName()));
	}

	@Override
	public boolean isConnected() {
		return (connection != null && connection.isOpen());
	}

	@Override
	public String toString() {
		return httpHost.toString();
	}

	@Override
	public HttpRequest newRequest(String method, String uri) {
		if (StringUtils.isEmpty(uri)) {
			uri = "/";
		}

		HttpRequestImpl req = new HttpRequestImpl(method, uri);
		req.setVersion(HttpVersion.HTTP_1_1);

		return req;
	}

	private HttpRequest newDefaultRequest() {
		return newRequest("POST", uriPath);
	}

	@Override
	public HttpResponse newResponse(String protocol, int major, int minor, int status) {
		return new HttpResponseImpl(new ProtocolVersion(protocol, major, minor), status);
	}

	@Override
	public URI getURI() {
		return uri;
	}

	/**
	 * Obtain HTTP connection timeout value in milliseconds.
	 *
	 * @return connection timeout value in milliseconds
	 */
	public long getConnTimeout() {
		return connTimeout;
	}

	/**
	 * Sets HTTP connection timeout value in milliseconds.
	 * 
	 * @param connTimeout
	 *            connection timeout value in milliseconds
	 */
	public void setConnTimeout(long connTimeout) {
		this.connTimeout = connTimeout;
	}
}
