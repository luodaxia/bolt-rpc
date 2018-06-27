package com.fmgame.bolt.benchmark;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import com.fmgame.bolt.benchmark.entity.IBenchmark;

public abstract class AbstractClientRunnable implements ClientRunnable {

    RunnableStatistics statistics;
    private CyclicBarrier cyclicBarrier;
    private CountDownLatch countDownLatch;
    private long startTime;
    private long endTime;
    private int statisticTime;
    private IBenchmark benchmark;

    public AbstractClientRunnable(IBenchmark benchmark, CyclicBarrier barrier, CountDownLatch latch, long startTime, long endTime) {
        this.cyclicBarrier = barrier;
        this.countDownLatch = latch;
        this.startTime = startTime;
        this.endTime = endTime;
        this.benchmark = benchmark;

        statisticTime = (int) ((endTime - startTime) / 1000000);
        statistics = new RunnableStatistics(statisticTime);
    }

    @Override
    public RunnableStatistics getStatistics() {
        return statistics;
    }

    @Override
    public void run() {
        try {
            cyclicBarrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
        callService();
        countDownLatch.countDown();
    }

    @SuppressWarnings("unused")
	private void callService() {
        long beginTime = System.nanoTime() / 1000L;
        while (beginTime <= startTime) {
            // warm up
            beginTime = System.nanoTime() / 1000L;
            Object result = call(benchmark);
        }
        while (beginTime <= endTime) {
            beginTime = System.nanoTime() / 1000L;
            Object result = call(benchmark);
            long responseTime = System.nanoTime() / 1000L - beginTime;
            collectResponseTimeDistribution(responseTime);
            int currTime = (int) ((beginTime - startTime) / 1000000L);
            if (currTime >= statisticTime) {
                continue;
            }
            if (result != null) {
                statistics.TPS[currTime]++;
                statistics.RT[currTime] += responseTime;
            } else {
                statistics.errTPS[currTime]++;
                statistics.errRT[currTime] += responseTime;
            }
        }
    }

    private void collectResponseTimeDistribution(long time) {
        double responseTime = (double) (time / 1000L);
        if (responseTime >= 0 && responseTime <= 1) {
            statistics.above0sum++;
        } else if (responseTime > 1 && responseTime <= 5) {
            statistics.above1sum++;
        } else if (responseTime > 5 && responseTime <= 10) {
            statistics.above5sum++;
        } else if (responseTime > 10 && responseTime <= 50) {
            statistics.above10sum++;
        } else if (responseTime > 50 && responseTime <= 100) {
            statistics.above50sum++;
        } else if (responseTime > 100 && responseTime <= 500) {
            statistics.above100sum++;
        } else if (responseTime > 500 && responseTime <= 1000) {
            statistics.above500sum++;
        } else if (responseTime > 1000) {
            statistics.above1000sum++;
        }
    }

    protected abstract Object call(IBenchmark benchmark);

}
