# Sample Apache Kafka consumer writing out trace and log files mirroring BVR trace and logs

This sample application shows how you can create an Apache Kafka consumer implemented in java to read and process log and trace information from Blueworx Voice Response (BVR).

The app polls the kafka topics blueworx.application, blueworx.error and blueworx.trace, formats entries into trace lines using the same formatting rules as BVR, and writes out those lines to the appropriate output files as follows:

**blueworx.application** - all lines from the same callIndex are written out to [outputDir]/callLogs/X/Y.out, where X and Y are escalating numbers that correspond to the order calls were received in. The X subdirectory structure allows the system to cope with the maximum files in a directory limit and has no significance other than sequential ordering.

**blueworx.error** - written out to [outputDir]/error\_X.out files, starting at X=0 and going up to the error file count limit before wrapping back to error_0.out. Each file will have a maximum size in MB controlled by the error file limit property.

**blueworx.trace** - written out to [outputDir]/trace\_X.out files, starting at X=0 and going up to the trace file count limit before wrapping back to trace_0.out. Each file will have a maximum size in MB controlled by the trace file limit property.

As you can probably guess from the above, the **blueworx.error** and **blueworx.trace** streams use the same code handler class.

## Setting Properties for the Sample
The code expects to run such that config/logging-consumer.properties is valid relevant to the current directory. All properties have default values if not set, but you will at minimum need to specify bootstrap.servers. For more information on Apache Kafka properties, please see [the Apache Kafka Documentation.](https://kafka.apache.org/documentation/#newconsumerconfigs)
The following properties can be set in the logging-consumer.properties file to alter behaviour of the existing code or are otherwise set in the sample code:

**output_directory** - the output directory to write all the files into. 

**bootstrap.servers** - Value used by Apache Kafka to locate the broker from which to get all the topics. This is set to localhost by default, so in the likely scenario that this is on a different machine to BVR you will need to edit this property.

**files_per_directory** - The maximum number of files that the blueworx.application handler will write into a callLogs subdirectory before incrementing and creating a new subdirectory.

**trace_file_limit and error_file_limit** - The size in MegaBytes of data that the trace/error handler will write before moving on to the next file. Slightly more data may be written than this as we always write out each message in its entirety, checking the limit between trace messages.

**trace_file_count and error_file_count** - The maximum number of trace/error files to write out before wrapping back around to the 0 file label.

**key.deserializer and value.deserializer** - These values are not read from the properties file but instead set in the properties internally after reading in the other property values. These tell BVR how to decode entries that it reads from the Apache Kafka topics

**group.id** - an Apache Kafka property that identifies what consumer group this program belongs to - we set this to LogFileConsumer.DEFAULT\_CONSUMER\_GROUP\_ID if it is not supplied in the properties file.

**poll_rate** - The rate at which Apache KAfka is polled for messages on our topics. If not supplied in the properties file we use LogFileConsumer.DEFAULT\_KAFKA\_POLL\_RATE

## Code Overview
* **LogFileConsumer** - The main class of our consumer, creating the kafka consumer; parsing the properties file; polling the consumer and dispatching the messages to the relevant handlers to be written out
* **TraceMessage** - A data class that contains the various fields found in the serialized trace message objects from the kafka topics. If a value is not set it will be set to the empty String - some trace messages may not know their callIndex value, for example. Also contains a method to format itself into a BVR trace line
* **TraceMessageDeserialiser** - Deserialises the serialised messages from the kafka topics into TraceMessage objects. Note that the **blueworx.cdr** topic uses a different serialisation - see the CDR-Postgres consumer sample for more details.
* **ApplicationMessageLogger** - This class has a queue onto which it receives TraceMessages and writes them out to individual files based on the call index of the message, as detailed above.
* **TraceMessageLogger** - This class is used for both error and trace file logging, one instance handling a different queue of messages just like with ApplicationMessageLogger. It writes out to error\_X.out and trace\_X.out files as detailed above.

## Modifying the Code
This sample is intended to give you an easy jumping-off point to your own handling of BVR logging and trace. You might not want the overhead of sending all the **blueworx.trace** information over kafka and only be interested in the other topics - to handle that you can disable it in BVR's [log] config and/or remove this line from LogFileConsumer:

`		topics.add(TRC_TOPIC);`

You don't need to disable the handling of the topic in the main loop of LogFileConsumer if you don't want to though, as no messages will reach the consumer with that topic label.
The handlers can be extended or replaced as needed - the queue mechanism should let you write your own handler fairly easily, at which point you can replace the relevant Logger.

## Building the Code
To build the code in eclipse, you can simply load the project and java will build into the build/classes folder automatically. If you are working on a server, try using this command in the project directory: 

`ant -f build.xml -v -Dbasedir="." -Dbuild_home="." -Djava\_home="*yourJavaHomeHere*"`

This should result in logging-consumer-1.0.jar being created in the bin directory of the project.
To run the program, use the following commands, replacing the first line:

`CONSUMER_HOME=/the/directory/of/the/consumer/project/goes/here`

`java -Dconsumer.properties="$CONSUMER_HOME/config/logging-consumer.properties" -Dlog4j.configuration=file:$CONSUMER_HOME/config/log4j.properties -cp $CONSUMER_HOME/bin/logging-consumer-1.0.jar:$CONSUMER_HOME/lib/*`

## License
This project is licensed under [Apache License Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)