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

import ch.grengine.code.Code;

import java.io.Serial;
import java.util.List;
import java.util.Map;


/**
 * Exception optionally thrown if code layers or code layers and parent
 * class loader contain classes with the same name.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class ClassNameConflictException extends GrengineException {

    @Serial
    private static final long serialVersionUID = -7064452473268097551L;

    // since 1.0, transient since 3.2.0
    private final transient Map<String,List<Code>> sameClassNamesInMultipleCodeLayersMap;

    /**
     * gets a map of class name to a list of all {@link Code} layers
     * that contain a class with that name, but only if the class name
     * occurred more than once in the code layers
     * - converted to a string.
     * <p>
     * Empty if had not been checked.
     *
     * @since 3.2.0
     */
    private final String sameClassNamesInMultipleCodeLayersMapAsString;

    // since 1.0, transient since 3.2.0
    private final transient Map<String,List<Code>> sameClassNamesInParentAndCodeLayersMap;

    /**
     * a map of class name to a list of all {@link Code} layers
     * that contain a class with that name, but only if the class name
     * occurred also in the parent class loader
     * - converted to a string.
     * <p>
     * Empty if had not been checked.
     *
     * @since 3.2.0
     */
    private final String sameClassNamesInParentAndCodeLayersMapAsString;

    /**
     * constructor from exception message and maps with class conflict information.
     * <p>
     * One of the maps may be null if the corresponding check has not been made.
     *
     * @param message message
     * @param sameClassNamesInMultipleCodeLayersMap map of class names in multiple code layers
     * @param sameClassNamesInParentAndCodeLayersMap map of class names in parent and code layers
     *
     * @since 1.0
     */
    public ClassNameConflictException(final String message,
            final Map<String,List<Code>> sameClassNamesInMultipleCodeLayersMap,
            final Map<String,List<Code>> sameClassNamesInParentAndCodeLayersMap) {
        super(message + " Duplicate classes in code layers: " + 
                toMessageString(sameClassNamesInMultipleCodeLayersMap) +
                ", classes in code layers and parent: " +
                toMessageString(sameClassNamesInParentAndCodeLayersMap));
        this.sameClassNamesInMultipleCodeLayersMap = sameClassNamesInMultipleCodeLayersMap;
        this.sameClassNamesInMultipleCodeLayersMapAsString =
                (sameClassNamesInMultipleCodeLayersMap == null) ? ""
                        : sameClassNamesInMultipleCodeLayersMap.toString();
        this.sameClassNamesInParentAndCodeLayersMap = sameClassNamesInParentAndCodeLayersMap;
        this.sameClassNamesInParentAndCodeLayersMapAsString =
                (sameClassNamesInParentAndCodeLayersMap == null) ? ""
                        : sameClassNamesInParentAndCodeLayersMap.toString();
    }

    /**
     * gets a map of class name to a list of all {@link Code} layers
     * that contain a class with that name, but only if the class name
     * occurred more than once in the code layers.
     * 
     * @return map or null if had not been checked
     *   or the exception was deserialized
     * 
     * @since 1.0
     */
    public Map<String,List<Code>> getSameClassNamesInMultipleCodeLayersMap() {
        return sameClassNamesInMultipleCodeLayersMap;
    }

    /**
     * the map of {@link #getSameClassNamesInMultipleCodeLayersMap()}
     * converted to a string; survives exception deserialization.
     *
     * @return map converted to a string or empty if had not been checked
     *
     * @since 3.2.0
     */
    public String getSameClassNamesInMultipleCodeLayersMapAsString() {
        return sameClassNamesInMultipleCodeLayersMapAsString;
    }

    /**
     * gets a map of class name to a list of all {@link Code} layers
     * that contain a class with that name, but only if the class name
     * occurred also in the parent class loader.
     * 
     * @return map or null if had not been checked
     *   or the exception was deserialized
     * 
     * @since 1.0
     */
    public Map<String,List<Code>> getSameClassNamesInParentAndCodeLayersMap() {
        return sameClassNamesInParentAndCodeLayersMap;
    }

    /**
     * the map of {@link #getSameClassNamesInParentAndCodeLayersMap()}
     * converted to a string; survives exception deserialization.
     *
     * @return map converted to a string or empty if had not been checked
     *
     * @since 3.2.0
     */
    public String getSameClassNamesInParentAndCodeLayersMapAsString() {
        return sameClassNamesInParentAndCodeLayersMapAsString;
    }

    private static String toMessageString(final Map<String,List<Code>> map) {
        return map == null ? "(not checked)" : map.keySet().toString();
    }

}
