package com.backstage.system.payment.huifu.crypto;

import com.backstage.system.payment.huifu.exception.HuifuConfigurationException;
import com.backstage.system.payment.huifu.exception.HuifuSignatureException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * 汇付 RSA2 加签验签工具。
 *
 * <p>同步请求/响应只排序 data 第一层；异步通知必须直接验签原始 resp_data 字符串。</p>
 */
public final class HuifuRsa2 {

    private static final String SIGN_ALGORITHM = "SHA256withRSA";
    private static final int MAX_KEY_FILE_BYTES = 64 * 1024;
    private static final ObjectMapper CANONICAL_MAPPER = createCanonicalMapper();

    private HuifuRsa2() {
    }

    /**
     * data 第一层按 key 的 ASCII/Unicode 码点升序排列，null 字段不参与签名。
     * 嵌套结构不递归排序；接口要求嵌套 JSON 作为字符串传递。
     */
    public static String canonicalize(Map<String, ?> data) {
        if (data == null) {
            throw new HuifuSignatureException("汇付签名 data 不能为空");
        }
        TreeMap<String, Object> sorted = new TreeMap<String, Object>();
        for (Map.Entry<String, ?> entry : data.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                sorted.put(entry.getKey(), entry.getValue());
            }
        }
        try {
            return CANONICAL_MAPPER.writeValueAsString(sorted);
        } catch (JsonProcessingException e) {
            throw new HuifuSignatureException("汇付签名 data 序列化失败", e);
        }
    }

    public static String signData(Map<String, ?> data, PrivateKey privateKey) {
        return signRaw(canonicalize(data), privateKey);
    }

    public static boolean verifySyncData(Map<String, ?> data, String encodedSignature, PublicKey publicKey) {
        return verifyRaw(canonicalize(data), encodedSignature, publicKey);
    }

    /** 异步通知只能传入 HTTP 请求中未经解析、未经重排的 resp_data 原文。 */
    public static boolean verifyAsyncResponseData(String rawResponseData,
                                                  String encodedSignature,
                                                  PublicKey publicKey) {
        if (rawResponseData == null) {
            throw new HuifuSignatureException("汇付异步 resp_data 不能为空");
        }
        return verifyRaw(rawResponseData, encodedSignature, publicKey);
    }

    public static String signRaw(String rawData, PrivateKey privateKey) {
        if (rawData == null || privateKey == null) {
            throw new HuifuSignatureException("汇付签名原文或私钥不能为空");
        }
        try {
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(rawData.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new HuifuSignatureException("汇付 RSA2 加签失败", e);
        }
    }

    public static boolean verifyRaw(String rawData, String encodedSignature, PublicKey publicKey) {
        if (rawData == null || isBlank(encodedSignature) || publicKey == null) {
            return false;
        }
        try {
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(rawData.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(encodedSignature));
        } catch (Exception e) {
            return false;
        }
    }

    /** 读取 PKCS#8 私钥文件，支持 PEM 或纯 Base64。 */
    public static PrivateKey loadPrivateKey(Path keyFile) {
        String content = readKeyFile(keyFile, "私钥");
        if (content.contains("BEGIN RSA PRIVATE KEY")) {
            throw new HuifuConfigurationException("汇付私钥必须为 PKCS#8 格式，不能使用 PKCS#1");
        }
        String base64 = stripPem(content, "PRIVATE KEY");
        try {
            byte[] encoded = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encoded));
        } catch (Exception e) {
            throw new HuifuConfigurationException("汇付 PKCS#8 私钥文件格式错误", e);
        }
    }

    /** 读取 X.509 公钥文件，支持 PEM 或纯 Base64。 */
    public static PublicKey loadPublicKey(Path keyFile) {
        String content = readKeyFile(keyFile, "公钥");
        String base64 = stripPem(content, "PUBLIC KEY");
        try {
            byte[] encoded = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
        } catch (Exception e) {
            throw new HuifuConfigurationException("汇付 X.509 公钥文件格式错误", e);
        }
    }

    private static ObjectMapper createCanonicalMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false);
        mapper.getFactory().configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
        return mapper;
    }

    private static String readKeyFile(Path keyFile, String keyType) {
        if (keyFile == null) {
            throw new HuifuConfigurationException("汇付" + keyType + "文件未配置");
        }
        try {
            if (!Files.isRegularFile(keyFile)) {
                throw new HuifuConfigurationException("汇付" + keyType + "文件不存在或不可读");
            }
            long size = Files.size(keyFile);
            if (size <= 0 || size > MAX_KEY_FILE_BYTES) {
                throw new HuifuConfigurationException("汇付" + keyType + "文件大小异常");
            }
            return new String(Files.readAllBytes(keyFile), StandardCharsets.UTF_8).trim();
        } catch (HuifuConfigurationException e) {
            throw e;
        } catch (IOException e) {
            throw new HuifuConfigurationException("读取汇付" + keyType + "文件失败", e);
        }
    }

    private static String stripPem(String content, String keyType) {
        return content
                .replace("-----BEGIN " + keyType + "-----", "")
                .replace("-----END " + keyType + "-----", "")
                .replaceAll("\\s", "");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
