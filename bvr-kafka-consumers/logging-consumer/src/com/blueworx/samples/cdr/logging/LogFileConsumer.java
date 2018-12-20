/** Licensed materials property of Blueworx
 *
 * SAMPLE
 *
 * (c) Copyright Blueworx. 2018. All Rights Reserved.
 */
package com.blueworx.samples.cdr.logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.log4j.Logger;

public class LogFileConsumer {

	public static final String APP_TOPIC = "blueworx.application";
	public static final String ERR_TOPIC = "blueworx.error";
	public static final String TRC_TOPIC = "blueworx.trace";
	
	// Default Apache Kafka broker connection information
	public static final String DEFAULT_KAFKA_CONSUMER_GROUP_ID = "Blueworx-Logging";
	public static final String DEFAULT_KAFKA_POLL_RATE = "1000"; // This is a string because it is used as a default when loading from properties file

	private static final Logger logger = Logger.getLogger("log4j.rootLogger");//This logger is for writing out errors in running this consumer, not the BVR logging
	private KafkaConsumer<String, TraceMessage> consumer;//Our connection to the kafka broker
	private boolean running;
	private Queue<TraceMessage> appMessages, errMessages, trcMessages;//We'll split the messages up according to type and send them to these queues
	private ApplicationMessageLogger appWorker;//This worker will handle all blueworx.application messages
	private TraceMessageLogger errorWorker, traceWorker; //and these workers will handle blueworx.error and blueworx.trace

