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

package ch.artecat.grengine.engine;



/**
 * Engine ID used to authenticate instances of {@link Loader}.
 * <p>
 * The engine ID is a secret shared between an engine and the loaders it creates.
 * Only loaders created by the same engine are allowed to be passed as arguments
 * to engine methods.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class EngineId {
    
    /**
     * constructor.
     * 
     * @since 1.0
     */
    public EngineId() {}

}
