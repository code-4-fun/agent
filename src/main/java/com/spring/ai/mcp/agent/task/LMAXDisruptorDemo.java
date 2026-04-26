package com.spring.ai.mcp.agent.task;

import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import com.lmax.disruptor.EventTranslatorOneArg;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LMAXDisruptorDemo {

    @Data
    static class Event {
        private long sequence, timestamp;
        private int producerId;
        private int value;
    }

    @Slf4j
    static class Producer implements Runnable {
        private final com.lmax.disruptor.RingBuffer<Event> ringBuffer;
        private final int producerId;
        private final int eventsToPublish;

        Producer(com.lmax.disruptor.RingBuffer<Event> ringBuffer, int producerId, int eventsToPublish) {
            this.ringBuffer = ringBuffer;
            this.producerId = producerId;
            this.eventsToPublish = eventsToPublish;
        }

        @Override
        public void run() {
            EventTranslatorOneArg<Event, Integer> translator = (event, sequence, value) -> {
                event.setSequence(sequence);
                event.setTimestamp(System.currentTimeMillis());
                // producerId captured from outer scope
                event.setProducerId(producerId);
                event.setValue(value);
            };

            for (int i = 0; i < eventsToPublish; i++) {
                ringBuffer.publishEvent(translator, i);
                log.info("Producer-{} published event payload={}", producerId, i);
            }
        }
    }

    @Slf4j
    static class ConsumerA implements EventHandler<Event> {
        @Override
        public void onEvent(Event event, long sequence, boolean endOfBatch) {
            log.info("[A] Processing event seq={} prod={} val={}", event.getSequence(), event.getProducerId(), event.getValue());
        }
    }

    @Slf4j
    static class ConsumerB implements EventHandler<Event> {
        @Override
        public void onEvent(Event event, long sequence, boolean endOfBatch) {
            log.info("[B] Processing event seq={} prod={} val={}", event.getSequence(), event.getProducerId(), event.getValue());
        }
    }

    @Slf4j
    static class ConsumerC implements EventHandler<Event> {
        private final CountDownLatch latch;

        ConsumerC(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onEvent(Event event, long sequence, boolean endOfBatch) {
            log.info("[C] Sequential consumer (after A & B) seq={} prod={} val={}", event.getSequence(), event.getProducerId(), event.getValue());
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    public static void main(String[] args) {
        // Demonstration parameters
        final int producers = 3;
        final int eventsPerProducer = 10;
        final int totalEvents = producers * eventsPerProducer;
        final int bufferSize = 64;

        // Use MULTI producer type for multiple concurrent publishers
        var disruptor = new Disruptor<>(Event::new, bufferSize, Executors.defaultThreadFactory(), ProducerType.MULTI, new BusySpinWaitStrategy());

        // Latch to wait until all events have traversed the consumer graph
        var latch = new CountDownLatch(totalEvents);

        // Consumers: A and B run in parallel, C runs after both A and B have processed the event
        var consumerA = new ConsumerA();
        var consumerB = new ConsumerB();
        var consumerC = new ConsumerC(latch);

        // Mixed dependency graph: A and B in parallel, then C sequentially after them
        disruptor.handleEventsWith(consumerA, consumerB).then(consumerC);

        var ringBuffer = disruptor.start();

        // Executor to run multiple producer threads
        ExecutorService producersPool = Executors.newFixedThreadPool(producers);
        for (int p = 0; p < producers; p++) {
            producersPool.submit(new Producer(ringBuffer, p, eventsPerProducer));
        }

        // Shutdown producers pool after submitting
        producersPool.shutdown();
        try {
            // wait for producers to finish
            if (!producersPool.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Producers did not terminate in time");
                producersPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Wait for all events to be processed by the terminal consumer C
        try {
            boolean allProcessed = latch.await(10, TimeUnit.SECONDS);
            log.info("All processed: {} (expect true when all {} events processed)", allProcessed, totalEvents);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for latch", e);
        }

        // Clean shutdown of disruptor
        disruptor.shutdown();

    }

}
