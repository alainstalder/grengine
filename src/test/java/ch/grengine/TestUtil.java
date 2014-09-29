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

package ch.grengine;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;


public class TestUtil {
        
    public static class FileThatThrowsInGetCanonicalFile extends File {
        private static final long serialVersionUID = -3224104992041563195L;
        public static final String ABSOLUTE_PATH = "/fallback/../to/absolute/path";
        public FileThatThrowsInGetCanonicalFile() { super(ABSOLUTE_PATH); }
        public File getCanonicalFile() throws IOException { throw new IOException(); }
        public File getAbsoluteFile() { return this; }
    }
    
    public static <T> Set<T> argsToSet(T... args) {
        Set<T> set = new HashSet<T>();
        for (T t : args) {
            set.add(t);
        }
        return set;
    }
    
    public static <T> List<T> argsToList(T... args) {
        List<T> list = new LinkedList<T>();
        for (T t : args) {
            list.add(t);
        }
        return list;
    }
    
    @SuppressWarnings("unchecked")
    public static <K,V> Map<K,V> argsToMap(Object... args) {
        assertTrue("must have even number of args", args.length % 2 == 0);
        Map<K,V> map = new HashMap<K,V>();
        boolean isKey = true;
        K key = null;
        for (Object arg : args) {
            if (isKey) {
                key = (K)arg;
            } else {
                map.put(key, (V)arg);
            }
            isKey = !isKey;
        }
        return map;
    }
    
    public static String multiply(String s, int nTimes) {
        StringBuilder out = new StringBuilder();
        for (int i=0; i<nTimes; i++) {
            out.append(s);
        }
        return out.toString();
    }
    
    public static void setFileText(File file, String text)
            throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        writer.write(text);
        writer.close();
    }
    
    public static String getFileText(File file) throws FileNotFoundException {
        Scanner scan = new Scanner(file);
        try {
            scan.useDelimiter("\\A");
            return scan.hasNext() ? scan.next() : "";
        } finally {
            scan.close();
        }
    }
    
    public static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

}
