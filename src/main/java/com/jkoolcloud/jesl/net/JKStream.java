/*
 * Copyright 2015-2019 JKOOL, LLC.
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
package com.jkoolcloud.jesl.net;

import java.io.IOException;
import java.net.URI;

/**
 * JESL Stream interface defines a way connect and interact with JESL streams. JESL stream implementation must implement
 * this interface.
 *
 * @version $Revision: 1 $
 */
public interface JKStream {
	/**
	 * Obtain {@link java.net.URI} associated with this stream.
	 *
	 * @return {@link java.net.URI} associated with this stream.
	 */
	URI getURI();

	/**
	 * Obtain host name associated with the stream
	 *
	 * @return Obtain host name associated with the stream
	 */
	String getHost();

	/**
	 * Obtain port number associated with the stream
	 *
	 * @return port number associated with the stream
	 */
	int getPort();

	/**
	 * True if stream is via a secure protocol (e.g. SSL), false otherwise
	 *
	 * @return true if stream is via a secure protocol (e.g. SSL), false otherwise
	 */
	boolean isSecure();

	/**
	 * Obtain proxy host name associated with the stream
	 *
	 * @return Obtain proxy host name associated with the stream, null if not defined
	 */
	String getProxyHost();

	/**
	 * Obtain proxy port number associated with the stream
	 *
	 * @return proxy port number associated with the stream, 0 if non defined
	 */
	int getProxyPort();

	/**
	 * Connect the stream to the underlying URI connection
	 *
	 * @throws IOException
	 *             if error establishing connection
	 */
	void connect() throws IOException;

	/**
	 * Connect the stream to the underlying URI connection
	 *
	 * @param token
	 *            access token (security token)
	 * @throws IOException
	 *             if error establishing connection
	 */
	void connect(String token) throws IOException;

	/**
	 * True if stream connected, false otherwise
	 *
	 * @return true if stream connected, false otherwise
	 */
	boolean isConnected();

	/**
	 * Stream message to the underlying stream. Message must end with new line '\n'.
	 *
	 * @param token
	 *            API access token
	 * @param msg
	 *            new line terminated message
	 * @param wantResponse
	 *            request response back
	 * @throws IOException
	 *             if error occurs when sending a message
	 */
	void send(String token, String msg, boolean wantResponse) throws IOException;

	/**
	 * Read a message (reply) from the stream.
	 *
	 * @return a message from the stream.
	 * @throws IOException
	 *             if error occurs when sending a message
	 */
	String read() throws IOException;

	/**
	 * Close the stream and release all resources
	 */
	void close();
}
