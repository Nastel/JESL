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

import com.jkool.jesl.net.socket.SocketConnection;

/**
 *
 *
 * @version $Revision: 1 $
 */
public class AuthUtils {
	public static void authenticate(SocketConnection client, String user, String pwd) throws SecurityException {
		String respStr = null;
		try {
			client.sendMessage(new LoginRequest(user, pwd, true).generateMsg(), true);
			respStr = client.getReply();
		}
		catch (Throwable e) {
			throw new SecurityException("Failed to log in to '" + client.getHost() + "' with user '" + user + "'", e);
		}

		LoginResponse resp = LoginResponse.parseMsg(respStr);
		if (!resp.isSuccess()) {
			String msg = "Failed to log in to '" + client.getHost() + "' with user '" + user + "'";
			if (resp.getReason() != null)
				msg += ": " + resp.getReason();
			throw new SecurityException(msg);
		}
	}

	public static void authenticate(SocketConnection client, String token) throws SecurityException {
		String respStr = null;
		try {
			client.sendMessage(new AccessRequest(token).generateMsg(), true);
			respStr = client.getReply();
		}
		catch (Throwable e) {
			throw new SecurityException("Failed to validate access to '" + client.getHost() + "' with token '" + token + "'", e);
		}

		AccessResponse resp = AccessResponse.parseMsg(respStr);
		if (!resp.isSuccess()) {
			String msg = "Failed to validate access to '" + client.getHost() + "' with token '" + token + "'";
			if (resp.getReason() != null)
				msg += ": " + resp.getReason();
			throw new SecurityException(msg);
		}
	}
}
