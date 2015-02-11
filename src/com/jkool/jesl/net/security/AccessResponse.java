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
 * JESL access response implementation, which encapsulates JESL
 * authentication response message.
 *
 * @version $Revision: 1 $
 */
public class AccessResponse {
	private static final String ROOT_TAG    = "access-resp";
	private static final String TOKEN_TAG   = "token";
	private static final String SUCCESS_TAG = "success";
	private static final String REASON_TAG  = "reason";

	private static final String ROOT_ELMT = "<" + ROOT_TAG + ">";

	private String  token;
	private boolean success;
	private String  reason;

	/**
	 * Create access response with a given access token and status
	 * 
	 * @param token access token
	 * @param success flag
	 */
	public AccessResponse(String token, boolean success) {
		this.token   = token;
		this.success = success;
	}

	/**
	 * Create access response with a given access token and status
	 * 
	 * @param token access token
	 * @param success flag
	 * @param reason success flag reason
	 */
	public AccessResponse(String token, boolean success, String reason) {
		this.token   = token;
		this.success = success;
		this.reason  = reason;
	}

	/**
	 * Get user name associated with the response
	 * 
	 * @return user name associated with the response
	 */
	public String getUser() {
		return token;
	}

	/**
	 * Is access response signify success
	 * 
	 * @return true if success, false otherwise
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * Get success flag reason message
	 * 
	 * @return success flag reason message
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * Generate access response message
	 * 
	 */
	public String generateMsg() {
		StringBuilder msg = new StringBuilder();

		msg.append("<").append(ROOT_TAG).append(">");
		msg.append("<").append(TOKEN_TAG).append(">").append(token).append("</").append(TOKEN_TAG).append(">");
		msg.append("<").append(SUCCESS_TAG).append(">").append(success).append("</").append(SUCCESS_TAG).append(">");

		if (reason != null)
			msg.append("<").append(REASON_TAG).append(">").append(reason).append("</").append(REASON_TAG).append(">");

		msg.append("</").append(ROOT_TAG).append(">");

		return msg.toString();
	}

	/**
	 * Parse and create access response from a given string
	 * 
	 * @param msg access response message
	 * @return access response object instance
	 */
	public static AccessResponse parseMsg(String msg) {
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			AccessResponseParserHandler handler = new AccessResponseParserHandler();
			parser.parse(new InputSource(new StringReader(msg)), handler);
			return handler.resp;
		}
		catch (Exception e) {
			throw new SecurityException("Failed to create AccessResponse from message", e);
		}
	}

	/**
	 * Is a given string message an access response message
	 * 
	 * @param msg access response message
	 * @return true if given string is an access response message, false otherwise
	 */
	public static boolean isAccessResponse(String msg) {
		return (msg != null &&
				msg.length() > ROOT_ELMT.length() &&
				msg.startsWith(ROOT_ELMT, 0));
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected static class AccessResponseParserHandler extends DefaultHandler {
		public    AccessResponse	resp;
		protected String			user;
		protected boolean			success;
		protected String			reason;
		protected StringBuilder		elmtValue = new StringBuilder();

		@Override
		public void endDocument() throws SAXException {
			if (reason != null)
				resp = new AccessResponse(user, success, reason);
			else
				resp = new AccessResponse(user, success);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			elmtValue.setLength(0);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (TOKEN_TAG.equals(qName))
				user = elmtValue.toString();
			else if (SUCCESS_TAG.equals(qName))
				success = Boolean.parseBoolean(elmtValue.toString());
			else if (REASON_TAG.equals(qName))
				reason = elmtValue.toString();
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			elmtValue.append(ch, start, length);
		}
	}
}
