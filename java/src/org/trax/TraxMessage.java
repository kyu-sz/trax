package org.trax;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

public class TraxMessage {
	
	private static final int PARSE_STATE_TYPE = 0;
	private static final int PARSE_STATE_SPACE_EXPECT = 1;		
	private static final int PARSE_STATE_SPACE = 2;	
	private static final int PARSE_STATE_UNQUOTED_KEY = 3;
	private static final int PARSE_STATE_UNQUOTED_VALUE = 4;
	private static final int PARSE_STATE_UNQUOTED_ESCAPE_KEY = 5;
	private static final int PARSE_STATE_UNQUOTED_ESCAPE_VALUE = 6;
	
	private static final int PARSE_STATE_QUOTED_KEY = 7;
	private static final int PARSE_STATE_QUOTED_VALUE = 8;
	private static final int PARSE_STATE_QUOTED_ESCAPE_KEY = 9;	
	private static final int PARSE_STATE_QUOTED_ESCAPE_VALUE = 10;	
	
	private static final int PARSE_STATE_PASS = 100;

	private static final int PARSE_MAX_KEY_LENGTH = 16;

	private static final String TRAX_PREFIX = "@@TRAX:";
	
	public static enum MessageType {INITIALIZE, FRAME, STATE, QUIT, HELLO};
	
	private MessageType type;
	
	private Vector<String> args = new Vector<String>();
	
	private Hashtable<String, String> kwargs = new Hashtable<String, String>();
	
	private static boolean isValidKey(String key) {
		
		if (key.length() < 1 || key.length() > PARSE_MAX_KEY_LENGTH) return false;
		
		for (int i = 0; i < key.length(); i++) {
			char c = key.charAt(i);
			if (!(Character.isLetterOrDigit(c) || c == '.' || c == '_'))
				return false;
			
		}
		
		return true;
	}
	
	public static TraxMessage createInitialize(TraxImage frame, TraxRegion region, Map<String, String> properties) {
		
		TraxMessage message = new TraxMessage();
		
		message.type = MessageType.INITIALIZE;
		
		message.args.add(frame.imageToString());
		
		message.args.add(region.toString());
		
		if (properties != null)
			message.kwargs.putAll(properties);
		
		return message;
	}
	
	public static TraxMessage createFrame(TraxImage frame, Map<String, String> properties) {
		
		TraxMessage message = new TraxMessage();
		
		message.type = MessageType.FRAME;
		
		message.args.add(frame.imageToString());
		
		if (properties != null)
			message.kwargs.putAll(properties);
		
		return message;
	}
	
	public static TraxMessage createQuit(Map<String, String> properties) {
		
		TraxMessage message = new TraxMessage();
		
		message.type = MessageType.QUIT;
		
		if (properties != null)
			message.kwargs.putAll(properties);
		
		return message;
	}
	
