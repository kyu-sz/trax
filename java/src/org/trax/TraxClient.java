package org.trax;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;
import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.trax.TraxMessage.MessageType;
import org.trax.TraxRegion.RegionFormat;

public class TraxClient {

	public static enum LoggingType {
		NONE, NO_PROTOCOL, IN_PROTOCOL, OUT_PROTOCOL, ALL
	};

	public static enum ImageFormat {
		PATH, URL, MEMORY, BUFFER
	};

	private BufferedReader in;

	private PrintWriter out, log;

	private boolean initialized = false;

	private EnumSet<RegionFormat> regionFormats;

	private EnumSet<ImageFormat> imageFormats;

	private int protocolVersion;

	private String trackerName;

	private String trackerIdentifier;

	private class LoggerReader extends BufferedReader {

		private Writer log;

		public LoggerReader(Reader in, Writer log) {
			super(in);
			this.log = log;
		}

		@Override
		public int read() throws IOException {
			int val = super.read();
			if (val != -1)
				log.write(val);
			return val;
		}

	}

	private class LoggerWriter extends PrintWriter {

		public LoggerWriter(OutputStream out, Writer log) {
			super(out, true);
			this.log = log;
		}

		private Writer log;

		@Override
		public void write(String s) {
			super.write(s);
			try {
				log.write(s);
			} catch (IOException e) {
			}
		}

		@Override
		public void flush() {
			super.flush();
			try {
				log.flush();
			} catch (IOException e) {
			}
		}
	}

	public TraxClient(InputStream input, OutputStream output, PrintWriter log,
			LoggingType type) throws TraxException {

		this.log = log;

		if (this.log != null) {
			switch (type) {
			case ALL:
				in = new LoggerReader(new InputStreamReader(input), this.log);
				out = new LoggerWriter(output, this.log);
				break;
			case IN_PROTOCOL:
				in = new LoggerReader(new InputStreamReader(input), this.log);
				out = new PrintWriter(output, true);
				break;
			case OUT_PROTOCOL:
				in = new BufferedReader(new InputStreamReader(input));
				out = new LoggerWriter(output, this.log); // TODO: filter
															// protocol messages
				break;
			case NO_PROTOCOL:
				in = new BufferedReader(new InputStreamReader(input)); // TODO:
																		// filter
																		// protocol
																		// messages
				out = new PrintWriter(output, true); // TODO: filter protocol
														// messages
				break;
			default:
				in = new BufferedReader(new InputStreamReader(input));
				out = new PrintWriter(output, true);
				break;
			}

		} else {
			in = new BufferedReader(new InputStreamReader(input));
			out = new PrintWriter(output, true);
		}

		handshake();

	}

	public TraxClient(InputStream input, OutputStream output, PrintWriter log)
			throws TraxException {

		this(input, output, log, LoggingType.ALL);

	}

	public TraxClient(InputStream input, OutputStream output)
			throws TraxException {

		this(input, output, null);

	}

	private void handshake() throws TraxException {

		try {

			TraxMessage message = TraxMessage.readFromReader(in);

			if (message == null)
				throw new TraxException("Handshake failed");

			protocolVersion = 1;
			imageFormats = EnumSet.noneOf(ImageFormat.class);
			regionFormats = EnumSet.noneOf(RegionFormat.class);

			if (message.containsProperty("trax.version"))
				protocolVersion = Integer.parseInt(message
						.getProperty("trax.version"));

			if (message.containsProperty("trax.region")) {
				String[] formats = message.getProperty("trax.region")
						.split(";");
				regionFormats.clear();
				for (String format : formats) {
					RegionFormat f = RegionFormat.valueOf(format.trim().toUpperCase());
					if (f != null)
						regionFormats.add(f);
				}
			}

			if (message.containsProperty("trax.image")) {
				String[] formats = message.getProperty("trax.image").split(";");
				imageFormats.clear();
				for (String format : formats) {
					ImageFormat f = ImageFormat.valueOf(format.trim().toUpperCase());
					if (f != null)
						imageFormats.add(f);
				}
			}

			if (message.containsProperty("trax.name"))
				trackerName = message.getProperty("trax.name");

			if (message.containsProperty("trax.identifier"))
				trackerIdentifier = message.getProperty("trax.identifier");

			return;

		} catch (NoSuchElementException e) {
			throw new TraxException("Illegal message", e);
		} catch (IOException e) {
			throw new TraxException("Unable to parse message", e);
		}

	}

	public TraxStatus initialize(TraxImage image, TraxRegion groundtruth,
			Map<String, String> parameters) throws TraxException {

		TraxRegion converted = null;

		if (!regionFormats.contains(groundtruth.getFormat())) {

			if (regionFormats.contains(RegionFormat.MASK)) {
				converted = groundtruth.convertTo(RegionFormat.MASK);
			} else if (regionFormats.contains(RegionFormat.POLYGON)) {
				converted = groundtruth.convertTo(RegionFormat.POLYGON);
			} else if (regionFormats.contains(RegionFormat.RECTANGLE)) {
				converted = groundtruth.convertTo(RegionFormat.RECTANGLE);
			}

		} else converted = groundtruth;

		if (converted == null)
			throw new TraxException("No known region format is supported");

		if (!imageFormats.contains(image.getFormat()))
			throw new TraxException("Image format is unsupported by tracker");
		
		TraxMessage.createInitialize(image, converted, parameters).write(out);

		TraxMessage message;
		try {
			message = TraxMessage.readFromReader(in);
		} catch (IOException e) {
			throw new TraxException("Unable to read message", e);
		}

		if (message == null)
			throw new TraxException("Initialization failed");

		if (message.getType() != MessageType.STATE)
			throw new TraxException("Invalid message, STATUS expected.");

		if (message.argumentsCount() != 1)
			throw new TraxException("Malformed STATUS message.");

		TraxRegion region = TraxRegion.parseRegion(message.getArgument(0));

		TraxStatus status = new TraxStatus(region, message.copyProperties(null));

		initialized = true;

		return status;

	}

	public TraxStatus frame(TraxImage image, Map<String, String> parameters)
			throws TraxException {

		if (!initialized)
			throw new TraxException("Not initialized");

		if (!imageFormats.contains(image.getFormat()))
			throw new TraxException("Image format is unsupported by tracker");
		
		TraxMessage.createFrame(image, parameters).write(out);

		TraxMessage message;
		try {
			message = TraxMessage.readFromReader(in);
		} catch (IOException e) {
			throw new TraxException("Unable to read message", e);
		}

		if (message == null)
			throw new TraxException("Tracking failed");

		if (message.getType() != MessageType.STATE)
			throw new TraxException("Invalid message, STATUS expected.");

		if (message.argumentsCount() != 1)
			throw new TraxException("Malformed STATUS message.");

		TraxRegion region = TraxRegion.parseRegion(message.getArgument(0));

		TraxStatus status = new TraxStatus(region, message.copyProperties(null));

		return status;

	}

	public void quit() {

		TraxMessage.createQuit(null).write(out);

	}

	public Set<RegionFormat> getRegionFormat() {
		return Collections.unmodifiableSet(regionFormats);
	}

	public Set<ImageFormat> getImageFormat() {
		return Collections.unmodifiableSet(imageFormats);
	}

	public int getProtocolVersion() {
		return protocolVersion;
	}

	public String getTrackerName() {
		return trackerName;
	}

	public String getTrackerIdentifier() {
		return trackerIdentifier;
	}

}
