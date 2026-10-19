package com.backstage.system.payment.huifu.crypto;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HuifuRsa2Test {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void canonicalizeSortsOnlyFirstLevelAndKeepsUnicodeAndSlash() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("z", "中文/路径");
        data.put("nested", "{\"z\":1,\"a\":2}");
        data.put("a", "lower");
        data.put("A", "upper");
        data.put("ignored", null);

        assertEquals("{\"A\":\"upper\",\"a\":\"lower\",\"nested\":\"{\\\"z\\\":1,\\\"a\\\":2}\",\"z\":\"中文/路径\"}",
                HuifuRsa2.canonicalize(data));
    }

    @Test
    public void signsAndVerifiesCanonicalData() throws Exception {
        KeyPair keyPair = generateKeyPair();
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("req_seq_id", "OSH202607100001");
        data.put("goods_desc", "课程/套餐");

        String signature = HuifuRsa2.signData(data, keyPair.getPrivate());

        assertTrue(HuifuRsa2.verifySyncData(data, signature, keyPair.getPublic()));
        data.put("goods_desc", "已篡改");
        assertFalse(HuifuRsa2.verifySyncData(data, signature, keyPair.getPublic()));
    }

    @Test
    public void asyncVerificationUsesExactRawResponseDataWithoutReordering() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String raw = "{\"b\":\"中文/路径\",\"a\":\"1\"}";
        String signature = HuifuRsa2.signRaw(raw, keyPair.getPrivate());

        assertTrue(HuifuRsa2.verifyAsyncResponseData(raw, signature, keyPair.getPublic()));
        assertFalse(HuifuRsa2.verifyAsyncResponseData(
                "{\"a\":\"1\",\"b\":\"中文/路径\"}", signature, keyPair.getPublic()));
    }

    @Test
    public void loadsPkcs8PrivateAndX509PublicFromPemOrBase64Files() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String privateBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        java.io.File privateFile = temporaryFolder.newFile("private.pem");
        java.io.File publicFile = temporaryFolder.newFile("public.key");
        Files.write(privateFile.toPath(),
                ("-----BEGIN PRIVATE KEY-----\n" + privateBase64 + "\n-----END PRIVATE KEY-----\n")
                        .getBytes(StandardCharsets.UTF_8));
        Files.write(publicFile.toPath(), publicBase64.getBytes(StandardCharsets.UTF_8));

        PrivateKey privateKey = HuifuRsa2.loadPrivateKey(privateFile.toPath());
        PublicKey publicKey = HuifuRsa2.loadPublicKey(publicFile.toPath());
        String signature = HuifuRsa2.signRaw("test", privateKey);

        assertTrue(HuifuRsa2.verifyRaw("test", signature, publicKey));
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
