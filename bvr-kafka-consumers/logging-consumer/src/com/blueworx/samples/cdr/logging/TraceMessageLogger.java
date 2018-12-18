/** Blueworx Confidential
 *
 * Copyright (c) Blueworx. 2018. All rights reserved
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
package com.blueworx.samples.cdr.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Queue;

import org.apache.log4j.Logger;

/**
 * This class writes out to a file up to a size limit in bytes, then moves on to the next file in sequence
 * It can be used for error and trace file logging as the process is virtually identical
 */
public class TraceMessageLogger extends Thread {

	private Queue<TraceMessage> queue;
	private boolean running;
	private Logger logger;
	private BufferedWriter writer;
	private File directory;
	private long bytesWritten;
	private final long fileLimit;
	private final long fileCount;
	private int fileNumber = 0;
	private final String prefix;

	public TraceMessageLogger(String prefix, Queue<TraceMessage> queue, File directory, Logger logger, long fileLimit, long fileCount) throws IOException {
		super(prefix + " Logger");
		this.queue = queue;
		this.logger = logger;
		this.fileLimit = fileLimit;
		this.fileCount = fileCount;
		this.directory = directory;
		this.prefix = prefix.toLowerCase() + "_";
		boolean append = true;
		File traceFile = new File(directory.getAbsolutePath() + File.separator + this.prefix + "0.out");
		bytesWritten = traceFile.length();
		while (bytesWritten >= fileLimit && fileNumber < fileCount) {
			++fileCount;
			traceFile = new File(directory.getAbsolutePath() + File.separator + this.prefix + fileCount + ".out");
			bytesWritten = traceFile.length();
		}
		if (bytesWritten >= fileLimit) {
			//All full, just overwrite from the start
			fileNumber = 0;
			bytesWritten = 0;
			append = false;
		}
		writer = new BufferedWriter(new FileWriter(traceFile, append));
		
		//Let's write out the preamble to show where we started logging out the trace on this execution
		writer.write(prefix + " logger started at " + new Date(System.currentTimeMillis()));
		writer.newLine();
		writer.flush();
		running = true;
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
			synchronized (queue) {
				try {
					if (queue.isEmpty())
						queue.wait(10000);
				} catch (InterruptedException e) {}//We ignore this as we'll just cycle around
			}
			if (!running)
				continue;
			if (queue.isEmpty())
				continue;
			while (!queue.isEmpty()) {
				TraceMessage current = queue.remove();
				try {
					String content = current.toOutputString();
					bytesWritten += content.getBytes().length;
					writer.write(content);
					writer.newLine();
					bytesWritten += 2;//accounting for newline
				} catch (IOException e) {
					logger.error("Failed to write to trace file: " + e.getMessage());
					close();
					continue;
				}
				if (bytesWritten > fileLimit) 
					try{
						bytesWritten = 0;
						++fileNumber;
						if (fileNumber >= fileCount)//we count from 0
							fileNumber = 0;
						writer.close();
						File traceFile = new File(directory.getAbsolutePath() + File.separator + "trace_" + fileNumber + ".out");
						writer = new BufferedWriter(new FileWriter(traceFile));
					} catch (IOException e) {
						logger.error("Failed to open trace file: " + e.getMessage());
						close();
						continue;
					}
			}//Done writing current messages, loop around to wait for more 
			try {
				writer.flush();
			} catch (IOException e) {}
		}
		if (writer != null)
			try {
				writer.close();
			} catch (IOException e) {}
	}
}
