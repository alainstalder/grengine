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

package ch.artecat.grengine.except;


/**
 * Exception thrown when creating an instance of {@link groovy.lang.Script} failed.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class CreateException extends GrengineException {

    private static final long serialVersionUID = 1910647986466544521L;

    /**
     * constructor from exception message.
     *
     * @param message message
     * 
     * @since 1.0
     */
    public CreateException(final String message) {
        super(message);
    }

    /**
     * constructor from exception message and causing throwable.
     *
     * @param message message
     * @param cause cause
     * 
     * @since 1.0
     */
    public CreateException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
