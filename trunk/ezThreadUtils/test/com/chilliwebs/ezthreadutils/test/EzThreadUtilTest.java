/*
 * Copyright 2013 Nick Hecht chilliwebs@gmail.com.
 *
 * DO NOT DISTRIBUTE
 */
package com.chilliwebs.ezthreadutils.test;

import com.chilliwebs.ezthreadutils.Consumer;
import com.chilliwebs.ezthreadutils.Finalizer;
import com.chilliwebs.ezthreadutils.Producer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Nick Hecht chilliwebs@gmail.com
 */
public class EzThreadUtilTest {
    
    public EzThreadUtilTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void TestProducerConsumer() {
        Integer[] numbers = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        int threads = 4;
        Producer<Integer> prod = new Producer<Integer>(threads, Integer.class, new Consumer<Integer>() {
            @Override
            public void consume(Integer data) throws Exception {
                assertTrue(data <= 9 && data >= 0);
                Thread.sleep(10);
            }
        });
        // test size
        assertEquals(0, prod.getSize());
        try {
            for (Integer num : numbers) {
                prod.produce(num);
            }
        } catch (Exception ex) {
            Logger.getLogger(EzThreadUtilTest.class.getName()).log(Level.SEVERE, null, ex);
            fail(ex.getLocalizedMessage());
        }
        
        // test size
        assertEquals(10, prod.getSize());
        
        // test started
        prod.start();
        assertEquals(threads, prod.getLiveConsumers());
        assertFalse(prod.isIdle());
        
        try {
            // test wait untill empty
            prod.waitUntilQueueEmpty();
            assertTrue(prod.isEmpty());
            
            // test idle
            prod.waitUntilIdle();
            assertTrue(prod.isIdle());
            
            //stop
            prod.stop();
        } catch (Exception ex) {
            Logger.getLogger(EzThreadUtilTest.class.getName()).log(Level.SEVERE, null, ex);
            fail(ex.getLocalizedMessage());
        }
        
        //test stoped
        assertEquals(0, prod.getLiveConsumers());
    }
    
    @Test
    public void TestFinalizer() {
        final boolean[] after_injectThreadFinalCallback = {false};
        final boolean[] thread_callback_called = {false};
        try {
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        Finalizer.injectThreadFinalCallback(new Runnable() {
                            @Override
                            public void run() {
                                synchronized (thread_callback_called) {
                                    thread_callback_called[0] = true;
                                }
                            }
                        });
                        synchronized (after_injectThreadFinalCallback) {
                            after_injectThreadFinalCallback[0] = true;
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(EzThreadUtilTest.class.getName()).log(Level.SEVERE, null, ex);
                        fail(ex.getLocalizedMessage());
                    }
                }
            };
            t.start();
            t.join();
            Thread.sleep(10);
        } catch (InterruptedException ex) {
            Logger.getLogger(EzThreadUtilTest.class.getName()).log(Level.SEVERE, null, ex);
            fail(ex.getLocalizedMessage());
        }
        assertTrue("inject was not called successfully", after_injectThreadFinalCallback[0]);
        assertTrue("callback was not called successfully", thread_callback_called[0]);
    }
}
