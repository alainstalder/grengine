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

package ch.grengine.code;


import static java.util.Objects.requireNonNull;

/**
 * Bytecode with class name.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class Bytecode {
    
    private final String className;
    private final byte[] bytes;
    
    /**
     * constructor from class name and bytecode bytes.
     *
     * @param className class name
     * @param bytes bytecode bytes
     *
     * @throws NullPointerException if any argument is null
     *
     * @since 1.0
     */
    public Bytecode(final String className, final byte[] bytes) {
        requireNonNull(className, "Class name is null.");
        requireNonNull(bytes, "Bytes are null.");
        this.className = className;
        this.bytes = bytes;
    }
       
    /**
     * gets the class name.
     *
     * @return class name
     * 
     * @since 1.0
     */
    public String getClassName() {
        return className;
    }
    
    /**
     * gets the bytecode bytes.
     *
     * @return bytecode bytes
     *
     * @since 1.0
     */
    public byte[] getBytes() {
        return bytes;
    }

    /**
     * returns a string suitable for logging.
     *
     * @return a string suitable for logging
     * 
     * @since 1.0
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[className=" + className + ", bytes=" + bytes + "]";
    }

}