	public static TraxMessage readFromReader(Reader reader) throws IOException {
		
		TraxMessage message = null;
		
		StringBuilder keyBuffer = new StringBuilder();
		StringBuilder valueBuffer = new StringBuilder();
		
		boolean complete = false;
		
	    int state = -TRAX_PREFIX.length();

	    while (!complete) {
	    	
	    	int val = reader.read();
	    	char chr;
	    	
	    	if (val == -1) {
	    		if (message == null) break;
	    		chr = '\n';
	    		complete = true;
	    	} else chr = (char) val;

	        switch (state) {
	            case PARSE_STATE_TYPE: { // Parsing message type

	            	try {
	            	
		                if (Character.isLetterOrDigit(val)) {
	
		                    keyBuffer.append(chr);
	
		                } else if (chr == ' ') {
	
		                	message = new TraxMessage();
		                    message.type = MessageType.valueOf(keyBuffer.toString().toUpperCase());
		                    state = PARSE_STATE_SPACE; 
	
		                    keyBuffer = new StringBuilder();
		                    valueBuffer = new StringBuilder();
	
		                } else if (chr == '\n') {
	
	                    	message = new TraxMessage();
	                        message.type = MessageType.valueOf(keyBuffer.toString().toUpperCase());
	
		                    keyBuffer = new StringBuilder();
		                    valueBuffer = new StringBuilder();
	
		                    complete = true;
		                    
		                } else {
		                    state = PARSE_STATE_PASS;
		                    keyBuffer = new StringBuilder();
		                }

	            	} catch (IllegalArgumentException e) {
	            		state = PARSE_STATE_PASS;
	            		message = null;
	            		keyBuffer = new StringBuilder();
	            	}
	                
	                break;
	            }
	            case PARSE_STATE_SPACE_EXPECT: {

	                if (chr == ' ') {
	                    state = PARSE_STATE_SPACE;
	                } else if (chr == '\n') {
	                	complete = true;
	                } else {
	                	message = null;
	                	state = PARSE_STATE_PASS;
	                    keyBuffer = new StringBuilder(Character.toString(chr));
	                    valueBuffer = new StringBuilder();	                	
	                }

	                break;

	            }		            
	            case PARSE_STATE_SPACE: {

	                if (chr == ' ' || chr == '\r') {
	                    // Do nothing
	                } else if (chr == '\n') {
	                	complete = true;
	                } else if (chr == '"') {
	                    state = PARSE_STATE_QUOTED_KEY;
	                    keyBuffer = new StringBuilder();
	                    valueBuffer = new StringBuilder();
	                } else {
	                	state = PARSE_STATE_UNQUOTED_KEY;
	                    keyBuffer = new StringBuilder(Character.toString(chr));
	                    valueBuffer = new StringBuilder();	                	
	                }

	                break;

	            }	            
	            case PARSE_STATE_UNQUOTED_KEY: {

	                if (chr == '\\') {
	                    state = PARSE_STATE_UNQUOTED_ESCAPE_KEY;
	                } else if (chr == '\n') { // append arg and finalize
	                	message.args.add(keyBuffer.toString());
	                    complete = true;
	                } else if (chr == ' ') { // append arg and move on
	                	message.args.add(keyBuffer.toString());
	                    state = PARSE_STATE_SPACE;
	                    keyBuffer = new StringBuilder();
	                } else if (chr == '=') { // we have a kwarg
	                	if (isValidKey(keyBuffer.toString()))
	                    	state = PARSE_STATE_UNQUOTED_VALUE;
	                	else {
	                		keyBuffer.append(chr);
	                	}
	                } else {                    
	                	keyBuffer.append(chr);
	                } 

	                break;

	            }
	            case PARSE_STATE_UNQUOTED_VALUE: {

	                if (chr == '\\') {
	                    state = PARSE_STATE_UNQUOTED_ESCAPE_VALUE;
	                } else if (chr == ' ') {
	                	
	                	message.kwargs.put(keyBuffer.toString(), valueBuffer.toString());
	                    state = PARSE_STATE_SPACE;
	                    keyBuffer = new StringBuilder();
	                    valueBuffer = new StringBuilder();                  
	                } else if (chr == '\n') {
	                	message.kwargs.put(keyBuffer.toString(), valueBuffer.toString());
	                    complete = true;
	                    keyBuffer = new StringBuilder();
	                    valueBuffer = new StringBuilder();
	                } else
	                    valueBuffer.append(chr);

	                break;

	            }
	            case PARSE_STATE_UNQUOTED_ESCAPE_KEY: {

	                if (chr == 'n') {
	                	keyBuffer.append('\n');
	                    state = PARSE_STATE_UNQUOTED_KEY;
	                } else if (chr != '\n') {
	                	keyBuffer.append(chr);
	                    state = PARSE_STATE_UNQUOTED_KEY;
	                } else {
	                    state = PARSE_STATE_PASS;
	                    message = null;
	                    keyBuffer = new StringBuilder();
	                    valueBuffer = new StringBuilder();
	                }
	                
	                break;

	            }	            
	            case PARSE_STATE_UNQUOTED_ESCAPE_VALUE: {

	                if (chr == 'n') {
	                    valueBuffer.append('\n');
	                    state = PARSE_STATE_UNQUOTED_VALUE;
	                } else if (chr != '\n') {
	                    valueBuffer.append(chr);
	                    state = PARSE_STATE_UNQUOTED_VALUE;
	                } else {
	                    state = PARSE_STATE_PASS;
	                    message = null;
	                    keyBuffer = new StringBuilder();
	                    valueBuffer = new StringBuilder();
	                }
	                
	                break;

	            }

	            case PARSE_STATE_QUOTED_KEY: {

	                if (chr == '\\') {
	                    state = PARSE_STATE_QUOTED_ESCAPE_KEY;
	                } else if (chr == '"') { // append arg and move on
	                	message.args.add(keyBuffer.toString());
	                	state = PARSE_STATE_SPACE_EXPECT;
	                } else if (chr == '=') { // we have a kwarg
	                	if (isValidKey(keyBuffer.toString()))
	                    	state = PARSE_STATE_QUOTED_VALUE;
	                	else {
	                		keyBuffer.append(chr);       		
	                	}
	                } else {                    
	                	keyBuffer.append(chr);
	                } 

	                break;

	            }
	            case PARSE_STATE_QUOTED_VALUE: {

	                if (chr == '\\') {
	                    state = PARSE_STATE_QUOTED_ESCAPE_VALUE;
	                } else if (chr == '"') {	                
	                	message.kwargs.put(keyBuffer.toString(), valueBuffer.toString());
	                    state = PARSE_STATE_SPACE_EXPECT;
	                    keyBuffer = new StringBuilder();
	                    valueBuffer = new StringBuilder();                  
	                } else
	                    valueBuffer.append(chr);

	                break;

	            }
	            case PARSE_STATE_QUOTED_ESCAPE_KEY: {

	                if (chr == 'n') {
	                	keyBuffer.append('\n');
	                    state = PARSE_STATE_QUOTED_KEY;
	                } else if (chr != '\n') {
	                	keyBuffer.append(chr);
	                    state = PARSE_STATE_QUOTED_KEY;
	                } else {
	                    state = PARSE_STATE_PASS;
	                    message = null;
	                    keyBuffer = new StringBuilder();
	                    valueBuffer = new StringBuilder();
	                }
	                
	                break;

	            }		            
	            case PARSE_STATE_QUOTED_ESCAPE_VALUE: {

	                if (chr == 'n') {
	                    valueBuffer.append('\n');
	                    state = PARSE_STATE_QUOTED_VALUE;
	                } else if (chr != '\n') {
	                    valueBuffer.append(chr);
	                    state = PARSE_STATE_QUOTED_VALUE;
	                } else {
	                    state = PARSE_STATE_PASS;
	                    message = null;
	                    keyBuffer = new StringBuilder();
	                    valueBuffer = new StringBuilder();
	                }
	                
	                break;

	            }	            
	            
	            case PARSE_STATE_PASS: {

	                if (chr == '\n') 
	                    state = -TRAX_PREFIX.length();

	                break;
	            }
	            default: { // Parsing prefix

	                if (state < 0) {
	                    if (chr == TRAX_PREFIX.charAt(TRAX_PREFIX.length()+state))
	                    	// When done, go to type parsing
	                        state++; 
	                    else 
	                    	// Not a message
	                        state = chr == '\n' ? -TRAX_PREFIX.length() : PARSE_STATE_PASS; 

	                }

	                break;
	            }
	        }

	    }

	    return message;
	    
	}
	
