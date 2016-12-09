package com.solace.samples.cloudfoundry.springcloud.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CertificateUtil {
    
    private static final Log logger = LogFactory.getLog(CertificateUtil.class);
    
    
    // Change this to match the file in src/main/resources
    private static final String CERTIFICATE_FILE_NAME = "my-cert.cer";
    
    // Each certificate in the trusted store needs to have a unique alias.
    private static final String CERTIFICATE_ALIAS = "my-alias";

    // Path to the jre trusted store, when this is deployed in Cloud Foundry.
    private static final String TRUST_STORE = "/home/vcap/app/.java-buildpack/open_jdk_jre/lib/security/cacerts";
    
    // Standard default password for the trust store
    private static final String TRUST_STORE_PASSWORD = "changeit";
    
    public static void importCertificate() throws Exception {
        
        File file = new File(CERTIFICATE_FILE_NAME);
        logger.info("Loading certificate from " + file.getAbsolutePath());
        FileInputStream is = new FileInputStream(TRUST_STORE);
        char[] password = TRUST_STORE_PASSWORD.toCharArray();
        
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(is, password);
        is.close();
        
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream certstream = fullStream(CERTIFICATE_FILE_NAME);
        Certificate certs = cf.generateCertificate(certstream);
        keystore.setCertificateEntry(CERTIFICATE_ALIAS, certs);

        // Save the new keystore contents
        FileOutputStream out = new FileOutputStream(TRUST_STORE);
        keystore.store(out, password);
        out.close();
        
    }
    
    private static InputStream fullStream(String fname) throws IOException {
        //InputStream is = ClassLoader.class.getResourceAsStream(fname);
        FileInputStream is = new FileInputStream(fname);
        DataInputStream dis = new DataInputStream(is);
        byte[] bytes = new byte[dis.available()];
        dis.readFully(bytes);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return bais;
    }
}
