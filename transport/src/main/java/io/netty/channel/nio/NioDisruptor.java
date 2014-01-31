package io.netty.channel.nio;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.channels.spi.SelectorProvider;
import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;


public class NioDisruptor extends NioEventLoop {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NioDisruptor.class);

    private final Executor executor;

    public NioDisruptor(NioEventLoopGroup parent, Executor executor, SelectorProvider selectorProvider) {
        super(parent, executor, selectorProvider);

        this.executor = executor;
    }

    @Override
    protected Queue<Runnable> newTaskQueue() {
        return new DisruptorQueueWrapper(executor);
    }

    static class DisruptorQueueWrapper<E> extends AbstractQueue<E> {

        private final Disruptor<ValueEvent<E>> disruptor;
        private final RingBuffer<ValueEvent<E>> ringBuffer;
        private static final int SIZE = 1 << 10; // 1 << N == 2^N
        private final AtomicLong nextRead;

        public DisruptorQueueWrapper(Executor executor) {
            disruptor = new Disruptor<ValueEvent<E>>(new EventFactory<ValueEvent<E>>() {
                @Override
                public ValueEvent<E> newInstance() {
                    return new ValueEvent<E>();
                }
            }, SIZE, executor, ProducerType.MULTI, new YieldingWaitStrategy());

            ringBuffer = disruptor.getRingBuffer();
            nextRead = new AtomicLong(ringBuffer.getMinimumGatingSequence());
        }

        @Override
        public Iterator<E> iterator() {
            throw new UnsupportedOperationException("Do not support iterator yet");
        }

        @Override
        public int size() {
            return (int) (SIZE - ringBuffer.remainingCapacity());
        }

        @Override
        public boolean offer(E e) {
            try {
                long next = ringBuffer.next();
                ValueEvent<E> event = ringBuffer.get(next);
                event.setValue(e);
                ringBuffer.publish(next);
                return true;
            } catch(Exception ex) {
                logger.warn("Could not offer to RingBuffer.", ex);
                return false;
            }
        }

        @Override
        public E poll() {
            long readSeq = nextRead.get();
            if (readSeq <= ringBuffer.getCursor()) {
                return ringBuffer.get(nextRead.getAndIncrement()).getValue();
            }
            return null;
        }

        @Override
        public E peek() {
            throw new UnsupportedOperationException("Do not support peek yet");
        }

    }

    public static final class ValueEvent<E> {
        private E value;

        public E getValue() {
            return value;
        }

        public void setValue(final E value) {
            this.value = value;
        }
    }

}
