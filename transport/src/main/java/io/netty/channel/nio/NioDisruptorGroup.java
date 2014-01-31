package io.netty.channel.nio;

import io.netty.channel.EventLoop;
import io.netty.util.concurrent.EventExecutor;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * Created by michael on 31/01/14.
 */
public class NioDisruptorGroup extends NioEventLoopGroup {

    public NioDisruptorGroup() {
    }

    public NioDisruptorGroup(int nThreads) {
        super(nThreads);
    }

    public NioDisruptorGroup(int nThreads, ThreadFactory threadFactory) {
        super(nThreads, threadFactory);
    }

    public NioDisruptorGroup(int nThreads, Executor executor) {
        super(nThreads, executor);
    }

    public NioDisruptorGroup(int nThreads, ThreadFactory threadFactory, SelectorProvider selectorProvider) {
        super(nThreads, threadFactory, selectorProvider);
    }

    public NioDisruptorGroup(int nThreads, Executor executor, SelectorProvider selectorProvider) {
        super(nThreads, executor, selectorProvider);
    }

    /**
     * Sets the percentage of the desired amount of time spent for I/O in the child event loops.  The default value is
     * {@code 50}, which means the event loop will try to spend the same amount of time for I/O as for non-I/O tasks.
     */
    public void setIoRatio(int ioRatio) {
        for (EventExecutor e: children()) {
            ((NioDisruptor) e).setIoRatio(ioRatio);
        }
    }

    /**
     * Replaces the current {@link java.nio.channels.Selector}s of the child event loops with newly created {@link java.nio.channels.Selector}s to work
     * around the  infamous epoll 100% CPU bug.
     */
    public void rebuildSelectors() {
        for (EventExecutor e: children()) {
            ((NioDisruptor) e).rebuildSelector();
        }
    }

    @Override
    protected EventLoop newChild(Executor executor, Object... args) throws Exception {
        return new NioDisruptor(this, executor, (SelectorProvider) args[0]);
    }
}
