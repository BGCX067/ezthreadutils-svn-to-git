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
package com.chilliwebs.ezthreadutils.exceptions;

/**
 * Standard exception for the ezThreadUtils library, this exception takes a string
 * message.
 * 
 * @author Nick Hecht chilliwebs@gmail.com
 */
public class ezThreadUtilsException extends RuntimeException {

    /**
     * Standard exception for the ezThreadUtils library, this exception takes a string
     * message.
     * 
     * @param message string message
     */
    public ezThreadUtilsException(String message) {
        super(message);
    }

    /**
     * Standard exception for the ezThreadUtils library, this exception can be used to
     * expose the underlying exception if the developer feels it necessary.
     * 
     * @param message   string message
     * @param cause     the cause exception
     */
    public ezThreadUtilsException(String message, Throwable cause) {
        super(message, cause);
    }
}
