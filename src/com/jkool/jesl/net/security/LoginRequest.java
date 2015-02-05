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
import java.security.GeneralSecurityException;

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
public class LoginRequest {
	private static final String ROOT_TAG     = "login-request";
	private static final String USER_TAG     = "user";
	private static final String PWD_TAG      = "password";
	private static final String ENCRYPT_ATTR = "encrypted";

	private static final String ROOT_ELMT = "<" + ROOT_TAG + ">";

	private String	user;
	private String	password;
	private boolean	encryptPwd;

	// AES encryption key
	private static byte[] aesKey = {0x31, 0x33, 0x35, 0x37, 0x39, 0x3B, 0x3D, 0x3F,
								    0x71, 0x73, 0x75, 0x77, 0x79, 0x7B, 0x7D, 0x7F};

	public LoginRequest(String user, String password, boolean encryptPwd) {
		this.user       = user;
		this.password   = password;
		this.encryptPwd = encryptPwd;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	public String generateMsg() throws GeneralSecurityException {
		StringBuilder msg = new StringBuilder();

		msg.append("<").append(ROOT_TAG).append(">");
		msg.append("<").append(USER_TAG).append(">").append(user).append("</").append(USER_TAG).append(">");
		msg.append("<").append(PWD_TAG).append(" ").append(ENCRYPT_ATTR).append("=\"").append(encryptPwd).append("\">");
		if (encryptPwd)
			msg.append(AesCipher.encrypt(password, aesKey));
		else
			msg.append(password);
		msg.append("</").append(PWD_TAG).append(">");
		msg.append("</").append(ROOT_TAG).append(">");

		return msg.toString();
	}

	public static LoginRequest parseMsg(String msg) {
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			LoginRequestParserHandler handler = new LoginRequestParserHandler();
			parser.parse(new InputSource(new StringReader(msg)), handler);
			return handler.req;
		}
		catch (Exception e) {
			throw new SecurityException("Failed to create LoginRequest from message", e);
		}
	}

	public static boolean isLoginRequest(String msg) {
		return (msg != null &&
				msg.length() > ROOT_ELMT.length() &&
				msg.startsWith(ROOT_ELMT, 0));
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected static class LoginRequestParserHandler extends DefaultHandler {
		public    LoginRequest	req;
		protected String		user;
		protected String		pwd;
		protected boolean		encrypted = false;
		protected StringBuilder	elmtValue = new StringBuilder();

		@Override
		public void endDocument() throws SAXException {
			if (encrypted) {
				try {
					pwd = AesCipher.decrypt(pwd, aesKey);
				}
				catch (GeneralSecurityException e) {
					throw new SAXException("Could not process login request", e);
				}
			}
			req = new LoginRequest(user, pwd, encrypted);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			elmtValue.setLength(0);
			if (PWD_TAG.equals(qName)) {
				encrypted = Boolean.parseBoolean(attributes.getValue(ENCRYPT_ATTR));
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (USER_TAG.equals(qName))
				user = elmtValue.toString();
			else if (PWD_TAG.equals(qName))
				pwd = elmtValue.toString();
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			elmtValue.append(ch, start, length);
		}
	}
}
