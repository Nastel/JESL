/*
 * Copyright 2015-2018 JKOOL, LLC.
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
package com.jkoolcloud.jesl.net.http;

import java.io.IOException;

/**
 * This exception encapsulates HTTP errors, status codes.
 *
 * @version $Revision: 1 $
 */
public class HttpRequestException extends IOException {
	private static final long serialVersionUID = 1827966640612146841L;

	protected int status;

	public HttpRequestException(int status, String message) {
		super(message);
		this.status = status;
	}

	public HttpRequestException(int status, String message, Throwable cause) {
		super(message, cause);
		this.status = status;
	}

	/**
	 * Get HTTP status code
	 * 
	 * @return HTTP status code
	 */
	public int getStatus() {
		return status;
	}

	@Override
	public String toString() {
		return this.getClass().getName() + ": " + getStatus() + " - " + getMessage();
	}
}
