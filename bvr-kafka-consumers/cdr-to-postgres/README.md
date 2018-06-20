# Sample Apache Kafka consumer outputting BVR CDRs to PostgreSQL

This sample application shows how you can create an Apache Kafka consumer, implemented in Java, to read and process Call and Conference Detail Record messages output by Blueworx Voice Response.

The app will poll the Kafka topic (interval configurable), construct a CallDetailRecord object (using the CDRDeserialiser), and then place the data on the record into a PostgreSQL database.

## Pre-requisites

* **Blueworx Voice Response V7.7.1** - you will need to configure BVR to output CDRs to a Kafka topic. For more information refer to the [Blueworx Voice Response documentation](http://docs.blueworx.com/BVR/InfoCenter/V7/Linux/help/topic/com.ibm.wvrlnx.config.doc/lnx_operation_vr_cdr.html?cp=1_5_1_5 "Blueworx Documentation")
* **PostgreSQL 9.6** - the sample outputs data into a PostgreSQL database, and has been tested with version 9.6.
* **Maven** - this sample uses Maven to build and package the sample as a Java jar file. Refer to the [Maven documentation](https://maven.apache.org/ "Apache Maven documentation") for installation instructions

**Note**: This sample was created using Apache Kafka version 1.1, and this version is specified in the pom.xml file. If the BVR version is upgraded to use a new version of Apache Kafka, this version may need to be updated.

## Customising the sample application

The consumer can be built and run on either a) the same machine as BVR, or b) a different machine that has network access to the BVR machine.

If you choose to run on a different machine, you will need to customise the consumer. There are also a few other variables you can customise, depending on your needs. The configurable options are:
* `DEFAULT_KAFKA_BROKERS` - static string of the list of brokers in your Kafka cluster. This does not have to be an exhaustive list, usually just a couple of brokers in the cluster. This maps to the **ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG** / **bootstrap.servers** configuration property. The format of this string is `host1:port1,host2:port2,...`
* `DEFAULT_KAFKA_CONSUMER_GROUP_ID` - static string for the consumer group ID. Each consumer group will only process a message once. This variable maps to the **ConsumerConfig.GROUP_ID_CONFIG** / **group.id** configuration property
* `DEFAULT_KAFKA_POLL_RATE` - integer value for the poll rate (in milliseconds) of the consumer to the Kafka broker to check for new messages.
* `DEFAULT_POSTGRES_URL` - JDBC URL for the PostgreSQL server
* `DEFAULT_POSTGRES_USERNAME` and `DEFAULT_POSTGRES_PASSWORD` - customise the username and password used to connect to PostgreSQL. This defaults to the user ID created in the `createDatabase.sql` script used to create the database.
* `BATCH_COMMIT_FREQUENCY` - by default, the sample will commit our offset in the topic after processing each message. To reduce the frequency of this alter this variable to the frequency you want to commit. The sample will always commit after processing a batch.

For more information about any of the Kafka variables, refer to the [Apache Kafka documentation](https://kafka.apache.org/documentation/#newconsumerconfigs "Apache Kafka documentation - Consumer configuration parameters").

## Installing the sample database

The data produced by this application is output into a PostgreSQL database `bw_sample_cdr`. To enable the sample to work, this database needs to be created, the samples username created, and the table structures created.

The `sql/createDatabase.sql` SQL file has been supplied to configure the database. Work with your database administrator to run this file.

## Building

To build the application, use the Maven build system. This will download any dependencies (which may take a while on first run) required by the consumer. To build the project, from the project root directory run the following:

```bash
mvn clean compile assembly:single
```

This will create a standalone JAR file. This JAR file will include all required dependencies.

**Note**: on some platforms, the JAR file created will be platform specific. It is recommended that you build on the platform you will run the application on.

## Running

As mentioned previously, the jar file created will include all dependencies. As such, to run, execute the following command from the application root.

```bash
java -jar target/kafka-postgres-cdr-consumer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## License
This project is licensed under [Apache License Version 2.0](LICENSE)
