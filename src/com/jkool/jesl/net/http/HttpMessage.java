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
package com.jkool.jesl.net.http;

import java.io.IOException;

/**
 * This interface defines JESL HTTP message.
 *
 * @version $Revision: 1 $
 */
public interface HttpMessage {
	/**
	 * Get HTTP header field from HTTP header
	 *
	 * @param name field name
	 * @return value associated with the header field name
	 */
	String getHeader(String name);

	/**
	 * Add HTTP header field to HTTP header
	 *
	 * @param name field name
	 * @param value value associated with the field name
	 */
	void addHeader(String name, String value);

	/**
	 * Set/replace HTTP header field/value
	 *
	 * @param name field name
	 * @param value value associated with the field name
	 */
	void setHeader(String name, String value);

	/**
	 * Remove HTTP header field
	 *
	 * @param name field name
	 */
	void removeHeader(String name);

	/**
	 * True if HTTP message has content
	 *
	 * @return true if HTTP message has content, false otherwise
	 */
	boolean hasContent();

	/**
	 * Obtain content from HTTP message
	 *
	 * @return bytes associated with HTTP content
	 * @throws IOException if error reading message content
	 */
	byte[] getContentBytes() throws IOException;

	/**
	 * Obtain content from HTTP message
	 *
	 * @return string message associated with HTTP content
	 * @throws IOException if error reading message content
	 */
	String getContentString() throws IOException;

	/**
	 * Obtain content from HTTP message with a given
	 * character set
	 *
	 * @param charset character set
	 * @return string message associated with HTTP content
	 * @throws IOException if error reading message content
	 */
	String getContentString(String charset) throws IOException;

	/**
	 * Set HTTP message content
	 *
	 * @param contentType contentType
	 * @param content string content
	 * @throws IOException if error writing message content
	 */
	void setContent(String contentType, String content) throws IOException;

	/**
	 * Set HTTP message content
	 *
	 * @param contentType contentType
	 * @param content content bytes
	 * @param contentEncoding encoding used for specified byte message
	 * @throws IOException if error writing message content
	 */
	void setContent(String contentType, byte[] content, String contentEncoding) throws IOException;

	/**
	 * Set HTTP message content
	 *
	 * @param contentType contentType
	 * @param content content string
	 * @param charset character set that represents string message
	 * @throws IOException if error writing message content
	 */
	void setContent(String contentType, String content, String charset) throws IOException;
}
