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

import ch.artecat.grengine.sources.Sources;



/**
 * Exception thrown when (Groovy script) compilation failed.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class CompileException extends GrengineException {

    private static final long serialVersionUID = -7064452473268097551L;
    
    private final Sources sources;

    /**
     * constructor from exception message and sources for which compilation failed.
     *
     * @param message message
     * @param sources sources for which compilation failed
     *
     * @since 1.0
     */
    public CompileException(final String message, final Sources sources) {
        super(message);
        this.sources = sources;
    }

    /**
     * constructor from exception message, causing throwable and sources for which compilation failed.
     *
     * @param message message
     * @param cause cause
     * @param sources sources for which compilation failed
     *
     * @since 1.0
     */
    public CompileException(final String message, final Throwable cause, final Sources sources) {
        super(message, cause);
        this.sources = sources;
    }
    
    /**
     * gets sources for which compilation failed.
     *
     * @return sources for which compilation failed
     * 
     * @since 1.0
     */
    public Sources getSources() {
        return sources;
    }

}
