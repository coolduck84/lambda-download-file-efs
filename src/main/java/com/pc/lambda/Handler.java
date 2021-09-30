package com.pc.lambda;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Handler implements RequestHandler<String, String> {

	private static Gson gson = null;
	private static LambdaLogger logger = null;

	public String handleRequest(String fileDirectory, Context context) {

		logger = context.getLogger();
		gson = new GsonBuilder().setPrettyPrinting().create();
		final String FILE_NAME = fileDirectory + "/" + "testFile.xlsx";

		logger.log("\nevent => " + gson.toJson(fileDirectory));
		logger.log("\ncontext => " + gson.toJson(context));

		String os = System.getProperty("os.name");
		logger.log("\nCurrent Opereting System => " + os);

		Path dir = Paths.get(fileDirectory);
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

		Path newFilePath = Paths.get(FILE_NAME);
		if (Files.exists(newFilePath)) {
			logger.log("\nFile Available: " + newFilePath.toAbsolutePath().toString());
		} else {
			logger.log("\nFile Unavailable: " + newFilePath.toAbsolutePath().toString());
		}

		/*
		 * try { writeFile(newFilePath); logger.log("\nFile written successfully: " +
		 * newFilePath.toAbsolutePath().toString());
		 * 
		 * readFile(newFilePath); logger.log("\nFile read successfully: " +
		 * newFilePath.toAbsolutePath().toString()); } catch (Exception e) {
		 * logger.log("\nException while performing file operations: " +
		 * e.getMessage()); }
		 */

		try {
			writeExcelFile(FILE_NAME);
			logger.log("\nFile written successfully: " + FILE_NAME);
			
			newFilePath = Paths.get(FILE_NAME);
			if (Files.exists(newFilePath)) {
				logger.log("\nFile Available: " + newFilePath.toAbsolutePath().toString());
			} else {
				logger.log("\nFile Unavailable: " + newFilePath.toAbsolutePath().toString());
			}

			readExcelFile(FILE_NAME);
			logger.log("\nFile read successfully: " + FILE_NAME);
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

	private void writeExcelFile(String filePath) throws IOException {

		// Blank workbook
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			// Create a blank sheet
			XSSFSheet sheet = workbook.createSheet("Sample Data");

			// This data needs to be written (Object[])
			Map<String, Object[]> data = new TreeMap<String, Object[]>();
			data.put("1", new Object[] { "ID", "NAME", "LASTNAME" });
			data.put("2", new Object[] { 1, "Amit", "Shukla" });
			data.put("3", new Object[] { 2, "Lokesh", "Gupta" });
			data.put("4", new Object[] { 3, "John", "Adwards" });
			data.put("5", new Object[] { 4, "Brian", "Schultz" });

			// Iterate over data and write to sheet
			Set<String> keyset = data.keySet();
			int rownum = 0;
			for (String key : keyset) {
				Row row = sheet.createRow(rownum++);
				Object[] objArr = data.get(key);
				int cellnum = 0;
				for (Object obj : objArr) {
					Cell cell = row.createCell(cellnum++);
					if (obj instanceof String)
						cell.setCellValue((String) obj);
					else if (obj instanceof Integer)
						cell.setCellValue((Integer) obj);
				}
			}

			// Write the workbook in file system
			try (FileOutputStream out = new FileOutputStream(new File(filePath))) {
				workbook.write(out);
			}
		}
	}

	private void readExcelFile(String filePath) throws FileNotFoundException, IOException {
		try (FileInputStream file = new FileInputStream(new File(filePath))) {

			// Create Workbook instance holding reference to .xlsx file
			try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {

				// Get first/desired sheet from the workbook
				XSSFSheet sheet = workbook.getSheetAt(0);

				logger.log("\n");
				
				// Iterate through each rows one by one
				Iterator<Row> rowIterator = sheet.iterator();
				while (rowIterator.hasNext()) {
					Row row = rowIterator.next();
					// For each row, iterate through all the columns
					Iterator<Cell> cellIterator = row.cellIterator();

					while (cellIterator.hasNext()) {
						Cell cell = cellIterator.next();
						// Check the cell type and format accordingly
						switch (cell.getCellType()) {
						case Cell.CELL_TYPE_NUMERIC:
							logger.log(cell.getNumericCellValue() + "\t");
							break;
						case Cell.CELL_TYPE_STRING:
							logger.log(cell.getStringCellValue() + "\t");
							break;
						}
					}
					logger.log("\n ");
				}
			}
		}
	}
}
