/*
 * Copyright 2013 Nick Hecht chilliwebs@gmail.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chilliwebs.ezthreadutils;

import java.lang.Thread.State;
import java.lang.reflect.Array;
import java.util.Arrays;

/**
 *
 * @author Nick Hecht
 */
public class Producer<T extends Comparable<T>> {

    final Queue queue;
    private ShedulerThread[] thread;
    private boolean threadStarted = false;
    private Boolean paused = false;
    private Consumer<T> consumer;
    private int priority = Thread.NORM_PRIORITY;
    // exception vars
    private boolean exception_thrown = false;
    private Exception exception = null;

    @SuppressWarnings({"unchecked"})
    public Producer(int threadCount, Class<T> clazz, Consumer<T> consumer) {
        this.consumer = consumer;
        queue = new Queue(clazz, 32);
        this.consumer.setQueue(queue);
        thread = (ShedulerThread[]) Array.newInstance(ShedulerThread.class, threadCount);
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void start() {
        //start Scheduler and Threads
        if (!threadStarted) {
            for (int i = 0; i < thread.length; i++) {
                thread[i] = new ShedulerThread();
                thread[i].setPriority(priority);
                thread[i].start();
            }
            threadStarted = true;
        }
    }

    public void stop() {
        // stop worker threads
        for (int i = 0; i < thread.length; i++) {
            thread[i].exit();
        }
        synchronized (queue) {
            queue.notifyAll();
        }
        for (int i = 0; i < thread.length; i++) {
            try {
                thread[i].join();
            } catch (InterruptedException ex) {
            }
        }
        threadStarted = false;
    }

    public void pause() {
        synchronized (queue) {
            if (!paused) {
                paused = true;
                for (int i = 0; i < thread.length; i++) {
                    if (!Thread.currentThread().equals(thread[i])) {
                        thread[i].pause(true);
                    }
                }
                for (int i = 0; i < thread.length; i++) {
                    if (!Thread.currentThread().equals(thread[i])) {
                        while (thread[i].getState() != Thread.State.WAITING) {
                            Thread.yield();
                        }
                    }
                }
            }
        }
    }

    public void resume() {
        synchronized (queue) {
            if (paused) {
                for (int i = 0; i < thread.length; i++) {
                    if (!Thread.currentThread().equals(thread[i])) {
                        thread[i].pause(false);
                    }
                }
                paused = false;
                queue.notifyAll();
            }
        }
    }

    public void waitUntilQueueEmpty() throws Exception {
        int size = 1;
        while (size > 0) {
            handleException();
            Thread.yield();
            synchronized (queue) {
                size = queue.size;
            }
        }
    }

    public void waitUntilIdle() throws Exception {
        boolean running = true;
        while (running) {
            running = false; // if threads are waiting this will stay false
            for (int i = 0; i < thread.length; i++) {
                if (thread[i].getState() == Thread.State.WAITING) {
                    thread[i].exit();
                } else {
                    running = true; // ok there is still a thread running
                }
            }
            handleException();
            Thread.yield();
        }
    }

    public int getSize() {
        int size;
        synchronized (queue) {
            size = queue.size;
        }
        return size;
    }

    public boolean isEmpty() {
        return getSize() == 0;
    }

    public boolean isIdle() {
        boolean running = false; // if threads are waiting this will stay false
        for (int i = 0; i < thread.length; i++) {
            if (thread[i].getState() == Thread.State.WAITING) {
                thread[i].exit();
            } else {
                running = true; // ok there is still a thread running
            }
        }
        return !running;
    }

    public int getLiveConsumers() {
        int consumers = 0;
        //start Scheduler and Threads
        for (int i = 0; i < thread.length; i++) {
            if (thread[i].getState() != State.TERMINATED) {
                consumers++;
            }
        }
        return consumers;
    }

    public void produce(T data) throws Exception {
        handleException();
        synchronized (queue) {
            queue.add(data);
            queue.notify();
        }
    }

    public void removeAll(T data) {
        synchronized (queue) {
            for (int i = 1; i <= queue.size(); i++) {
                if (queue.get(i).equals(data)) {
                    queue.quickRemove(i);
                }
            }
        }
    }

    public boolean paused() {
        boolean p;
        synchronized (queue) {
            p = paused;
        }
        return p;
    }

    private void handleException() throws Exception {
        if (exception_thrown) {
            throw exception;
        }
    }

    private class ShedulerThread extends Thread {

        private Boolean running = true;
        private boolean paused = false;
        private Boolean newWorkMayBeScheduled = true;

        private ShedulerThread() {
        }

        @Override
        public void run() {
            try {
                mainLoop();
            } finally {
                // Someone killed this Thread, behave as if Timer cancelled
                synchronized (this) {
                    newWorkMayBeScheduled = false;
                }
            }
        }

        public void exit() {
            synchronized (this) {
                running = false;
                newWorkMayBeScheduled = false;
            }
        }

        public void pause(boolean paused) {
            this.paused = paused;
        }

        private void mainLoop() {
            boolean run = true;
            boolean nwrk = true;
            while (run) {
                T data = null;
                try {
                    synchronized (queue) {
                        // Wait for queue to become non-empty
                        while (paused || (nwrk && queue.isEmpty())) {
                            data = null;
                            queue.wait();
                            synchronized (this) {
                                nwrk = newWorkMayBeScheduled;
                            }
                        }
                        if (!nwrk && queue.isEmpty()) {
                            break; // Queue is empty and will forever remain; die
                        }
                        data = queue.getMin();
                        queue.removeMin();
                    }
                    consumer.consume(data);
                } catch (InterruptedException e) {
                    synchronized (queue) {
                        queue.add(data);
                        queue.notify();
                    }
                } catch (Exception e) {
                    running = false;
                    exception_thrown = true;
                    exception = e;
                }
                synchronized (this) {
                    run = running;
                }
            }
        }
    }

    class Queue {

        private T[] queue;
        private int size = 0;

        public Queue(Class<T> c, int s) {
            queue = (T[]) Array.newInstance(c, s);
        }

        private int size() {
            return size;
        }

        private void add(T qi) {
            // Grow backing store if necessary
            if (size + 1 == queue.length) {
                queue = Arrays.copyOf(queue, 2 * queue.length);
            }
            queue[++size] = qi;
            fixUp(size);
        }

        private T getMin() {
            return queue[1];
        }

        private T get(int i) {
            return queue[i];
        }

        private void removeMin() {
            queue[1] = queue[size];
            queue[size--] = null;  // Drop extra reference to prevent memory leak
            fixDown(1);
        }

        private void quickRemove(int i) {
            assert i <= size;
            queue[i] = queue[size];
            queue[size--] = null;  // Drop extra ref to prevent memory leak
        }

        private boolean isEmpty() {
            return size == 0;
        }

        private void clear() {
            // Null out task references to prevent memory leak
            for (int i = 1; i < queue.length; i++) {
                queue[i] = null;
            }
            size = 0;
        }

        private void fixUp(int k) {
            while (k > 1) {
                int j = k >> 1;
                if (queue[j].compareTo(queue[k]) <= 0) {
                    break;
                }
                T tmp = queue[j];
                queue[j] = queue[k];
                queue[k] = tmp;
                k = j;
            }
        }

        private void fixDown(int k) {
            int j;
            while ((j = k << 1) <= size && j > 0) {
                if (j < size && queue[j].compareTo(queue[j + 1]) > 0) {
                    j++; // j indexes smallest kid
                }
                if (queue[k].compareTo(queue[j]) <= 0) {
                    break;
                }
                T tmp = queue[j];
                queue[j] = queue[k];
                queue[k] = tmp;
                k = j;
            }
        }
    }
}
