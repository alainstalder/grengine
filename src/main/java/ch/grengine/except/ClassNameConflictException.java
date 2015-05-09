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

import java.util.List;
import java.util.Map;

import ch.grengine.code.Code;


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

    private static final long serialVersionUID = -7064452473268097551L;
    
    private final Map<String,List<Code>> sameClassNamesInMultipleCodeLayersMap;
    private final Map<String,List<Code>> sameClassNamesInParentAndCodeLayersMap;

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
        this.sameClassNamesInParentAndCodeLayersMap = sameClassNamesInParentAndCodeLayersMap;
    }
    
    /**
     * gets a map of class name to a list of all {@link Code} layers
     * that contain a class with that name, but only if the class name
     * occurred more than once in the code layers.
     * 
     * @return map or null if had not been checked
     * 
     * @since 1.0
     */
    public Map<String,List<Code>> getSameClassNamesInMultipleCodeLayersMap() {
        return sameClassNamesInMultipleCodeLayersMap;
    }
    
    /**
     * gets a map of class name to a list of all {@link Code} layers
     * that contain a class with that name, but only if the class name
     * occurred also in the parent class loader.
     * 
     * @return map or null if had not been checked
     * 
     * @since 1.0
     */
    public Map<String,List<Code>> getSameClassNamesInParentAndCodeLayersMap() {
        return sameClassNamesInParentAndCodeLayersMap;
    }
    
    private static String toMessageString(final Map<String,List<Code>> map) {
        return map == null ? "(not checked)" : map.keySet().toString();
    }

}
