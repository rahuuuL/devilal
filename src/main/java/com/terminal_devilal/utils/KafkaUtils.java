package com.terminal_devilal.Utils;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Service;

import com.terminal_devilal.controllers.DataGathering.Model.AverageTrueRange;
import com.terminal_devilal.controllers.DataGathering.Model.PriceDeliveryVolume;
import com.terminal_devilal.controllers.DataGathering.Service.AverageTrueRangeService;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;

@Service
public class KafkaUtils {

	private AverageTrueRangeService averageTrueRangeService;

	public KafkaUtils(AverageTrueRangeService averageTrueRangeService) {
		this.averageTrueRangeService = averageTrueRangeService;
	}

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

	public void produceToStockTopic(String stockName, PriceDeliveryVolume message) {
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);

		try (KafkaProducer<String, PriceDeliveryVolume> producer = new KafkaProducer<>(props)) {
			ProducerRecord<String, PriceDeliveryVolume> record = new ProducerRecord<>(stockName, null, message);
			producer.send(record);
			System.out.println("Produced to topic: " + stockName);
		}
	}

	public void consumePDVforATR(String stockName) {
		Properties props = new Properties();
		props.put("bootstrap.servers", "localhost:9092");
		props.put("group.id", "stock-consumer-group");
		props.put("key.deserializer", StringSerializer.class);
		props.put("value.deserializer", KafkaAvroDeserializer.class);
		props.put("auto.offset.reset", "earliest");

		// Required when using KafkaAvroDeserializer
		props.put("schema.registry.url", "http://localhost:8081");

		// If you are using specific Avro classes
		props.put("specific.avro.reader", "true");

		try (KafkaConsumer<String, PriceDeliveryVolume> consumer = new KafkaConsumer<>(props)) {

			List<AverageTrueRange> atrList = new LinkedList<AverageTrueRange>();
			String lastTicker = null;

			consumer.subscribe(Collections.singletonList(stockName));

			while (true) {
				ConsumerRecords<String, PriceDeliveryVolume> records = consumer.poll(Duration.ofMillis(1000));
				for (ConsumerRecord<String, PriceDeliveryVolume> record : records) {

					// Initializing the Last ticker value
					if (lastTicker == null) {
						lastTicker = record.value().getTicker();
					}
					
					// If ticker changes, save the list and reset
					if (!lastTicker.equals(record.value().getTicker())) {
						this.averageTrueRangeService.saveAllATR(atrList);
						atrList.clear();
						lastTicker = record.value().getTicker();
					}

					// Calc ATR
					double trueRange = this.averageTrueRangeService.calculateTrueRange(record.value().getHigh(),
							record.value().getLow(), record.value().getPrevoiusClosePrice());

					// Make ATR object and add to list
					AverageTrueRange averageTrueRange = new AverageTrueRange(record.value().getTicker(),
							record.value().getDate(), trueRange);
					atrList.add(averageTrueRange);
				}
				
				//Ensure last ticker's data is saved
				if (lastTicker != null && !atrList.isEmpty()) {
					this.averageTrueRangeService.saveAllATR(atrList);
				}
			}
		}
	}

}
