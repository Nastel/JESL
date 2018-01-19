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
package com.jkoolcloud.jesl.net.ssl;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Implements a factory for generating client- and server-side SSL Contexts.
 *
 * @version $Revision: 1 $
 */
public class SSLContextFactory {
	protected static final String PROTOCOL = "SSL";
	protected static final String KEYSTORE_TYPE = "JKS"; // default to javax.net.ssl.keyStoreType/trustStoreType

	protected SSLContext serverContext = null;
	protected SSLContext clientContext = null;
	protected SSLContext dfltContext = null;

	/**
	 * Create default SSL context factory
	 *
	 * @throws SecurityException
	 *             if error initializing context factory using default context
	 */
	public SSLContextFactory() throws SecurityException {
		init();
	}

	/**
	 * Create SSL context factory with given attributes
	 *
	 * @param keyStoreFileName
	 *            key store file name
	 * @param keyStorePassword
	 *            key store password
	 * @param keyPassword
	 *            key password
	 * @throws SecurityException
	 *             if error initializing context factory using specified keystore
	 */
	public SSLContextFactory(String keyStoreFileName, String keyStorePassword, String keyPassword)
			throws SecurityException {
		if (keyStoreFileName == null) {
			init();
		} else {
			init(keyStoreFileName, keyStorePassword, keyPassword);
		}
	}

	/**
	 * Initialize default SSL context
	 *
	 * @throws SecurityException
	 *             if error initializing context factory using default context
	 */
	protected void init() throws SecurityException {
		try {
			dfltContext = SSLContext.getDefault();
		} catch (Exception e) {
			throw new SecurityException("Failed to initialize the default SSLContext", e);
		}
	}

	/**
	 * Initialize SSL context factory with given attributes
	 *
	 * @param keyStoreFileName
	 *            key store file name
	 * @param keyStorePassword
	 *            key store password
	 * @param keyPassword
	 *            key password
	 * @throws SecurityException
	 *             if error initializing context factory using specified keystore
	 */
	protected void init(String keyStoreFileName, String keyStorePassword, String keyPassword) throws SecurityException {
		String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
		if (algorithm == null) {
			algorithm = "SunX509";
		}

		KeyManagerFactory kmf = null;
		TrustManagerFactory tmf = null;

		try {
			// Load keystore
			KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE);
			FileInputStream fis = null;
			try {
				ks.load(new FileInputStream(keyStoreFileName), keyStorePassword.toCharArray());
			} finally {
				if (fis != null) {
					fis.close();
				}
			}

			// Set up key manager factory to use our keystore
			kmf = KeyManagerFactory.getInstance(algorithm);
			kmf.init(ks, keyPassword.toCharArray());

			// Set up trust manager factory to use our keystore
			tmf = TrustManagerFactory.getInstance(algorithm);
			tmf.init(ks);
		} catch (Exception e) {
			throw new SecurityException("Failed to initialize key manager", e);
		}

		try {
			// Initialize the server SSLContext to work with our key managers.
			serverContext = SSLContext.getInstance(PROTOCOL);
			serverContext.init(kmf.getKeyManagers(), null, null);
		} catch (Exception e) {
			throw new SecurityException("Failed to initialize the server-side SSLContext", e);
		}

		try {
			// Initialize the client SSLContext to work with our trust manager
			clientContext = SSLContext.getInstance(PROTOCOL);
			clientContext.init(null, tmf.getTrustManagers(), null);
		} catch (Exception e) {
			throw new SecurityException("Failed to initialize the client-side SSLContext", e);
		}
	}

	/**
	 * Obtain <code>SSLContext</code> instance
	 *
	 * @param isClient
	 *            is client
	 * @return SSL context instance
	 */
	public SSLContext getSslContext(boolean isClient) {
		if (dfltContext != null) {
			return dfltContext;
		}
		if (isClient) {
			return clientContext;
		}
		return serverContext;
	}
}
