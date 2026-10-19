package com.backstage.system.utils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.backstage.common.config.OssProperties;
import com.backstage.common.utils.ServletUtils;
import com.backstage.common.utils.ip.IpUtils;
import com.backstage.system.domain.vo.common.OssOperationLogVo;
import com.backstage.system.exception.UpLoadException;
import com.backstage.system.mapper.common.OssMapper;
import com.backstage.system.service.common.OssService;
import eu.bitwalker.useragentutils.UserAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class OssUtil {

    @Autowired
    private OssMapper ossMapper;

    @Autowired
    private OssProperties ossProperties;


    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private OssService ossService;


    // 创建连接
    public AmazonS3Client createS3Client() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(
                ossProperties.getAccessKey(),
                ossProperties.getSecretKey()
        );
        AmazonS3Client s3 = new AmazonS3Client(credentials);
        s3.setEndpoint(ossProperties.getEndpoint());
        s3.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build());
        return s3;
    }

    public boolean bucketExists(String bucketName) {
        AmazonS3Client s3 = createS3Client();
        return s3.doesBucketExistV2(bucketName);
    }

    public void ensureBucket(String bucketName) {
        AmazonS3Client s3 = createS3Client();
        if (!s3.doesBucketExistV2(bucketName)) {
            s3.createBucket(bucketName);
        }
    }

    public boolean objectExists(String bucketName, String fileName) {
        AmazonS3Client s3 = createS3Client();
        return s3.doesObjectExist(bucketName, fileName);
    }

    public ObjectMetadata getObjectMetadata(String bucketName, String fileName) {
        AmazonS3Client s3 = createS3Client();
        return s3.getObjectMetadata(bucketName, fileName);
    }

    public List<S3ObjectSummary> listObjects(String bucketName, String prefix, int limit) {
        AmazonS3Client s3 = createS3Client();
        List<S3ObjectSummary> summaries = new ArrayList<>();
        String token = null;
        int maxKeys = Math.max(1, Math.min(limit, 1000));
        do {
            ListObjectsV2Request request = new ListObjectsV2Request()
                    .withBucketName(bucketName)
                    .withPrefix(prefix == null ? "" : prefix)
                    .withMaxKeys(Math.min(maxKeys, limit - summaries.size()))
                    .withContinuationToken(token);
            ListObjectsV2Result result = s3.listObjectsV2(request);
            summaries.addAll(result.getObjectSummaries());
            token = result.getNextContinuationToken();
        } while (token != null && summaries.size() < limit);
        if (summaries.size() > limit) {
            return summaries.subList(0, limit);
        }
        return summaries;
    }

    public void copyObject(String sourceBucket, String sourceKey, String targetBucket, String targetKey) {
        AmazonS3Client s3 = createS3Client();
        CopyObjectRequest request = new CopyObjectRequest(sourceBucket, sourceKey, targetBucket, targetKey);
        s3.copyObject(request);
    }

    public void downloadObject(String bucketName, String objectKey, File targetFile) throws Exception {
        AmazonS3Client s3 = createS3Client();
        S3Object object = s3.getObject(bucketName, objectKey);
        try {
            InputStream input = object.getObjectContent();
            Files.copy(input, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            object.close();
        }
    }

    public void uploadObject(String bucketName, String objectKey, File sourceFile, String contentType) throws Exception {
        AmazonS3Client s3 = createS3Client();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(sourceFile.length());
        if (contentType != null && !contentType.trim().isEmpty()) {
            metadata.setContentType(contentType);
        }
        try (FileInputStream input = new FileInputStream(sourceFile)) {
            PutObjectRequest request = new PutObjectRequest(bucketName, objectKey, input, metadata);
            s3.putObject(request);
        }
    }

    public void deleteObject(String bucketName, String objectKey) {
        AmazonS3Client s3 = createS3Client();
        s3.deleteObject(bucketName, objectKey);
    }

    // 上传文件默认配置路径
    public String uploadFile(MultipartFile file) throws Exception {
        return uploadFile(file, null);
    }





    // 上传文件 带路径
    public String uploadFile(MultipartFile file, String customPath) throws Exception, UpLoadException {
        AmazonS3Client s3 = createS3Client();

        String path = customPath != null ? customPath : ossProperties.getBasePath();

        String fileName = path + UUID.randomUUID() + "_" + file.getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        PutObjectRequest request = new PutObjectRequest(
                ossProperties.getBucketName(),
                fileName,
                file.getInputStream(),
                metadata
        );

        s3.putObject(request);



        ossService.insertMapper(file, fileName);

        return fileName;
    }

    // 生成文件名 不带前缀
    public String generateFileName(MultipartFile file) {
        return generateFileName(file, null);
    }

    // 生成文件名 带前缀
    public String generateFileName(MultipartFile file, String prefix) {
        String originalFilename = file.getOriginalFilename();
        if (prefix != null && !prefix.isEmpty()) {
            return prefix + "_" + UUID.randomUUID() + "_" + originalFilename;
        }
        return UUID.randomUUID() + "_" + originalFilename;
    }

    // 获取文件完整路径
    public String getFullFilePath(String fileName) {
        // 如果fileName已经包含basePath，直接拼接publicDomain
        // 否则拼接basePath后再拼接publicDomain
        String fullPath;
        if (fileName.startsWith(ossProperties.getBasePath())) {
            fullPath = fileName;
        } else {
            fullPath = ossProperties.getBasePath() + fileName;
        }
        return ossProperties.getPublicDomain() + "/" + fullPath;
    }

    // 获取OSS配置信息
    public OssProperties getOssProperties() {
        return ossProperties;
    }

    // 删除文件
    public boolean deleteFile(String fileName) {
        try {
            AmazonS3Client s3 = createS3Client();
            s3.deleteObject(ossProperties.getBucketName(), fileName);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 判断文件是否存在
    public boolean doesFileExist(String fileName) {
        AmazonS3Client s3 = createS3Client();
        return s3.doesObjectExist(ossProperties.getBucketName(), fileName);
    }

    // 重命名文件 本质是删除再上传
    public void renameFile(String oldFileName, String newFileName) {
        try {
            AmazonS3Client s3 = createS3Client();

            CopyObjectRequest copyRequest = new CopyObjectRequest(
                    ossProperties.getBucketName(),
                    oldFileName,
                    ossProperties.getBucketName(),
                    newFileName
            );
            s3.copyObject(copyRequest);

            s3.deleteObject(ossProperties.getBucketName(), oldFileName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("重命名失败：" + e.getMessage());
        }
    }



    // 获取文件签名URL
    public String getSignedUrl(String fileKey, int expireMinutes) {
        return getSignedUrl(ossProperties.getBucketName(), fileKey, expireMinutes);
    }

    public String getInlinePreviewSignedUrl(String fileKey, int expireMinutes, String contentType) {
        return getInlinePreviewSignedUrl(ossProperties.getBucketName(), fileKey, expireMinutes, contentType);
    }

    public String getAttachmentDownloadSignedUrl(String fileKey, int expireMinutes, String contentType, String fileName) {
        return getAttachmentDownloadSignedUrl(ossProperties.getBucketName(), fileKey, expireMinutes, contentType, fileName);
    }

    public String getSignedUrl(String bucketName, String fileKey, int expireMinutes) {
        AmazonS3Client s3 = createS3Client();

        // 过期时间 分钟*60*1000 毫秒
        long expireTime = System.currentTimeMillis() + (long) expireMinutes * 60 * 1000;
        Date expiration = new Date(expireTime);

        // 对 fileKey 做 URL 解码，避免已编码的中文字符被 S3 SDK 二次编码导致签名失败
        String decodedKey = fileKey;
        try {
            decodedKey = java.net.URLDecoder.decode(fileKey, "UTF-8");
        } catch (Exception ignored) {
            // 解码失败时使用原始 key
        }

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                bucketName,
                decodedKey
        ).withExpiration(expiration);

        return s3.generatePresignedUrl(request).toString();
    }

    public String getInlinePreviewSignedUrl(String bucketName, String fileKey, int expireMinutes, String contentType) {
        AmazonS3Client s3 = createS3Client();

        long expireTime = System.currentTimeMillis() + (long) expireMinutes * 60 * 1000;
        Date expiration = new Date(expireTime);

        String decodedKey = fileKey;
        try {
            decodedKey = java.net.URLDecoder.decode(fileKey, "UTF-8");
        } catch (Exception ignored) {
            // keep raw key on decode failure
        }

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                bucketName,
                decodedKey
        ).withExpiration(expiration);

        ResponseHeaderOverrides overrides = new ResponseHeaderOverrides();
        overrides.setContentDisposition("inline");
        if (contentType != null && !contentType.trim().isEmpty()) {
            overrides.setContentType(contentType);
        }
        request.setResponseHeaders(overrides);

        return s3.generatePresignedUrl(request).toString();
    }

    public String getAttachmentDownloadSignedUrl(String bucketName,
                                                 String fileKey,
                                                 int expireMinutes,
                                                 String contentType,
                                                 String fileName) {
        AmazonS3Client s3 = createS3Client();

        long expireTime = System.currentTimeMillis() + (long) expireMinutes * 60 * 1000;
        Date expiration = new Date(expireTime);

        String decodedKey = fileKey;
        try {
            decodedKey = java.net.URLDecoder.decode(fileKey, "UTF-8");
        } catch (Exception ignored) {
            // keep raw key on decode failure
        }

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                bucketName,
                decodedKey
        ).withExpiration(expiration);

        ResponseHeaderOverrides overrides = new ResponseHeaderOverrides();
        overrides.setContentDisposition(buildAttachmentDisposition(fileName));
        if (contentType != null && !contentType.trim().isEmpty()) {
            overrides.setContentType(contentType);
        }
        request.setResponseHeaders(overrides);

        return s3.generatePresignedUrl(request).toString();
    }

    private String buildAttachmentDisposition(String fileName) {
        String normalizedName = (fileName == null || fileName.trim().isEmpty()) ? "document.pdf" : fileName.trim();
        String asciiName = normalizedName.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "_");
        String encodedName;
        try {
            encodedName = URLEncoder.encode(normalizedName, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("UTF-8 is not supported", ex);
        }
        return "attachment; filename=\"" + asciiName + "\"; filename*=UTF-8''" + encodedName;
    }

}
