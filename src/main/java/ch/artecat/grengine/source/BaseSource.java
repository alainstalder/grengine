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

package ch.artecat.grengine.source;



/**
 * Abstract base implementation of the {@link Source} interface.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public abstract class BaseSource implements Source {
    
    /**
     * the source id.
     * 
     * @since 1.0
     */
    protected String id;
    
    /**
     * protected constructor.
     * 
     * @since 1.0
     */
    protected BaseSource() {
    }
    
    /**
     * gets the protected member variable {@code id}.
     *
     * @return id
     * 
     * @since 1.0
     */
    @Override
    public String getId() {
        return id;
    }
    
    /**
     * always returns 0.
     *
     * @return always 0
     * 
     * @since 1.0
     */
    @Override
    public long getLastModified() {
        return 0;
    }
    
    /**
     * implementation based on the protected member variable {@code id}.
     * 
     * @since 1.0
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Source)) return false;
        return getId().equals(((Source)obj).getId());
    }
    
    /**
     * implementation based on the protected member variable {@code id}.
     * 
     * @since 1.0
     */
    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[ID=" + getId() + "]";
    }

}
