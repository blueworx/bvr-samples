/** Licensed materials property of Blueworx
 *
 * SAMPLE
 *
 * (c) Copyright Blueworx. 2018. All Rights Reserved.
 */
package com.blueworx.samples.cdr.logging;

import java.io.UnsupportedEncodingException;
import java.util.Map;

@SuppressWarnings("rawtypes") 
public class TraceMessageDeserialiser implements org.apache.kafka.common.serialization.Deserializer {
	
	public void configure(Map configs, boolean isKey) {
		// nothing to configure
	}

	public TraceMessage deserialize(String topic, byte[] data) {
		TraceMessage msg = new TraceMessage(topic);
		try {
			StringBuffer buffer = new StringBuffer(new String(data, "UTF-8"));
			if (buffer.charAt(0) == '{')
				buffer.delete(0, 1);
			while (buffer.length() > 0) {
				int lineEnd = buffer.indexOf("\n");
				String current;
				if (lineEnd == -1) {
					current = buffer.toString().trim();
					if (current.endsWith("}"))
						current = current.substring(0, current.length() - 1);
					buffer.delete(0, buffer.length());
				} else {
					current = buffer.substring(0, lineEnd).trim();
					buffer.delete(0, lineEnd + 1);
				}
				if ("".equals(current))
					continue;
				if (!current.matches("\"[a-zA-Z]+\":\".*"))//This pattern doesn't check the end of the line because the content may be multiple lines
					throw new IllegalArgumentException("Could not deserialise string '" + current + "' from " + new String(data, "UTF-8"));
				String key = current.substring(1).replaceFirst("\".*", "").toLowerCase();
				String value = current.replaceFirst(".*:\"", "");
				if (value.matches(".*\",?$")){//The comma won't be present on the last element, so it needs to be optional here
					value = value.replaceFirst("\",?$", "");
				} else {
					//OK, multi-line - we can cheat a little in that we know the last kvp is traceLevel and that's never multiline
					//Thus, the string MUST end in ",
					int actualEnd = buffer.indexOf("\",\n");
					value += "\n" + buffer.substring(0, actualEnd);
					buffer.delete(0, actualEnd + 3);
				} 
				if (key.equals("accountid")) {
					msg.setAccountID(Integer.parseInt(value.substring(2), 16));
				} else if (key.equals("applicationid")) {
					msg.setApplicationID(Integer.parseInt(value.substring(2), 16));
				} else if (key.equals("callindex")) {
					msg.setCallIndex(value);
				} else if (key.equals("content")) {
					msg.setContent(value);
				} else if (key.equals("tracecomponent")) {
					msg.setTraceComponent(value);
				} else if (key.equals("thread")) {
					msg.setThreadName(value);
				} else if (key.equals("traceid")) {
					msg.setTraceID(value);
				} else if (key.equals("tracelevel")) {
					msg.setTraceLevel(value);
				} else if (key.equals("timestamp")) {
					msg.setTimestamp(value);
				} else throw new IllegalArgumentException("Bad key supplied '" + key + "' for line " + current);
			}
		} catch (UnsupportedEncodingException e) {//This shouldn't happen
			e.printStackTrace();
		}
		return msg;
	}

	public void close() {
		// nothing to close
	}
	
	public static void main(String[] args) {
		//Simple sanity test for errors in deserialisation with a static example
		String trace = "{\n"+
		        "		\"accountID\":\"0x0\",\n" +
		        "		\"applicationID\":\"0x10000000\",\n" +
		        "		\"callIndex\":\"VC091431F700000001\",\n" +
		        "		\"content\":\"CALL - Call End\",\n" +
		        "		\"thread\":\"Call dispatcher 1  -  239\",\n" +
		        "		\"timestamp\":\"2018/08/16 17:00:22.907\",\n" +
		        "		\"traceComponent\":\"CALL\",\n" +
		        "		\"traceID\":\"[T003324]\",\n" +
		        "		\"traceLevel\":\"TRACE\"\n" +
"		}";
		TraceMessageDeserialiser deserialiser = new TraceMessageDeserialiser();
		deserialiser.deserialize("blueworx.application", trace.getBytes());
		deserialiser.close();
	}
}
