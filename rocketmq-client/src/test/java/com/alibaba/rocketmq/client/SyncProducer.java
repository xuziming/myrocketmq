package com.alibaba.rocketmq.client;

import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.common.message.Message;

public class SyncProducer {

	public static void main(String[] args) throws Exception {
		// Instantiate with a producer group name.
		DefaultMQProducer producer = new DefaultMQProducer("please_rename_unique_group_name");
		// Specify name server addresses.
		producer.setNamesrvAddr("localhost:9876");
		// Launch the instance.
		producer.start();
		for (int i = 0; i < 100; i++) {
			// Create a message instance, specifying topic, tag and message body.
			Message msg = new Message("TopicTest", "TagA", ("Hello RocketMQ " + i).getBytes("UTF-8"));
			// Call send message to deliver message to one of brokers.
			SendResult sendResult = producer.send(msg);
			System.out.printf("%s%n", sendResult);
		}
		// Shut down once the producer instance is not longer in use.
		producer.shutdown();
	}

}