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

import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

/**
 * Implements AES encryption/decryption.
 *
 * This implementation assumes that the length of the original string is inserted into
 * the first byte of the string to be encrypted, and thus this length is extracted
 * when a string is decrypted.
 */
public class AesCipher {
	private static final String AES_ALGORITHM = "AES/ECB/NoPadding";
	private static final String AES_KEY_TYPE  = "AES";
	private static final int    AES_BLOCK_SIZE = 16;

	/**
	 * AES-encrypts the specified string using the given key.
	 *
	 * @param str string to encrypt
	 * @param key encryption key
	 * @return Base64-encoded encrypted value
	 * @throws GeneralSecurityException if errors encrypting string
	 */
	public static String encrypt(String str, byte[] key) throws GeneralSecurityException {
		Key keySpec = new SecretKeySpec(key, AES_KEY_TYPE);
		Cipher c = Cipher.getInstance(AES_ALGORITHM);
		c.init(Cipher.ENCRYPT_MODE, keySpec);

		if (str.length() > 255)
			throw new GeneralSecurityException("specified input string exceed maximum supported length of 255");

		// Determine length of string to be encrypted:
		//    - add 1 to encode length of original string into string to be encrypted
		//    - string to be encrypted must be padded to length that's a multiple of block size
		int strLen = 1 + str.length();
		int nEncLen = (strLen/AES_BLOCK_SIZE*AES_BLOCK_SIZE)
						+ (strLen%AES_BLOCK_SIZE != 0 ? AES_BLOCK_SIZE : 0);

		byte[] strBytes = new byte[nEncLen];

		// encode length of original string into first byte of encoded string
		strBytes[0] = (byte)str.length();

		// add original string to encoded string
		System.arraycopy(str.getBytes(), 0, strBytes, 1, str.length());

		// pad encoded string with blanks if necessary
		if (nEncLen > strLen)
			Arrays.fill(strBytes, strLen, nEncLen, (byte)' ');

		// perform encryption
		byte[] encVal = c.doFinal(strBytes);

		// return base64-encoding of encrypted value
        return new String(Base64.encodeBase64(encVal));
	}

	/**
	 * AES-decrypts the specified encrypted string using the given key.
	 *
	 * @param str Base64-encoded encrypted string to decrypt
	 * @param key encryption key
	 * @return decrypted string
	 * @throws GeneralSecurityException if errors decrypting string
	 */
	public static String decrypt(String str, byte[] key) throws GeneralSecurityException {
		Key keySpec = new SecretKeySpec(key, AES_KEY_TYPE);
		Cipher c = Cipher.getInstance(AES_ALGORITHM);
		c.init(Cipher.DECRYPT_MODE, keySpec);

		// Base64-decode value to get raw encrypted value
		byte[] encVal = Base64.decodeBase64(str.getBytes());

		// perform decryption
        String decStr = new String(c.doFinal(encVal));

        // extract length of original string from first byte of encoded string
        int origLen = decStr.getBytes()[0];

        // extract original string from encoded string
        return decStr.substring(1, 1+origLen);
	}
}
