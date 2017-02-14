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

package ch.grengine.load;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.codehaus.groovy.reflection.ClassInfo;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * Default implementation for releasing class metadata when done using it.
 * <p>
 * Should permit on-the-fly garbage collection of the class, with any Groovy
 * version from 1.7.5 to the version that was current when this version of
 * Groovy was released (see Gradle dependency in the source for exact version).
 * 
 * @since 1.1
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class DefaultClassReleaser implements ClassReleaser {

    private Object globalClassSet;
    private Method globalClassSetRemoveMethod;
    private Object globalClassValue;
    private Method globalClassValueRemoveMethod;

    private static ClassReleaser releaser;
    private static final Object LOCK = new Object();

    /**
     * gets the singleton instance of the default class releaser.
     *
     * @return singleton instance, never null
     */
    public static ClassReleaser getInstance() {
        if (releaser == null) {
            synchronized (LOCK) {
                if (releaser == null) {
                    releaser = new DefaultClassReleaser();
                }
            }
        }
        return releaser;
    }

    private DefaultClassReleaser() {

        // ClassInfo#globalClassSet has been in Groovy since at least 1.7.5,
        // but only contains a remove(Object clazz) method before 2.4.0
        try {
            Field globalClassSetField = ClassInfo.class.getDeclaredField("globalClassSet");
            globalClassSetField.setAccessible(true);
            globalClassSet = globalClassSetField.get(ClassInfo.class);
            globalClassSetRemoveMethod = globalClassSet.getClass().getMethod("remove", Object.class);
            globalClassSetRemoveMethod.setAccessible(true);
        } catch (Exception ignore) {
            globalClassSetRemoveMethod = null;
        }

        // ClassInfo#globalClassValue has been added to Groovy in 2.4.0
        try {
            Field globalClassValueField = ClassInfo.class.getDeclaredField("globalClassValue");
            globalClassValueField.setAccessible(true);
            globalClassValue = globalClassValueField.get(ClassInfo.class);
            globalClassValueRemoveMethod = globalClassValue.getClass().getMethod("remove", Class.class);
            globalClassValueRemoveMethod.setAccessible(true);
        } catch (Exception ignore) {
            globalClassValueRemoveMethod = null;
        }
    }

    @Override
    public void release(Class<?> clazz) {

        // has been in Groovy since at least 1.7.5.
        InvokerHelper.removeClass(clazz);

        // globalClassSet.remove(clazz);
        if (globalClassSetRemoveMethod != null) {
            try {
                globalClassSetRemoveMethod.invoke(globalClassSet, clazz);
            } catch (Exception ignore) {
            }
        }

        // globalClassValue.remove(clazz);
        if (globalClassValueRemoveMethod != null) {
            try {
                globalClassValueRemoveMethod.invoke(globalClassValue, clazz);
            } catch (Exception ignore) {
            }
        }
    }

}
