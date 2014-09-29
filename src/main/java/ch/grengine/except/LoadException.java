/*
   Copyright 2014-now by Alain Stalder. Made in Switzerland.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package ch.grengine.except;



/**
 * Exception thrown when loading a class failed.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class LoadException extends GrengineException {

    private static final long serialVersionUID = -5845310653276575510L;

    /**
     * constructor from exception message.
     * 
     * @since 1.0
     */
    public LoadException(final String message) {
        super(message);
    }

    /**
     * constructor from exception message and causing throwable.
     * 
     * @since 1.0
     */
    public LoadException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
