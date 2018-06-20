/*----------------------------------------------------*/
/* Licensed Materials - Property of Blueworx          */
/*                                                    */
/* SAMPLE                                             */
/*                                                    */
/* (c) Copyright Blueworx. 2018. All Rights Reserved. */
/*----------------------------------------------------*/

package com.blueworx.samples.cdr.kafka;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

import com.blueworx.samples.cdr.kafka.CallDetailRecord;
import com.blueworx.samples.cdr.kafka.MRCPCDR;
import com.blueworx.samples.cdr.kafka.XMLCDR;

public class App {
    public static final String _copyright = "Licensed Materials - Property of Blueworx. Copyright (c) Blueworx. 2018 All Rights Reserved. ";

    // Configure this variable to set the frequency that we commit the Kafka offset to the Kafka broker
    private static final int BATCH_COMMIT_FREQUENCY = 1;

    // Default Apache Kafka broker connection information
    private static final String DEFAULT_KAFKA_BROKERS = "localhost:9092";
    private static final String DEFAULT_KAFKA_CONSUMER_GROUP_ID = "BWCDRConsumerSample";
    private static final String DEFAULT_KAFKA_TOPIC = "blueworx.cdr";
    private static final int DEFAULT_KAFKA_POLL_RATE = 1000;

    // Default PostgreSQL database connection information
    private static final String DEFAULT_POSTGRES_URL = "jdbc:postgresql://localhost:5432/bw_sample_cdr";
    private static final String DEFAULT_POSTGRES_USERNAME = "bwsamples";
    private static final String DEFAULT_POSTGRES_PASSWORD = "md5bfd4102f654bb7059d11950eccb7b522";

    // Kafka Consumer and PostgreSQL Database connection objects
    private KafkaConsumer<String, CallDetailRecord> consumer;
    private Connection dbConnection;

