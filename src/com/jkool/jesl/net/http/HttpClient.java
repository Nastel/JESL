/*
 * Copyright 2015 Nastel Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jkool.jesl.net.http;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.ProtocolVersion;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.RouteInfo;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.params.BasicHttpParams;

import com.jkool.jesl.net.http.apache.HttpRequestImpl;
import com.jkool.jesl.net.http.apache.HttpResponseImpl;
import com.jkool.jesl.net.security.AccessResponse;
import com.jkool.jesl.net.security.AuthUtils;
import com.jkool.jesl.net.ssl.SSLContextFactory;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * Establishes an HTTP[S] connection to the specified server based on given URL.
 *
 * @version $Revision: 3 $
 */
public class HttpClient implements HttpStream {
	public static final String NO_RESPONSE_REQUIRED_HEADER = "Pragma";
	public static final String NO_RESPONSE_REQUIRED_VALUE  = "no-response";

	protected URI							uri;
	protected EventSink		 			    logger;
	protected String						host;
	protected int							port;
	protected String						uriPath;
	protected String						sslKeystore;
	protected String						sslKeystorePwd;
	protected HttpHost						httpHost;
	protected HttpHost						httpProxy;
	protected BasicClientConnectionManager	connMgr;
	protected ClientConnectionRequest		connReq;
	protected ManagedClientConnection		connection;
	protected HttpRoute						route;
	protected SchemeRegistry				schemeReg;
	protected boolean						secure = false;

	public HttpClient(String urlStr, EventSink logger) throws URISyntaxException {
		this(urlStr, null, 0, logger);
	}

	public HttpClient(String urlStr, String proxyHost, int proxyPort, EventSink logger) throws URISyntaxException {
		uri = new URI(urlStr);
		String scheme = uri.getScheme();
		secure = "https".equalsIgnoreCase(scheme);
		host = uri.getHost();
	    if (host == null)
	    	host = "localhost";
	    port = uri.getPort();
	    if (port <= 0)
	    	port = (secure ? 443 : 80);
		init(host, port, uri.getPath(), secure, proxyHost, proxyPort, logger);
	}

	/**
	 *
	 */
	private void init(String host, int port, String uriPath, boolean secure,
					  String proxyHost, int proxyPort, EventSink logger) {
		this.host    = host;
		this.port    = port;
		this.uriPath = uriPath;
		this.secure  = secure;
		this.logger  = (logger != null ? logger : DefaultEventSinkFactory.defaultEventSink(HttpClient.class));

		String scheme = (secure ? "https" : "http");

	    httpHost = new HttpHost(host, port, scheme);
		if (!StringUtils.isEmpty(proxyHost))
			httpProxy = new HttpHost(proxyHost, proxyPort);
	}

