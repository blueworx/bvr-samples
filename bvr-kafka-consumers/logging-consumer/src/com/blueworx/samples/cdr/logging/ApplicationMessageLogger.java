/** Licensed materials property of Blueworx
 *
 * SAMPLE
 *
 * (c) Copyright Blueworx. 2018. All Rights Reserved.
 */
package com.blueworx.samples.cdr.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Queue;

import org.apache.log4j.Logger;

/**
 * This class handles application logging, replicating the file creation per-call that is done in BVR local logging.
 * Trace points are arranged by call index to get to their destination files
 * Note that the numbers for filenames are arbitrary and don't use the callIndices of the trace statements - this is 
 * because those numbers eventually wrap back around in BVR. 
 * 
 * These files are not deleted at the end of the call like they are on BVR - I've marked out the code below where you
 *  can change that if disc space is a priority 
 */
public class ApplicationMessageLogger extends Thread {

	private Queue<TraceMessage> queue;
	private boolean running;
	private HashMap<String, BufferedWriter> callLogs;
	private File directory;
	private Logger logger;
	private long subDirNo = 0, fileNo = 0;
	private final long fileNoLimit;

	public ApplicationMessageLogger(Queue<TraceMessage> queue, File directory, Logger logger, long fileNumberLimit) {
		super("Application Logger");
		this.queue = queue;
		this.directory = new File(directory.getAbsolutePath() + File.separator + "callLogs");
		directory.mkdirs();
		this.callLogs = new HashMap<String, BufferedWriter>();
		running = true;
		this.logger = logger;
		
		fileNoLimit = fileNumberLimit;
		
		start();
	}
	
	public void close() {
		running = false;
		synchronized (queue) {
			queue.notify();
		}
	}
	
	public void run(){
		while (running) {
			TraceMessage message = null;
			synchronized (queue) {
				if (queue.isEmpty())
					try {
						queue.wait();
					} catch (InterruptedException e) {}
				message = queue.remove();
			}
			if (message == null)
				continue;
			BufferedWriter output = callLogs.get(message.getCallIndex());
			if (output == null) {//New call index, let's open a new file
				try {
					++fileNo;
					if (fileNo > fileNoLimit){
						fileNo = 0;
						++subDirNo;
					}
					File target = new File(directory.getAbsolutePath() + File.separator + subDirNo + File.separator + fileNo + ".out");
					target.getParentFile().mkdirs();
					output = new BufferedWriter(new FileWriter(target));
					callLogs.put(message.getCallIndex(), output);
					
					//Write the preamble to label the file
					output.write("------------------------------------");
					output.newLine();
					output.write("Blueworx Voice Response Linux - LOG FILE");
					output.newLine();
					output.write("------------------------------------");
					output.newLine();
					output.write("Call Index: " + message.getCallIndex());
					output.newLine();
					output.write("Start Time: " + Calendar.getInstance().getTime());
					output.newLine();
					output.write("------------------------------------");
					output.newLine();
				} catch (IOException e) {
					logger.error("Failed to create call log file: " + e.getMessage());
					close();
					continue;
				}
			}
			//OK, we have a writer, write!
			try {
				output.write(message.toOutputString());
				output.newLine();
				output.flush();
			} catch (IOException e) {
				logger.error("Failed to write to call log file: " + e.getMessage());
				close();
				continue;
			}
			if ("[T003324]".equals(message.getTraceID())) {
				//This is the last message on a call. Close the file handler
				//If you want to delete or archive the file on call hangup, now would be a great time
				callLogs.remove(message.getCallIndex());
				try {
					output.close();
				} catch (IOException e) {
					logger.info("Error on closing call log file: " + e.getMessage());
				}
			}
		}
		//OK, if we exited before BVR shut down we should clean up our filehandlers for calls that are still going
		Iterator<BufferedWriter> iter = callLogs.values().iterator();
		while (iter.hasNext())//This will be empty if all calls are shut down
			try {
				iter.next().close();
			} catch (IOException e) {} //We just want to shut down at this point, errors aren't important
		callLogs.clear();
	}
}
