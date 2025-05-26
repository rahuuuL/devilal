package com.terminal_devilal.Utils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaUtils {

	public void createTopic(String topicName) {
	    Properties config = new Properties();
	    config.put("bootstrap.servers", "localhost:9092");

	    try (AdminClient admin = AdminClient.create(config)) {
	        NewTopic newTopic = new NewTopic(topicName, 1, (short) 1);
	        admin.createTopics(Collections.singletonList(newTopic));
	        System.out.println("Created topic: " + topicName);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	public void produceToStockTopic(String stockName, String message) {
	    Properties props = new Properties();
	    props.put("bootstrap.servers", "localhost:9092");
	    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
	    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

	    try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
	        ProducerRecord<String, String> record = new ProducerRecord<>(stockName, null, message);
	        producer.send(record);
	        System.out.println("Produced to topic: " + stockName);
	    }
	}
	
	public void consumeFromStockTopic(String stockName) {
	    Properties props = new Properties();
	    props.put("bootstrap.servers", "localhost:9092");
	    props.put("group.id", "stock-consumer-group");
	    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
	    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
	    props.put("auto.offset.reset", "earliest");

	    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
	        consumer.subscribe(Collections.singletonList(stockName));

	        while (true) {
	            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
	            for (ConsumerRecord<String, String> record : records) {
	                System.out.printf("Consumed from %s: %s%n", stockName, record.value());
	            }
	        }
	    }
	}


}
