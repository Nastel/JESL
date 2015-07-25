/*
 * Copyright 2015 JKOOL, LLC.
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
package com.jkool.jesl.net.socket;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.lang3.StringUtils;

import com.jkool.jesl.net.JKStream;
import com.jkool.jesl.net.security.AuthUtils;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * This class provides TCP/SSL connection to the specified JESK server based on given URL.
 * tcp[s]://host:port
 *
 * @version $Revision: 3 $
 */
public class SocketClient implements JKStream {
	protected EventSink logger;

	protected InetSocketAddress proxyAddr;
	protected Proxy proxy = Proxy.NO_PROXY;	// default to direct connection

	protected String			host;
	protected int				port;
	protected boolean			secure;
	protected Socket			socket;
	protected DataOutputStream	out;
	protected BufferedReader	in;


	/**
	 * Create JESL HTTP[S} client stream with given attributes
	 * 
	 * @param host JESL host server
	 * @param port JESL host port number
	 * @param secure use SSL if secure, standard sockets if false
	 * @param logger event sink used for logging, null if none
	 * 
	 */
	public SocketClient(String host, int port, boolean secure, EventSink logger) {
		this.host   = host;
		this.port   = port;
		this.secure = secure;
		this.logger = (logger != null ? logger : DefaultEventSinkFactory.defaultEventSink(SocketClient.class));
	}

	/**
	 * Create JESL HTTP[S} client stream with given attributes
	 * 
	 * @param host JESL host server
	 * @param port JESL host port number
	 * @param secure use SSL if secure, standard sockets if false
	 * @param proxyHost proxy host name if any, null if none
	 * @param proxyPort proxy port number if any, 0 of none
	 * @param logger event sink used for logging, null if none
	 * 
	 */
	public SocketClient(String host, int port, boolean secure, String proxyHost, int proxyPort, EventSink logger) {
		this(host, port, secure, logger);
		if (!StringUtils.isEmpty(proxyHost)) {
			proxyAddr = new InetSocketAddress(proxyHost, proxyPort);
			proxy = new Proxy(Proxy.Type.SOCKS, proxyAddr);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void connect() throws IOException {
		if (isConnected()) return;
		if (secure) {
			SocketFactory socketFactory = SSLSocketFactory.getDefault();
			socket = socketFactory.createSocket(host, port);
		}
		else {
			socket = new Socket(host, port);
		}
		out = new DataOutputStream(socket.getOutputStream());
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void connect(String token) throws IOException {
		connect();
		if (!StringUtils.isEmpty(token)) {
			logger.log(OpLevel.DEBUG, "Authenticating connection={0} with token={1}", this, token);
			AuthUtils.authenticate(this, token);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void send(String msg, boolean wantResponse) throws IOException {
		if (wantResponse)
			throw new UnsupportedOperationException("Responses are not supported for TCP connections");
		
		String lineMsg = msg.endsWith("\n")? msg: msg + "\n";
		byte [] bytes = lineMsg.getBytes();
		out.write(bytes, 0, bytes.length);
		out.flush();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void close() {
		try {
			if (socket != null) {
				if (out != null) out.close();
				if (in  != null) in.close();
				socket.close();
			}
		}
		catch (IOException e) {}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getHost() {
		return host;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getPort() {
		return port;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isSecure() {
		return secure;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getProxyHost() {
		return (proxyAddr != null ? proxyAddr.getHostName() : null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getProxyPort() {
		return (proxyAddr != null ? proxyAddr.getPort() : 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isConnected() {
		return (socket != null && socket.isConnected());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized String read() throws IOException {
		if (socket == null) connect();
		return in.readLine();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "tcp" + (isSecure() ? "s" : "") + "://" + host + ":" + port;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public URI getURI() { 
		try {
			return new URI("tcp" + (isSecure() ? "s" : "") + "://" + host + ":" + port);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
    }
}