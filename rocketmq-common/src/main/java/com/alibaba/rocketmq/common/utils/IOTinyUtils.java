/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.alibaba.rocketmq.common.utils;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class IOTinyUtils {

	static public String toString(InputStream input, String encoding) throws IOException {
		return (null == encoding) ? toString(new InputStreamReader(input))
				: toString(new InputStreamReader(input, encoding));
	}

	static public String toString(Reader reader) throws IOException {
		CharArrayWriter sw = new CharArrayWriter();
		copy(reader, sw);
		return sw.toString();
	}

	static public long copy(Reader input, Writer output) throws IOException {
		char[] buffer = new char[1 << 12];
		long count = 0;
		for (int n = 0; (n = input.read(buffer)) >= 0;) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}

	static public List<String> readLines(Reader input) throws IOException {
		BufferedReader reader = toBufferedReader(input);
		List<String> list = new ArrayList<String>();
		String line = null;
		for (;;) {
			line = reader.readLine();
			if (null != line) {
				list.add(line);
			} else {
				break;
			}
		}
		return list;
	}

	static private BufferedReader toBufferedReader(Reader reader) {
		return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
	}

	static public void copyFile(String source, String target) throws IOException {
		File sourceFile = new File(source);
		if (!sourceFile.exists()) {
			throw new IllegalArgumentException("source file does not exist.");
		}

		File targetFile = new File(target);
		targetFile.getParentFile().mkdirs();
		if (!targetFile.exists() && !targetFile.createNewFile()) {
			throw new RuntimeException("failed to create target file.");
		}

		FileInputStream  fis = null;
		FileOutputStream fos = null;
		FileChannel sourceChannel = null;
		FileChannel targetChannel = null;

		try {
			fis = new FileInputStream(sourceFile);
			fos = new FileOutputStream(targetFile);
			sourceChannel = fis.getChannel();
			targetChannel = fos.getChannel();
			sourceChannel.transferTo(0, sourceChannel.size(), targetChannel);
		} finally {
			close(fis, fos, sourceChannel, targetChannel);
		}
	}

	/**
	 * 关闭资源
	 * @param resources 资源
	 */
	public static final void close(AutoCloseable... resources) {
		if (resources == null || resources.length == 0) {
			return;
		}

		for (AutoCloseable resource : resources) {
			if (resource != null) {
				try {
					resource.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}

	public static void delete(File fileOrDir) throws IOException {
		if (fileOrDir == null) {
			return;
		}

		if (fileOrDir.isDirectory()) {
			cleanDirectory(fileOrDir);
		}

		fileOrDir.delete();
	}

	public static void cleanDirectory(File directory) throws IOException {
		if (!directory.exists()) {
			String message = directory + " does not exist";
			throw new IllegalArgumentException(message);
		}

		if (!directory.isDirectory()) {
			String message = directory + " is not a directory";
			throw new IllegalArgumentException(message);
		}

		File[] files = directory.listFiles();
		if (files == null) { // null if security restricted
			throw new IOException("Failed to list contents of " + directory);
		}

		IOException exception = null;
		for (File file : files) {
			try {
				delete(file);
			} catch (IOException ioe) {
				exception = ioe;
			}
		}

		if (null != exception) {
			throw exception;
		}
	}

	public static void writeStringToFile(File file, String data, String encoding) throws IOException {
		OutputStream os = null;
		try {
			os = new FileOutputStream(file);
			os.write(data.getBytes(encoding));
		} finally {
			if (null != os) {
				os.close();
			}
		}
	}

}