	public LogFileConsumer() throws IOException {
		// Set up the Apache Kafka broker connection
		String propertiesFile = System.getProperty("consumer.properties");
		if (propertiesFile == null)
			propertiesFile = "config" + File.separator + "logging-consumer.properties";
		
		// Get properties from properties file
		Properties props = getProperties(propertiesFile);
		if (props == null) {
			props = new Properties();
		}

		// Kafka properties and file handling properties
		int pollRate = Integer.parseInt(props.getProperty("poll_rate", DEFAULT_KAFKA_POLL_RATE));
		long filesPerCallLogSubDirectory = Long.parseLong(props.getProperty("files_per_directory", "300"));
		long fileLimit = Long.parseLong(props.getProperty("trace_file_limit", "2048"))*1000000L;
		long fileCount = Long.parseLong(props.getProperty("trace_file_count", "5"));
		long errorLimit = Long.parseLong(props.getProperty("error_file_limit", "2048"))*1000000L;
		long errorCount = Long.parseLong(props.getProperty("error_file_count", "5"));
		File logDirectory = new File(props.getProperty("output_directory", "logs"));
		logDirectory.mkdirs();
		
		//Start up the handler threads
		appMessages = new LinkedList<TraceMessage>();
		appWorker = new ApplicationMessageLogger(appMessages, logDirectory, logger, filesPerCallLogSubDirectory);
		errMessages = new LinkedList<TraceMessage>();
		errorWorker = new TraceMessageLogger("Error", errMessages, logDirectory, logger, errorLimit, errorCount);
		trcMessages = new LinkedList<TraceMessage>();
		traceWorker = new TraceMessageLogger("Trace", trcMessages, logDirectory, logger, fileLimit, fileCount);
		
		consumer = new KafkaConsumer<String, TraceMessage>(props);
		//Here we specify that we are interested in all types of logging.
		//If BVR is not configured to use Kafka for the type of logging then it'll never reach us.
		//If you have not set up the topic in the Kafka broker, please remove it the topics ArrayList here
		ArrayList<String> topics = new ArrayList<String>();
		topics.add(APP_TOPIC);
		topics.add(ERR_TOPIC);
		topics.add(TRC_TOPIC);
		consumer.subscribe(topics);

		// Get the current thread. We use this in the Shutdown Hook below to rejoin the main thread once all shutdown processing is complete
		final Thread mainThread = Thread.currentThread();
		running = true;
		// Registering a shutdown hook so we can exit cleanly (including closing the consumer connection to the Apache Kafka broker
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				logger.info("Starting exit...");

				running = false;
				// Note that shutdownhook runs in a separate thread, so the only thing we can safely do to a consumer is wake it up
				KafkaConsumer<String, TraceMessage> consumerLocal = consumer;
				//This avoids any potential race conditions between the check and the wakeup
				if (consumerLocal != null)
					consumerLocal.wakeup();

				try {
					mainThread.join();
				} catch (InterruptedException e) {
					logger.warn(e.getMessage());
				}
			}
		});
		
		//Main thread loop
		while (running) {
			ConsumerRecords<String, TraceMessage> records = consumer.poll(pollRate);
			//Poll will block until it has data, so the while condition might now be false
			if (!running)
				continue;
			//We now need to process the records we got from polling the kafka broker.
			//We want the three different topics to go to different files, like they do in the usual BVR trace
			//APP_TOPIC trace will go to the callLogs subdirectory, into the appropriate callID's log file
			//ERR_TOPIC trace will go to the error_X.out files
			//TRC_TOPIC trace will go to the trace_X.out files
			//You could recode this to do whatever you want, of course - and if you don't have a use for one or
			// more of these then simply don't setup KAFKA or BOTH trace options in the BVR config.
			for (TopicPartition partition : records.partitions()) {
				String topic = partition.topic(); //Determine destination by topic
				if (topic.equals(APP_TOPIC)) {
					for (ConsumerRecord<String, TraceMessage> record : records.records(partition)) {
						TraceMessage message = record.value();
						appMessages.add(message);
					}
					synchronized(appMessages) {
						appMessages.notifyAll();
					}
				} else if (topic.equals(ERR_TOPIC)) {
					for (ConsumerRecord<String, TraceMessage> record : records.records(partition)) {
						TraceMessage message = record.value();
						errMessages.add(message);
					}
					synchronized (errMessages) {
						errMessages.notifyAll();
					}
				} else if (topic.equals(TRC_TOPIC)) {
					for (ConsumerRecord<String, TraceMessage> record : records.records(partition)) {
						TraceMessage message = record.value();
						trcMessages.add(message);
					}
					synchronized (trcMessages) {
						trcMessages.notifyAll();
					}
				} else logger.error("Could not resolve topic partition " + topic);
			}
		}
		//Shut down the other threads
		appWorker.close();
		errorWorker.close();
		traceWorker.close();
	}

	public static final void main(String[] args) {
		LogFileConsumer lfc = null;
		try {
			lfc = new LogFileConsumer();
		} catch (Exception e) {
			System.err.println("Terminal Error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (lfc != null) {
				lfc.close();
			}
		}
	}
	
	private void close() {
		running = false;
		synchronized (consumer) {
			consumer.notifyAll();
		}
		consumer.close();
	}

	/*
	 * Loads up the properties and supplies the extra ones we need
	 */
	private Properties getProperties(String propertiesFile) {
		Properties props = new Properties();

		if (propertiesFile != null) {
			InputStream input = null;
			try {
				input = new FileInputStream(propertiesFile);
				props.load(input);
			} catch (FileNotFoundException fnfe) {
				logger.warn("Properties file load failed. FileNotFoundException when attempting to open properties file. Check the file specified in configuration exists. Exception message: " + fnfe.getMessage());
			} catch (SecurityException se) {
				logger.error("Properties file load failed. SecurityException when attempting to open properties file. Check BVR has access to read the specified properties file. Exception message: " + se.getMessage());
			} catch (IOException ioe) {
				logger.error("Properties file load failed. IOException when attempting to load properties file. Exception message: " + ioe.getMessage());
				props.clear();
			} catch (IllegalArgumentException iae) {
				logger.error("Properties file load failed. IllegalArgumentException when attempting to load properties file. Check format of file. Exception message: " + iae.getMessage());
				props.clear();
			} finally {
				if (input != null) {
					try {
						input.close();
					} catch (IOException ioe) {
						logger.warn("IOException when attempting to close properties file. Exception message: " + ioe.getMessage());
					}
				}
			}
		}
		
		//These are necessary to get the messages in the correct format
		props.put(ConsumerConfig.GROUP_ID_CONFIG, props.getOrDefault(ConsumerConfig.GROUP_ID_CONFIG, DEFAULT_KAFKA_CONSUMER_GROUP_ID));             // Consumer group ID - each consumer group will only process a single message once
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer"); // use StringDeserializer for key - this will probably be null anyway, but this matches the producer
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "com.blueworx.util.consumer.logging.TraceMessageDeserialiser"); // We use our own deserialiser class

		return props;
	}
}
