package com.pushtechnology.benchmarks.util;


import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * JVMSupport
 * <P>
 * This class provides support for JVM vendor and implementation specific code.
 * This may be needed for such things as the Java Secure Socket Extension
 * (JSSE). It implements the singleton pattern to ensure that shared state is
 * maintained. This state is actively loaded with the enum.
 *
 * @author mchampion - created 14 Mar 2013
 * @since 4.5.1
 */
public enum JVMSupport {

    /** Singleton instance. */
    SINGLETON;

    private static final String VENDOR_PROPERTY = "java.vm.vendor";
    private static final String VENDOR_PROPERTY_DEFAULT = "NONE";

    // Possible state values
    private static final String VENDOR_IBM = "IBM Corporation";
    private static final String JSSE_IBM = "IBMJSSE2";
    private static final String JSSE_SUN = "SunJSSE";
    private static final String X509_IBM = "IbmX509";
    private static final String X509_SUN = "SunX509";

    // Singleton state
    private final String vendor;
    private final String jsse;
    private final String x509;

    private JVMSupport() {
        vendor = System.getProperty(VENDOR_PROPERTY, VENDOR_PROPERTY_DEFAULT);

        // Populate the singleton state based on vendor
        if (VENDOR_IBM.equals(vendor)) {
            jsse = JSSE_IBM;
            x509 = X509_IBM;
        }
        else {
            jsse = JSSE_SUN;
            x509 = X509_SUN;
        }
    }

    /**
     * getKeyManagerFactory method
     * <P>
     * JVM vendors may provide custom implementations of key managers these are
     * retrieved by a key. The correct key to use varies by vendor. This method
     * uses the correct key. A new KeyManagerFactory instance is returned by
     * each invocation. An X509 KeyManagerFactory is returned.
     *
     * @return KeyManagerFactory provided by the JVM
     * @throws NoSuchAlgorithmException Thrown if the provider does not support
     *         the algorithm
     * @since 4.5.1
     */
    public static final KeyManagerFactory getKeyManagerFactory()
        throws NoSuchAlgorithmException {
        return KeyManagerFactory.getInstance(SINGLETON.x509);
    }

    /**
     * getTrustManagerFactory method
     * <P>
     * JVM vendors may provide custom implementations of the JSSE. Factories for
     * the objects used may need to be retrieved by keys that will vary for each
     * provider. This method uses the correct key. A new TrustManagerFactory
     * instance is returned by each invocation. An X509 TrustManagerFactory is
     * returned.
     *
     * @return TrustManagerFactory provided by the JVM
     * @throws NoSuchAlgorithmException Thrown if the provider does not support
     *         the algorithm
     * @throws NoSuchProviderException Thrown if the provider is not available
     * @since 4.5.1
     */
    public static final TrustManagerFactory getTrustManagerFactory()
        throws NoSuchAlgorithmException, NoSuchProviderException {
        return TrustManagerFactory.getInstance(SINGLETON.x509, SINGLETON.jsse);
    }
}