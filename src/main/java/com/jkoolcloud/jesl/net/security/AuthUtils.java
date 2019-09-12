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
package com.jkoolcloud.jesl.net.security;

import com.jkoolcloud.jesl.net.JKStream;
import com.jkoolcloud.tnt4j.utils.Utils;

/**
 * JESL Authentication Utility class used to authenticate with jKool servers.
 *
 * @version $Revision: 1 $
 */
public class AuthUtils {
	/**
	 * Authenticate with jKool servers using client handle and a given access token.
	 *
	 * @param client
	 *            jKool client handle encapsulating connection to jKool servers
	 * @param token
	 *            access token to be used for authentication
	 * @throws SecurityException
	 *             if error validating token
	 */
	public static void authenticate(JKStream client, String token) throws SecurityException {
		String respStr = null;
		try {
			client.send(token, new AccessRequest(token).generateMsg(), true);
			respStr = client.read();
		} catch (Throwable e) {
			throw new SecurityException("Failed to authenticate with service='" + client.getURI() + "' token='"
					+ Utils.hide(token, "x", 4) + "'", e);
		}

		AccessResponse resp = AccessResponse.parseMsg(respStr);
		if (!resp.isSuccess()) {
			String msg = "Failed to validate access with service='" + client.getURI() + "' token='"
					+ Utils.hide(token, "x", 4) + "'";
			if (resp.getReason() != null) {
				msg += ": reason='" + resp.getReason() + "'";
			}
			throw new SecurityException(msg);
		}
	}
}
