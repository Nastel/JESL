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
package com.jkool.jesl.net.security;

import java.io.StringReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * JESL access request implementation, which encapsulates JESL
 * authentication request message.
 *
 * @version $Revision: 1 $
 */
public class AccessRequest {
	private static final String ROOT_TAG  = "access-request";
	private static final String TOKEN_TAG = "token";

	private static final String ROOT_ELMT = "<" + ROOT_TAG + ">";

	private String	token;

	/**
	 * Create access request with a given access token
	 * 
	 * @param token access token
	 */
	public AccessRequest(String token) {
		this.token = token;
	}

	/**
	 * Get access token associated with this request
	 * 
	 * @return access token associated with this request
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Generate access request message
	 * 
	 */
	public String generateMsg() {
		StringBuilder msg = new StringBuilder();

		msg.append("<").append(ROOT_TAG).append(">");
		msg.append("<").append(TOKEN_TAG).append(">").append(token).append("</").append(TOKEN_TAG).append(">");
		msg.append("</").append(ROOT_TAG).append(">");

		return msg.toString();
	}

	/**
	 * Parse and create access request from a given string
	 * 
	 * @param msg access request message
	 * @return access request object instance
	 */
	public static AccessRequest parseMsg(String msg) {
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			AccessRequestParserHandler handler = new AccessRequestParserHandler();
			parser.parse(new InputSource(new StringReader(msg)), handler);
			return handler.req;
		}
		catch (Exception e) {
			throw new SecurityException("Failed to create AccessRequest from message", e);
		}
	}

	/**
	 * Is a given string message an access request message
	 * 
	 * @param msg access request message
	 * @return true if given string is an access request message, false otherwise
	 */
	public static boolean isAccessRequest(String msg) {
		return (msg != null &&
				msg.length() > ROOT_ELMT.length() &&
				msg.startsWith(ROOT_ELMT, 0));
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected static class AccessRequestParserHandler extends DefaultHandler {
		public    AccessRequest	req;
		protected String		token;
		protected StringBuilder	elmtValue = new StringBuilder();

		@Override
		public void endDocument() throws SAXException {
			req = new AccessRequest(token);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			elmtValue.setLength(0);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (TOKEN_TAG.equals(qName))
				token = elmtValue.toString();
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			elmtValue.append(ch, start, length);
		}
	}
}
