package hu.kovaron.spring.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * Reads file directly from http response body.
 *
 * @author Aron Kovacs
 */
public class FileMessageConverter extends AbstractHttpMessageConverter<File> {

	private static final Logger LOGGER = LoggerFactory.getLogger(FileMessageConverter.class);

	private static final String TEMP_FILE_PREFIX = "download-temp-file";

	public FileMessageConverter() {
		super(MediaType.APPLICATION_OCTET_STREAM);
	}

	@Override
	protected boolean supports(final Class<?> clazz) {
		return File.class.equals(clazz);
	}

	@Override
	protected File readInternal(final Class<? extends File> clazz, final HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
		File file = null;
		BufferedOutputStream bufferedOutputStream = null;
		FileOutputStream os = null;
		try {
			os = new FileOutputStream(file);
			bufferedOutputStream = new BufferedOutputStream(os);
			file = File.createTempFile(TEMP_FILE_PREFIX, null);
			file.deleteOnExit();
			IOUtils.copy(inputMessage.getBody(), bufferedOutputStream);
		} catch (final IOException e) {
			LOGGER.error("Error during HTTPMessage -> File conversion", e);
		} finally {
			close(os);
			close(bufferedOutputStream);
		}
		return file;
	}

	@Override
	protected void writeInternal(final File file, final HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
		FileInputStream is = null;
		BufferedInputStream bufferedInputStream = null;
		try {
			is = new FileInputStream(file);
			bufferedInputStream = new BufferedInputStream(is);
			IOUtils.copy(bufferedInputStream, outputMessage.getBody());
		} catch (final IOException e) {
			LOGGER.error("Error during File -> HTTPMessage conversion", e);
		} finally {
			close(is);
			close(bufferedInputStream);
		}
	}

	/**
	 * Closes the parameter closeable (mostly stream)
	 */
	private static void close(final Closeable c) {
		if (c == null) {
			return;
		}
		try {
			c.close();
		} catch (final IOException e) {
			LOGGER.warn("Error during closing stream", e);
		}
	}

}
