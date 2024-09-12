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

package ch.grengine.source;


/**
 * Interface for a (Groovy script) source.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public interface Source {
    
    /**
     * gets the unique ID of this source.
     *
     * @return ID
     * 
     * @since 1.0
     */
    String getId();
    
    /**
     * gets the date and time the source was last recognized to have been modified,
     * in milliseconds since 1970.
     * <p>
     * You should not rely on this number to increase monotonically or even
     * to represent an actual date and time, but instead consider the source
     * changed each time this method returns a different value.
     *
     * @return last modified
     * 
     * @since 1.0
     */
    long getLastModified();
    
    /**
     * two sources are equal if and only if their source IDs are equal.
     *
     * @return whether the two sources are equal
     * 
     * @since 1.0
     */
    boolean equals(Object obj);
    
    /**
     * returns the hash code of the source ID.
     *
     * @return hash code of the source ID
     * 
     * @since 1.0
     */
    int hashCode();
    
    /**
     * returns a string suitable for logging.
     *
     * @return a string suitable for logging
     *
     * @since 1.0
     */
    String toString();

}