	public MessageType getType() {
		return type;
	}

	private TraxMessage() {
		
	}

	public String getArgument(int index) {
		return args.get(index);
	}

	public int argumentsCount() {
		return args.size();
	}

	public String getProperty(String key) {
		return kwargs.get(key);
	}

	public Enumeration<String> propertyKeys() {
		return kwargs.keys();
	}

	public Map<String, String> copyProperties(Map<String, String> map) {
		if (map == null) map = new Hashtable<String, String>();
		map.putAll(kwargs);
		return map;
	}
	
	public void write(PrintWriter out) {
	
		out.print(TRAX_PREFIX);
		out.print(type.toString().toLowerCase());
		out.print(" ");
		
		for (String arg : args) {
			out.print("\"" + arg.replace("\n", "\\n").replace("\"", "\\\"").replace("\\", "\\\\") + "\" ");
		}

		for (String key : kwargs.keySet()) {
			out.print("\"" + key + "=" + kwargs.get(key).replace("\n", "\\n").replace("\"", "\\\"").replace("\\", "\\\\") + "\" ");
		}
		
		out.print("\n");
		
		out.flush();
	}

	public boolean containsProperty(String key) {
		return kwargs.containsKey(key);
	}
	
	
	public static void testParsing(Reader reader) throws IOException {
		
		PrintWriter out = new PrintWriter(System.out);
		
		while (true) {
			
			TraxMessage message = TraxMessage.readFromReader(reader);
			
			if (message == null) break;
			
			message.write(out);
			
			out.flush();
		}
				
	}
}
