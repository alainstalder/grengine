/*
   Copyright 2014-now by Alain Stalder. Made in Switzerland.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package ch.grengine.except;

import java.util.Date;


/**
 * General Grengine exception.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class GrengineException extends RuntimeException {

    private static final long serialVersionUID = 393479472945664906L;
    
    private final String message;
    private final Date dateThrown = new Date();
    
    /**
     * constructor from exception message.
     *
     * @param message message
     * 
     * @since 1.0
     */
    public GrengineException(final String message) {
        super(message);
        this.message = message;
    }

    /**
     * constructor from exception message and causing throwable.
     *
     * @param message message
     * @param cause cause
     *
     * @since 1.0
     */
    public GrengineException(final String message, final Throwable cause) {
        super(message, cause);
        this.message = message + (cause == null ? "" : " Cause: " + cause);
    }
    
    /**
     * gets the exception message given in the constructor,
     * with the exception message of the causing throwable appended,
     * if there was one.
     *
     * @return message
     *
     * @since 1.0
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * gets the date (with time) when the exception was thrown.
     *
     * @return date (with time) when the exception was thrown
     * 
     * @since 1.0
     */
    public Date getDateThrown() {
        return dateThrown;
    }

}
