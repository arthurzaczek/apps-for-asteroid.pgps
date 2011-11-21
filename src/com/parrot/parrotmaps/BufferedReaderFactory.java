/*
 * Copyright (c) 2011, Parrot
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   * Neither the name of "Parrot SA" nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 * PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.parrot.parrotmaps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class BufferedReaderFactory {
	
	//! Timeout for URL connection, 20s
	private static final int URL_CONNECTION_TIMEOUT = 20000;
	//! Timeout for URL read, 20s
	private static final int URL_READ_TIMEOUT = 20000;
	//! Buffer size of buffered reader
	private static final int READ_BUFFER_SIZE = 8*1024;
	
	public static BufferedReader openBufferedReader(URL url) throws IOException {
		BufferedReader bufferedReader = null;
		
		// Open URL connection
		URLConnection connection = url.openConnection();
		
		// Setup timeouts
		connection.setConnectTimeout(URL_CONNECTION_TIMEOUT);
		connection.setReadTimeout(URL_READ_TIMEOUT);
		
		bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()),READ_BUFFER_SIZE);

		return bufferedReader;
	}
}
