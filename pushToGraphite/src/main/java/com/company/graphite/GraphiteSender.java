package com.company.graphite;

import com.company.Config;
import com.company.kafka.KafkaObservationData;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.python.core.*;
import org.python.modules.cPickle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class GraphiteSender {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void send(ConsumerRecords<String, String> records) {
        try (Socket socket = new Socket(Config.graphiteHostName(), Config.graphitePort()))  {
            PyList list = new PyList();

            records.forEach(record -> {
            	addFloatMetric(record, list, record.value());
            });

            PyString payload = cPickle.dumps(list);
            byte[] header = ByteBuffer.allocate(4).putInt(payload.__len__()).array();

            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(header);
            outputStream.write(payload.toBytes());
            outputStream.flush();

        } catch (IOException e) {
            logger.error("Exception thrown writing data to graphite: " + e);
        }
    }
    
    private void addFloatMetric(ConsumerRecord<String, String> record, List list, String value) {
    	if (value == null) {
    		return;
    	}
    	
    	PyString metricName = new PyString("numOfRecords");
    	String[] values = value.split("_");
    	PyLong timestamp = new PyLong(Long.parseLong(values[0]));
    	PyInteger metricValue = new PyInteger(Integer.parseInt(values[1]));
    	PyTuple metric = new PyTuple(metricName, new PyTuple(timestamp, metricValue));
    	logMetric(metric);
    	list.add(metric);
    }

    private void logMetric(PyTuple metric) {
        logger.info("Added metric: " + metric.toString());
    }
}
