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

/**
 * This interface defines JESL HTTP message. 
 *
 * @version $Revision: 1 $
 */
public interface HttpMessage {
	String getHeader(String name);

	void addHeader(String name, String value);
	void setHeader(String name, String value);
	void removeHeader(String name);

	boolean hasContent();
	byte[] getContentBytes() throws IOException;
	String getContentString() throws IOException;
	String getContentString(String charset) throws IOException;

	void setContent(String contentType, String content) throws IOException;
	void setContent(String contentType, byte[] content, String contentEncoding) throws IOException;
	void setContent(String contentType, String content, String charset) throws IOException;
}
