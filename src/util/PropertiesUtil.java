/*
 * Copyright 2013 Push Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package util;

import java.util.Properties;

/**
 * A utility for dealing with properties.
 * 
 * @author nitsanw
 * 
 */
public final class PropertiesUtil {
    /**
     * no instances wanted...
     */
    private PropertiesUtil() {
    }

    /**
     * Compensating for the lack of Double.getDouble method. This method is the
     * same as {@link Integer.getInteger}
     * 
     * @param sysPropertyName ...
     * @param defaultVal ...
     * @return the value of the property or default value should parsing fail
     */
    public static double getSysPropertyVal(String sysPropertyName,
            double defaultVal) {
        return valueOf(System.getProperty(sysPropertyName), defaultVal);
    }

    /**
     * Similar to getSysPropertyVal, but for Properties.
     * 
     * @param props ...
     * @param propertyName ...
     * @param defaultVal ...
     * @return the value of the property or default value should parsing fail
     */
    public static double getPropertyVal(Properties props, String propertyName,
            double defaultVal) {
        return valueOf(props.getProperty(propertyName), defaultVal);
    }

    /**
     * Value of double with default fall back value.
     * 
     * @param stringVal ...
     * @param defaultVal ...
     * @return the value of stringVal or defaultVal if it fails
     */
    public static double valueOf(String stringVal, double defaultVal) {
        try {
            return Double.valueOf(stringVal);
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
