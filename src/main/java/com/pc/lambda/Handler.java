package com.pc.lambda;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Handler implements RequestHandler<String, String> {

	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public String handleRequest(String event, Context context) {

		final String FILE_NAME = "testFile.txt";
		LambdaLogger logger = context.getLogger();

		logger.log("\nevent => " + gson.toJson(event));
		logger.log("\ncontext => " + gson.toJson(context));

		String os = System.getProperty("os.name");
		logger.log("\nCurrent Opereting System => " + os);

		Path dir = Paths.get(event);
		if (Files.exists(dir)) {
			logger.log("\n!! Directory Available !!");
		} else {
			logger.log("\n!! Directory Unavailable !!");
			try {
				logger.log("\n!! Creating Directory !!");
				Files.createDirectories(dir);
				logger.log("\n!! Created Directory !!");
			} catch (IOException e) {
				logger.log("\n!! Error while creating directory !!");
			}
		}
		
		Path newFilePath = Paths.get(event + "/" + FILE_NAME);
		if (Files.exists(newFilePath)) {
			logger.log("\nFile Available: " + newFilePath.toAbsolutePath().toString());
		} else {
			logger.log("\nFile Unavailable: " + newFilePath.toAbsolutePath().toString());
		}

		try {
			writeFile(newFilePath);
			logger.log("\nFile written successfully: " + newFilePath.toAbsolutePath().toString());

			readFile(newFilePath);
			logger.log("\nFile read successfully: " + newFilePath.toAbsolutePath().toString());
		} catch (Exception e) {
			logger.log("\nException while performing file operations: " + e.getMessage());
		}
		
		logger.log("\n\n");
		return "Processed successfully";
	}

	private void writeFile(Path file) throws IOException {
		// Create the set of options for appending to the file.
		Set<OpenOption> options = new HashSet<OpenOption>();
		options.add(APPEND);
		options.add(CREATE);

		try (SeekableByteChannel sbc = Files.newByteChannel(file, options)) {
			// Convert the string to a ByteBuffer.
			String s = "================ Testing out the file reading and writing =========================";
			byte data[] = s.getBytes();
			ByteBuffer bb = ByteBuffer.wrap(data);
			sbc.write(bb);
		}
	}

	private void readFile(Path file) throws IOException {
		try (SeekableByteChannel sbc = Files.newByteChannel(file)) {
			final int BUFFER_CAPACITY = 100;
			ByteBuffer buf = ByteBuffer.allocate(BUFFER_CAPACITY);

			// Read the bytes with the proper encoding for this platform. If you skip this
			// step, you might see foreign or ill-eligible characters.
			String encoding = System.getProperty("file.encoding");
			System.out.print("\nEncoding: " + encoding);
			while (sbc.read(buf) > 0) {
				buf.flip();
				System.out.print("\n");
				System.out.print(Charset.forName(encoding).decode(buf));
				System.out.print("\n");
				buf.clear();
			}
		}
	}
}
