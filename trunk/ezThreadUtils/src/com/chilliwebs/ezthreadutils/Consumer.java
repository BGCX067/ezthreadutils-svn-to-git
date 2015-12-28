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

import com.chilliwebs.ezthreadutils.Producer.Queue;

/**
 *
 * @author Nick Hecht chilliwebs@gmail.com
 */
public abstract class Consumer<T> {

    private Queue queue;

    public abstract void consume(T data) throws Exception;

    void setQueue(Queue queue) {
        this.queue = queue;
    }

    void sleep(long timeout) throws Exception {
        synchronized (queue) {
            queue.wait(timeout);
        }
    }
}
