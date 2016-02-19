package org.trax;

import java.io.File;
import java.net.URL;

import org.trax.TraxClient.ImageFormat;

public abstract class TraxImage {

	public static enum ImageFileFormat {JPEG, PNG}

	public static enum PixelFormat {GRAY8, GRAY16, RGB}
	
	public static class PathImage extends TraxImage {
		
		private File file;
		
		public PathImage(File file) {

			this.file = file;
			
		}
		
		public String imageToString() {
			
			return file.toURI().toString();
			
		}

		@Override
		public ImageFormat getFormat() {
			return ImageFormat.PATH;
		}
	}
	
	public static class URLImage extends TraxImage {
		
		private URL url;
		
		public URLImage(URL url) {

			this.url = url;
			
		}
		
		public String imageToString() {
			
			return url.toString();
			
		}

		@Override
		public ImageFormat getFormat() {
			return ImageFormat.PATH;
		}
	}
	
	
	public static class BufferImage extends TraxImage {
		
		private byte[] data;
		
		private ImageFileFormat format;
		
		private ImageFileFormat verifyImageFormat(byte[] data) throws TraxException {

		    if (data.length < 4) throw new TraxException("Unsupported image file format.");
			
		    if (data[0] == 255 && data[1] == 216 && data[2] == 255 && data[3] == 224) return ImageFileFormat.JPEG;

		    if (data[0] == 137 && data[1] == 80 && data[2] == 78 && data[3] == 71) return ImageFileFormat.PNG;
		    
		    throw new TraxException("Unsupported image file format.");
		}
		
		public BufferImage(byte[] data) throws TraxException {

			this.data = data;
			
			this.format = verifyImageFormat(data);
			
		}
		
		public String imageToString() {
			
			StringBuilder builder = new StringBuilder();
			
			builder.append("data:");
			
			switch (format) {
			case JPEG:
				builder.append("image/jpeg;");
				break;
			case PNG:
				builder.append("image/png;");
				break;			
			}
			
			builder.append(Base64.encode(data));
			
			return builder.toString();
			
		}

		@Override
		public ImageFormat getFormat() {
			return ImageFormat.BUFFER;
		}
	}
	
	public static TraxImage parseImage(String input) {
		
		int pos = input.indexOf(':');
		
		if (pos < 1) return new PathImage(new File(input));
		
		String scheme = input.substring(0, pos).toLowerCase();
		
		if (scheme.compareTo("file") == 0) {
			
			if (input.length() > 7 && input.charAt(5) == '/' && input.charAt(6) == '/') {
				return new PathImage(new File(input.substring(7)));
			} else return null;
			
		} else if (scheme.compareTo("data") == 0) {
		
			int pos2 = input.indexOf(';', pos);
			if (pos2 < 1) return null;
			
			//String format = input.substring(pos+1, pos2);
			
			try {
				
				return new BufferImage(input.substring(pos2+1, input.length()-1).getBytes());
				
			} catch (TraxException e) {
				return null;
			}
			
		}
		
		
		return null;
		
	}

	public abstract String imageToString();
	
	public abstract ImageFormat getFormat();
		
}