    public static void main(String[] args) {
        // Create a new instance of this app
        final App kafkaCDRConsumer = new App();

        // Set up the Apache Kafka broker connection and PostgreSQL databases connection information
        // A possible extension could be to load these options from a Java properties file, or
        // load from the args...

        // Kafka
        String brokers = DEFAULT_KAFKA_BROKERS;
        String groupId = DEFAULT_KAFKA_CONSUMER_GROUP_ID;
        String topic = DEFAULT_KAFKA_TOPIC;
        int pollRate = DEFAULT_KAFKA_POLL_RATE;

        // PostgreSQL
        String db_url = DEFAULT_POSTGRES_URL;
        String db_user = DEFAULT_POSTGRES_USERNAME;
        String db_pass = DEFAULT_POSTGRES_PASSWORD;

        // Configure the app
        kafkaCDRConsumer.configure(brokers, groupId, db_url, db_user, db_pass);

        // Get the current thread. We use this in the Shutdown Hook below to rejoin the main thread once all
        // shutdown processing is complete
        final Thread mainThread = Thread.currentThread();

        // Registering a shutdown hook so we can exit cleanly (including closing the consumer connection to the
        // Apache Kafka broker, and the database connection to PostgreSQL)
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Starting exit...");
                // Note that shutdownhook runs in a separate thread, so the only thing we can safely do to a consumer is wake it up
                if (kafkaCDRConsumer != null && kafkaCDRConsumer.consumer != null)
                    kafkaCDRConsumer.consumer.wakeup();

                // Now we have shutdown the consumer, close the connection to the database
                try {
                    if (kafkaCDRConsumer != null && kafkaCDRConsumer.dbConnection != null)
                        kafkaCDRConsumer.dbConnection.close();
                    System.out.println("Closed database connection");

                    // Rejoin the main thread and allow the application to close
                    mainThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (SQLException sqle) {
                    sqle.printStackTrace();
                }
            }
        });

        try {
            // Subscribe to the topic
            kafkaCDRConsumer.consumer.subscribe(Collections.singletonList(topic));

            // Start processing loop. This will continue to loop until ctrl-c (or similar) is
            // invoked, triggering the shutdown hook to cleanup on exit
            while (true) {
                // Start polling the Kafka topic
                ConsumerRecords<String, CallDetailRecord> records = kafkaCDRConsumer.consumer.poll(pollRate);

                // For each assigned partition, process the messages
                for (TopicPartition partition : records.partitions()) {
                    List<ConsumerRecord<String, CallDetailRecord>> partitionRecords = records.records(partition);
                    // Create map to keep track of message offsets we have processed... this is used when committing message offsets
                    Map<TopicPartition, OffsetAndMetadata> partitionOffset = new HashMap<TopicPartition, OffsetAndMetadata>();
                    // Count number of records processed... used in conjunction with BATCH_COMMIT_FREQUENCY to control how often
                    // during batch processing we commit our offset
                    int count = 1;

                    // Loop through each record for the batch we are currently processing
                    for (ConsumerRecord<String, CallDetailRecord> record : partitionRecords) {
                        // Using CDRDeserialiser we automatically create a CallDetailRecord object to hold the data
                        CallDetailRecord cdr = record.value();

                        // Print basic information
                        System.out.printf("offset = %d, key = %s\n", record.offset(), record.key());
                        System.out.println(formatHeader("CDR record received") + cdr.toString());
                        try {
                            String sql = "";
                            if (cdr.getType().equals("Call")) {
                                // Process Call Detail Record...

                                // Construct the SQL statement to insert data into database
                                sql = "INSERT INTO \"CallRecords\" (callIndexFull, callIndex, accountID, callStart, callEnd, duration, direction, fromAddress, toAddress, ani, dnis, terminationReason, ttsSessionDuration, ttsStreamDuration, asrSessionDuration, asrStreamDuration, applicationID, bvrMachine, xmlInitial, xmlTerminate) VALUES ('" + cdr.getCallIndexFull() + "', '" + cdr.getCallIndex() + "', '" + cdr.getAccountID() + "', '" + cdr.getStart() + "', '" + cdr.getEnd() + "', " + cdr.getDuration() + ", '" + cdr.getDirection() + "', '" + cdr.getFrom() + "', '" + cdr.getTo() + "', '" + cdr.getAni() + "', '" + cdr.getDnis() + "', '" + cdr.getTerminationReason() + "', " + cdr.getTts().getSessionDuration() + ", " + cdr.getTts().getStreamDuration() + ", " + cdr.getAsr().getSessionDuration() + ", " + cdr.getAsr().getStreamDuration() + ", '" + cdr.getApplicationID() + "', '" + cdr.getBvrMachine() + "', '" + cdr.getXml().getInitial() + "', '" + cdr.getXml().getTerminate() + "');";

                                System.out.println(formatHeader("SQL INSERT statement") + sql);

                                // Create required PostgreSQL object and execute SQL statement
                                Statement statement = kafkaCDRConsumer.dbConnection.createStatement();
                                int rc = statement.executeUpdate(sql);

                                System.out.println("\nPostgreSQL return code: [" + rc + "]\n");
                            } else if (cdr.getType().equals("Conference")) {
                                // Process Conference Detail Record...
                                // A possible extension to this would be to incorporate all the SQL INSERT statements below into
                                // a single PostgreSQL transaction

                                // Construct the SQL statement to insert data into database
                                sql = "INSERT INTO \"ConferenceRecords\" (conferenceId, duration) VALUES ('" + cdr.getConferenceId() + "', " + cdr.getDuration() + ");";

                                System.out.println(formatHeader("SQL INSERT statement") + sql);

                                // Create required PostgreSQL object and execute SQL statement
                                Statement statement = kafkaCDRConsumer.dbConnection.createStatement();
                                int rc = statement.executeUpdate(sql);

                                System.out.println("\nPostgreSQL return code: [" + rc + "]\n");

                                for (ConferenceMember member : cdr.getMembers()) {
                                    // Construct the SQL statement to insert data into database
                                    sql = "INSERT INTO \"ConferenceMembers\" (conferenceId, callIndexFull, callIndex, participantStart, participantEnd) VALUES ('" + cdr.getConferenceId() + "', '" + member.getCallIndexFull() + "', '" + member.getCallIndex() + "', '" + member.getStart() + "', '" + member.getEnd() + "');";

                                    System.out.println(formatHeader("SQL INSERT statement") + sql);

                                    // Create required PostgreSQL object and execute SQL statement
                                    statement = kafkaCDRConsumer.dbConnection.createStatement();
                                    rc = statement.executeUpdate(sql);

                                    System.out.println("\nPostgreSQL return code: [" + rc + "]\n");
                                }
                            }

                            // Record the current offset for the record we just processed
                            partitionOffset.put(partition, new OffsetAndMetadata(record.offset()+1));

                            // Check if we want to commit at this point in processing the current batch of messages
                            if (count % BATCH_COMMIT_FREQUENCY == 0) {
                                System.out.println("Committing (async) offset at position:" + partitionOffset.get(partition).offset());
                                // You can sync or async commit here... difference is whether we block processing or not until we get
                                // a confirmation the offset has been committed.
                                kafkaCDRConsumer.consumer.commitAsync(partitionOffset, null);
                            }
                            count++;
                        } catch (SQLException sqle) {
                          	sqle.printStackTrace();
                        }
                    }
                    // At the end of the current batch, commit that we have processed the records.
                    System.out.println("Committing (sync) offset at position:" + partitionOffset.get(partition).offset());
                    kafkaCDRConsumer.consumer.commitSync(partitionOffset);
                }
            }
        } catch (WakeupException we) {
            // Ignore this exception. This is invoked if the user closes the application
        } finally {
            kafkaCDRConsumer.consumer.close();
            System.out.println("Closed consumer");
        }
    }

    // Consfiguration method to configure the Apache Kafka consumer connection, and the PostgreSQL database
    // connection information
    private void configure(String brokers, String groupId, String db_url, String db_user, String db_pass) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);    // List of Apache Kafka brokers
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);             // Consumer group ID - each consumer group will only process a single message once
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");   // Turn auto commit off - we want to control when we commit our position in the topic
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer"); // use StringDeserializer for key - this will probably be null anyway, but this matches the producer
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "com.blueworx.samples.cdr.kafka.CDRDeserialiser");         // use a customer CDRDeserialiser for the message - this automatically creates a CallDetailRecord object instead of giving us JSON
        consumer = new KafkaConsumer<String, CallDetailRecord>(props);  // Because we use a customer CDRDeserialiser, we specify our consumer as <String, CDRDeserialiser>

        // Create PostgreSQL database connection
        try {
            Class.forName("org.postgresql.Driver");     // Load driver
            dbConnection = DriverManager.getConnection(db_url, db_user, db_pass);   // Create connection

            // Example of getting database connection metadata
            DatabaseMetaData dmd = dbConnection.getMetaData();
            String url = dmd.getURL();
            System.out.println("Opened database successfully: " + url);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
    }

    // Small helper method to format the header of logging output
    private static String formatHeader(String header) {
        return "\n" + header + ":\n\n";
    }
}
