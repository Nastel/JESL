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
package com.jkool.jesl.net.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.lang3.StringUtils;

import com.jkool.jesl.net.security.AuthUtils;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 *
 *
 * @version $Revision: 2 $
 */
public class SocketClient implements SocketConnection {
	protected EventSink logger;

	protected InetSocketAddress proxyAddr;
	protected Proxy proxy = Proxy.NO_PROXY;	// default to direct connection

	protected String			host;
	protected int				port;
	protected boolean			secure;
	protected Socket			socket;
	protected PrintWriter		out;
	protected BufferedReader	in;

	public SocketClient(String host, int port, boolean secure, EventSink logger) {
		this.host   = host;
		this.port   = port;
		this.secure = secure;
		this.logger = (logger != null ? logger : DefaultEventSinkFactory.defaultEventSink(SocketClient.class));
	}

	public SocketClient(String host, int port, boolean secure, String proxyHost, int proxyPort, EventSink logger) {
		this(host, port, secure, logger);
		if (!StringUtils.isEmpty(proxyHost)) {
			proxyAddr = new InetSocketAddress(proxyHost, proxyPort);
			proxy = new Proxy(Proxy.Type.SOCKS, proxyAddr);
		}
	}

	@Override
	public void connect() throws IOException {
		if (secure) {
			SocketFactory socketFactory = SSLSocketFactory.getDefault();
			socket = socketFactory.createSocket(host, port);
		}
		else {
			socket = new Socket(host, port);
		}
		out = new PrintWriter(socket.getOutputStream(), false);
	}

	@Override
	public void connect(String token) throws Throwable {
		connect();
		if (token != null && !StringUtils.isEmpty(token)) {
			AuthUtils.authenticate(this, token);
		}
	}

	@Override
	public void sendMessage(String msg, boolean wantResponse) throws Throwable {
		if (wantResponse)
			throw new UnsupportedOperationException("Responses are not supported for TCP connections");

		if (!isConnected())
			connect();

		out.println(msg);
		out.flush();
		if (out.checkError()) {
			close();
			throw new IOException("Unknown error writing data to socket, closing");
		}
	}

	@Override
	public void sendRequest(String msg, boolean wantResponse) throws Throwable {
		sendMessage(msg, wantResponse);
	}

	@Override
	public void close() {
		try {
			if (socket != null) {
				if (out != null) out.close();
				if (in  != null) in.close();
				socket.close();
			}
		}
		catch (IOException e) {}
		out    = null;
		in     = null;
		socket = null;
	}

	@Override
	public String getHost() {
		return host;
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public boolean isSecure() {
		return secure;
	}

	@Override
	public String getProxyHost() {
		return (proxyAddr != null ? proxyAddr.getHostName() : null);
	}

	@Override
	public int getProxyPort() {
		return (proxyAddr != null ? proxyAddr.getPort() : 0);
	}

	@Override
	public boolean isConnected() {
		return (socket != null && out != null);
	}

	@Override
	public String getReply() throws Throwable {
		if (socket == null)
			connect();

		if (in == null)
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

		return in.readLine();
	}

	@Override
	public String toString() {
		return "tcp" + (isSecure() ? "s" : "") + "://" + host + ":" + port;
	}
}
