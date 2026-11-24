package com.exam.file;

import com.exam.common.BizException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final MinioClient minioClient;

    @Value("${app.minio.bucket}")
    private String bucket;

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择文件");
        }
        String original = file.getOriginalFilename() == null ? "image.png" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED.contains(ext)) {
            throw new BizException("只允许上传 jpg、png、webp、gif");
        }
        String object = UUID.randomUUID() + "." + ext;
        try {
            ensureBucket();
            try (InputStream in = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(object)
                        .stream(in, file.getSize(), -1)
                        .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                        .build());
            }
            return object;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException("文件上传失败: " + ex.getMessage());
        }
    }

    public InputStream open(String object) {
        if (object == null || object.contains("..") || object.contains("/") || object.contains("\\")) {
            throw new BizException(404, "文件不存在");
        }
        try {
            return minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(object).build());
        } catch (Exception ex) {
            throw new BizException(404, "文件不存在");
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
