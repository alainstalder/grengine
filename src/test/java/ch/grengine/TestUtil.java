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

package ch.grengine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class TestUtil {
        
    public static class FileThatThrowsInGetCanonicalFile extends File {
        private static final long serialVersionUID = -3224104992041563195L;
        public static final String ABSOLUTE_PATH = "/fallback/../to/absolute/path";
        public FileThatThrowsInGetCanonicalFile() { super(ABSOLUTE_PATH); }
        @Override public File getCanonicalFile() throws IOException { throw new IOException(); }
        @Override public File getAbsoluteFile() { return this; }
    }

    @SuppressWarnings("unchecked")
    public static <K,V> Map<K,V> argsToMap(Object... args) {
        assertThat(args.length % 2 == 0, is(true));
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