	/**
	 * Sets keystore and password for obtaining SSL certificate for server.
	 *
	 * @param sslKeystore path to keystore
	 * @param sslKeystorePwd password for keystore
	 */
	public void setSslKeystore(String sslKeystore, String sslKeystorePwd) {
		this.sslKeystore    = sslKeystore;
		this.sslKeystorePwd = sslKeystorePwd;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void connect() throws IOException {
		if (logger.isSet(OpLevel.DEBUG))
			logger.log(OpLevel.DEBUG, "Connecting to {0}{1}", httpHost, (httpProxy != null ? " via proxy " + httpProxy : ""));

		try {
		    if (secure) {
		    	SSLSocketFactory ssf = null;
		    	if (!StringUtils.isEmpty(sslKeystore)) {
			    	SSLContextFactory scf = new SSLContextFactory(sslKeystore, sslKeystorePwd, sslKeystorePwd);
			    	ssf = new SSLSocketFactory(scf.getSslContext(true));
		    	}
		    	else {
		    		ssf = new SSLSocketFactory(SSLContext.getDefault());
		    	}

		    	Scheme secureScheme = new Scheme("https", port, ssf);
		    	schemeReg = new SchemeRegistry();
		    	schemeReg.register(secureScheme);
		    }

			route = new HttpRoute(httpHost, null, httpProxy, secure, RouteInfo.TunnelType.PLAIN, RouteInfo.LayerType.PLAIN);

			if (schemeReg != null)
				connMgr = new BasicClientConnectionManager(schemeReg);
			else
				connMgr = new BasicClientConnectionManager();

			connReq = connMgr.requestConnection(route, null);
			connection = connReq.getConnection(0, null);
			connection.open(route, null, new BasicHttpParams());
		}
		catch (Exception e) {
			close();
			throw new IOException("Failed to connect to " + httpHost, e);
		}
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void connect(String token) throws IOException {
		connect();
		if (!StringUtils.isEmpty(token)) {
			if (logger.isSet(OpLevel.DEBUG))
				logger.log(OpLevel.DEBUG, "Authenticating connection={0} with token='{1}'", this, token);
			AuthUtils.authenticate(this, token);
		}
	}

	@Override
	public synchronized void sendRequest(HttpRequest request, boolean wantResponse) throws IOException {
		if (!(request instanceof org.apache.http.HttpRequest))
			throw new IllegalArgumentException("request must be an instance of org.apache.http.HttpRequest");

		if (!wantResponse)
			request.setHeader(NO_RESPONSE_REQUIRED_HEADER, NO_RESPONSE_REQUIRED_VALUE);

		if (logger.isSet(OpLevel.TRACE))
			logger.log(OpLevel.TRACE, "Sending to {0}: {1}", httpHost, request);

		try {
			org.apache.http.HttpRequest httpRequest = (org.apache.http.HttpRequest) request;
			connection.sendRequestHeader(httpRequest);
			if (httpRequest instanceof HttpEntityEnclosingRequest && request.hasContent())
				connection.sendRequestEntity((HttpEntityEnclosingRequest)httpRequest);
			connection.flush();
		} catch (HttpException he) {
			throw new IOException(he.getMessage(), he);
		}
	}

	@Override
	public void sendRequest(String method, String reqUri, String contentType, String content, boolean wantResponse) throws IOException {
		if (StringUtils.isEmpty(reqUri))
			reqUri = "/";

		HttpRequest req = newRequest(method, reqUri);
		if (!StringUtils.isEmpty(content))
			req.setContent(contentType, content, "UTF-8");

		try {
			sendRequest(req, wantResponse);
		}
		catch (IllegalStateException ise) {
			close();
			throw ise;
		}
		catch (IOException ioe) {
			close();
			throw ioe;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendMessage(String msg, boolean wantResponse) throws IOException {
		String contentType = (msg != null && msg.startsWith("<?xml") ? "text/xml" : "text/plain");
		sendRequest("POST", uriPath, contentType, msg, wantResponse);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendRequest(String msg, boolean wantResponse) throws IOException {
		sendMessage(msg, wantResponse);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized HttpResponse getResponse() throws IOException {
		try {
			HttpResponseImpl resp = new HttpResponseImpl(connection.receiveResponseHeader());
			String contentLenStr = resp.getHeader(HttpHeaders.CONTENT_LENGTH);
			String contentType   = resp.getHeader(HttpHeaders.CONTENT_TYPE);
			int contentLen = (StringUtils.isEmpty(contentLenStr) ? 0 : Integer.parseInt(contentLenStr));
			if (contentLen > 0 || !StringUtils.isEmpty(contentType))
				connection.receiveResponseEntity(resp);

			return resp;
		} catch (Throwable ex) {
			throw new IOException(ex.getMessage(), ex);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized String getReply() throws IOException {
		HttpResponse resp = null;
		resp = getResponse();
		String content = resp.getContentString();

		if (logger.isSet(OpLevel.TRACE))
			logger.log(OpLevel.TRACE, "Received response from {0}: {1}", httpHost, content);

		int status = resp.getStatus();

		if (status >= 400) {
			if (AccessResponse.isAccessResponse(content)) {
				close();
				AccessResponse accessResp = AccessResponse.parseMsg(content);
				String reason = accessResp.getReason();
				if (StringUtils.isEmpty(reason))
					reason = "Access Denied";
				throw new SecurityException(reason);
			}
			else {
				throw new RequestFailedException(String.valueOf(status), content);
			}
		}
		return content;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() {
		if (connection != null) {
			if (logger.isSet(OpLevel.DEBUG))
				logger.log(OpLevel.DEBUG, "Closing connection to {0}", httpHost);

			connMgr.releaseConnection(connection, 0, TimeUnit.MILLISECONDS);
			connMgr.closeIdleConnections(0, TimeUnit.MILLISECONDS);
			connMgr.shutdown();

			connReq    = null;
			connection = null;
			connMgr    = null;
			route      = null;
			schemeReg  = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getHost() {
		return (httpHost != null ? httpHost.getHostName() : null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getPort() {
		return (httpHost != null ? httpHost.getPort() : 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getProxyHost() {
		return (httpProxy != null ? httpProxy.getHostName() : null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getProxyPort() {
		return (httpProxy != null ? httpProxy.getPort() : 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isSecure() {
		return (httpHost != null && "https".equals(httpHost.getSchemeName()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isConnected() {
		return (connection != null && connection.isOpen());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return httpHost.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HttpRequest newRequest(String method, String uri) {
		return new HttpRequestImpl(method, uri);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HttpResponse newResponse(String protocol, int major, int minor, int status) {
		return new HttpResponseImpl(new ProtocolVersion(protocol, major, minor), status);
	}

	@Override
    public URI getURI() {
	    return uri;
    }
}
