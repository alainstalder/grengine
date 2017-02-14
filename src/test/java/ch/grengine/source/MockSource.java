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


public class MockSource extends BaseSource {
    
    private long lastModified;
    
    public MockSource(String id) {
        this.id = id;
    }
        
    @Override
    public long getLastModified() {
        return lastModified;
    }
    
    public MockSource setLastModified(long lastModified) {
        this.lastModified = lastModified;
        return this;
    }
    
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[ID='" + id + "', lastModified=" + lastModified + "]";
    }

}
