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

import java.io.FileInputStream;
import java.io.IOException;
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
     * Loads a properties from a file name.
     * @param filename ...
     * @return the initialized properties
     * @throws IOException ...
     */
    public static Properties load(String filename)
            throws IOException {
        Properties p = new Properties();
        FileInputStream inStream = new FileInputStream(filename);
        p.load(inStream);
        inStream.close();
        return p;
    }

    /**
     * Similar to Integer.getInteger, but for Properties.
     * 
     * @param props ...
     * @param pName ...
     * @param dVal ...
     * @return the value of the property or default value should parsing fail
     */
    public static double getProperty(Properties props, String pName,
            double dVal) {
        double pVal = valueOf(props.getProperty(pName), dVal);
        props.setProperty(pName, String.valueOf(pVal));
        return pVal;
    }

    /**
     * Value of double with default fall back value.
     * 
     * @param stringVal ...
     * @param dVal ...
     * @return the value of stringVal or dVal if it fails
     */
    public static double valueOf(String stringVal, double dVal) {
        try {
            return Double.valueOf(stringVal);
        } catch (Exception e) {
            return dVal;
        }
    }

    // CHECKSTYLE:OFF
    public static long getProperty(Properties props, String pName, long dVal) {
        long pVal = valueOf(props.getProperty(pName), dVal);
        props.setProperty(pName, String.valueOf(pVal));
        return pVal;
    }

    public static long valueOf(String stringVal, long dVal) {
        try {
            return Long.valueOf(stringVal);
        } catch (Exception e) {
            return dVal;
        }
    }

    public static int getProperty(Properties props, String pName, int dVal) {
        int pVal = valueOf(props.getProperty(pName), dVal);
        props.setProperty(pName, String.valueOf(pVal));
        return pVal;
    }

    public static int valueOf(String stringVal, int dVal) {
        try {
            return Integer.valueOf(stringVal);
        } catch (Exception e) {
            return dVal;
        }
    }

    public static boolean getProperty(Properties props, String pName,
            boolean dVal) {
        boolean pVal = valueOf(props.getProperty(pName), dVal);
        props.setProperty(pName, String.valueOf(pVal));
        return pVal;
    }

    public static boolean valueOf(String stringVal, boolean dVal) {
        try {
            return Boolean.valueOf(stringVal);
        } catch (Exception e) {
            return dVal;
        }
    }

    public static String getProperty(Properties props, String pName,
            String dVal) {
        String pVal = props.getProperty(pName, dVal);
        props.setProperty(pName, pVal);
        return pVal;
    }
    // CHECKSYLE:ON
}
