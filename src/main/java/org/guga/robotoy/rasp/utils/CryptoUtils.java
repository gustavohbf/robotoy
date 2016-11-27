/*******************************************************************************
 * Copyright 2016 See https://github.com/gustavohbf/robotoy/blob/master/AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.guga.robotoy.rasp.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Random;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Cryptographic utility methods
 * 
 * @author Gustavo Figueiredo
 */
public class CryptoUtils {
	
	public static final String DEFAULT_SIGNATURE_ALGORITHM = "SHA256WithRSAEncryption";
	
	public static final String DEFAULT_KEY_ALGORITHM = "RSA";
	
	public static final int DEFAULT_KEY_SIZE = 1024;

	/**
	 * Generates a self-signed certificate stored in memory
	 * @param name Distinguished Name to be used in certificate
	 * @param keyAlgorithm Algorithm for key generation (e.g.: RSA)
	 * @param keySize Key size (e.g.: 1024)
	 * @param days Number of days in future for expiration of generated certificate
	 * @param sigAlgorithm Algorithm for certificate signature
	 * @param keystorePassword Password for both keystore and private key
	 * @param keystoreAlias Alias in keystore for holding key entry
	 */
	public static KeyStore genKeyStoreWithSelfSignedCert(String name,String keyAlgorithm,int keySize,int days,String sigAlgorithm,
			char[] keystorePassword, String keystoreAlias) 
			throws NoSuchAlgorithmException, NoSuchProviderException, IOException, OperatorCreationException, CertificateException, 
				KeyStoreException {
		
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)==null)
			Security.addProvider(new BouncyCastleProvider()); 

		SecureRandom sr = new SecureRandom();
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance(keyAlgorithm, "BC");
        keyGen.initialize( keySize, sr);
        KeyPair keypair = keyGen.generateKeyPair();
        
        X500Name subjectName = new X500Name("CN="+name);
        X500Name issuerName = subjectName; // subjects name: the same as we are self signed. 
        
        Date NOT_BEFORE = new Date(System.currentTimeMillis()); 
        Date NOT_AFTER = new Date(System.currentTimeMillis() + 86400000L * days); 
        
        BigInteger serial = BigInteger.valueOf(new Random().nextInt());
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuerName, 
        		serial, NOT_BEFORE, NOT_AFTER, subjectName, keypair.getPublic());
        builder.addExtension(Extension.subjectKeyIdentifier, false, createSubjectKeyIdentifier(keypair.getPublic())); 
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true)); 
        
        KeyUsage usage = new KeyUsage(KeyUsage.keyCertSign | KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.cRLSign); 
        builder.addExtension(Extension.keyUsage, false, usage); 
 
        ASN1EncodableVector purposes = new ASN1EncodableVector(); 
        purposes.add(KeyPurposeId.id_kp_serverAuth); 
        purposes.add(KeyPurposeId.id_kp_clientAuth); 
        purposes.add(KeyPurposeId.anyExtendedKeyUsage); 
        builder.addExtension(Extension.extendedKeyUsage, false, new DERSequence(purposes)); 
 
        X509Certificate cert = signCertificate(sigAlgorithm, builder, keypair.getPrivate()); 
 
        Certificate[] certChain = new Certificate[]{ cert }; 
        
        KeyStore ks = KeyStore.getInstance("pkcs12");
    	ks.load(null, keystorePassword);
    	ks.setKeyEntry(keystoreAlias, keypair.getPrivate(), keystorePassword, certChain);
    	
    	return ks;
	}
	
	public static void saveKeyStore(KeyStore ks,String filename,char[] keystorePassword) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
		FileOutputStream fOut = new FileOutputStream(new File(filename));
        ks.store(fOut, keystorePassword);
	}
	
    private static SubjectKeyIdentifier createSubjectKeyIdentifier(Key key) throws IOException { 
        ASN1InputStream is = null; 
        try { 
            is = new ASN1InputStream(new ByteArrayInputStream(key.getEncoded())); 
            ASN1Sequence seq = (ASN1Sequence) is.readObject(); 
            SubjectPublicKeyInfo info = new SubjectPublicKeyInfo(seq); 
            return new BcX509ExtensionUtils().createSubjectKeyIdentifier(info); 
        } finally { 
            try { is.close(); } catch (Throwable e){ }
        } 
    } 
    
    
    private static X509Certificate signCertificate(String sigAlgorithm, X509v3CertificateBuilder certificateBuilder, PrivateKey signedWithPrivateKey) 
    		throws OperatorCreationException, CertificateException { 
        ContentSigner signer = new JcaContentSignerBuilder(sigAlgorithm).setProvider("BC").build(signedWithPrivateKey); 
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certificateBuilder.build(signer)); 
    } 

}
