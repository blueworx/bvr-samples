/** Licensed materials property of Blueworx
 *
 * SAMPLE
 *
 * (c) Copyright Blueworx. 2018. All Rights Reserved.
 */
package com.blueworx.samples.cdr.logging;

/**
 * This is the deserialised version of a single entry in the BVR topic.
 * Note that if you want to use the blueworx.cdr topic you should see 
 * the CDR-postgresql example for the deserialisation there
 */
public class TraceMessage {

	private String topic;
	private int accountID = -1, applicationID = -1;
	//These Strings will remain blank unless set by the deserialiser; so if for example
	//BVR didn't have a known callIndex for the trace point then it will remain blank.
	private String content = "", threadName = "", traceComponent = "", 
			traceID = "", timestamp = "", callIndex = "", traceLevel = "";

	public TraceMessage(String topic) {
		this.topic = topic;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public int getAccountID() {
		return accountID;
	}

	public void setAccountID(int accountID) {
		this.accountID = accountID;
	}

	public int getApplicationID() {
		return applicationID;
	}

	public void setApplicationID(int applicationID) {
		this.applicationID = applicationID;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getThreadName() {
		return threadName;
	}

	public void setThreadName(String threadName) {
		this.threadName = threadName;
	}

	public String getTraceComponent() {
		return traceComponent;
	}

	public void setTraceComponent(String traceComponent) {
		this.traceComponent = traceComponent;
	}

	public String getTraceID() {
		return traceID;
	}

	public void setTraceID(String traceID) {
		this.traceID = traceID;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getCallIndex() {
		return callIndex;
	}

	public void setCallIndex(String callIndex) {
		this.callIndex = callIndex;
	}

	public String getTraceLevel() {
		return traceLevel;
	}

	public void setTraceLevel(String traceLevel) {
		this.traceLevel = traceLevel;
	}
	
	/**
	 * This gets you the equivalent string to what BVR would trace out to a file based on the component parts of the message
	 */
	public String toOutputString() {
		StringBuilder builder = new StringBuilder();
		builder.append(timestamp);
		builder.append(' ');
		if ("".equals("callIndex"))
			builder.append("##################");//Sometimes the codeline is not associated with a particular call, so supply filler
		else builder.append(callIndex);
		builder.append(' ');
		builder.append(traceComponent);
		builder.append(" [");
		builder.append(threadName);
		builder.append("] ");
		builder.append(traceID);
		builder.append(' ');
		builder.append(content);
		return builder.toString();//Note that we are not using traceLevel here. 
		//In the straight to disc BVR logging, trace level determines whether or not the line is written out or not
		//When kafka is enabled, it also determines whether the message is sent out to the broker
		//Possible uses of the trace level are to further refine what you want to display or store
	}
}
