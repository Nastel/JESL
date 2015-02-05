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
 *
 *
 * @version $Revision: 1 $
 */
public class LoginResponse {
	private static final String ROOT_TAG    = "login-resp";
	private static final String USER_TAG    = "user";
	private static final String SUCCESS_TAG = "success";
	private static final String REASON_TAG  = "reason";

	private static final String ROOT_ELMT = "<" + ROOT_TAG + ">";

	private String  user;
	private boolean success;
	private String  reason;

	public LoginResponse(String user, boolean success) {
		this.user    = user;
		this.success = success;
	}

	public LoginResponse(String user, boolean success, String reason) {
		this.user    = user;
		this.success = success;
		this.reason  = reason;
	}

	public String getUser() {
		return user;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getReason() {
		return reason;
	}

	public String generateMsg() {
		StringBuilder msg = new StringBuilder();

		msg.append("<").append(ROOT_TAG).append(">");
		msg.append("<").append(USER_TAG).append(">").append(user).append("</").append(USER_TAG).append(">");
		msg.append("<").append(SUCCESS_TAG).append(">").append(success).append("</").append(SUCCESS_TAG).append(">");

		if (reason != null)
			msg.append("<").append(REASON_TAG).append(">").append(reason).append("</").append(REASON_TAG).append(">");

		msg.append("</").append(ROOT_TAG).append(">");

		return msg.toString();
	}

	public static LoginResponse parseMsg(String msg) {
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			LoginResponseParserHandler handler = new LoginResponseParserHandler();
			parser.parse(new InputSource(new StringReader(msg)), handler);
			return handler.resp;
		}
		catch (Exception e) {
			throw new SecurityException("Failed to create LoginResponse from message", e);
		}
	}

	public static boolean isLoginResponse(String msg) {
		return (msg != null &&
				msg.length() > ROOT_ELMT.length() &&
				msg.startsWith(ROOT_ELMT, 0));
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected static class LoginResponseParserHandler extends DefaultHandler {
		public    LoginResponse	resp;
		protected String		user;
		protected boolean		success;
		protected String		reason;
		protected StringBuilder	elmtValue = new StringBuilder();

		@Override
		public void endDocument() throws SAXException {
			if (reason != null)
				resp = new LoginResponse(user, success, reason);
			else
				resp = new LoginResponse(user, success);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			elmtValue.setLength(0);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (USER_TAG.equals(qName))
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
