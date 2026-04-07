package com.rural.ecommerce.controller;

import com.rural.ecommerce.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/file")
public class FileController {

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (file.isEmpty()) {
            return Result.error("请选择文件");
        }

        try {
            // 使用项目根目录下的 uploads 文件夹
            String projectRoot = System.getProperty("user.dir");
            Path uploadDir = Paths.get(projectRoot, "uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

            // 保存文件
            Path filePath = uploadDir.resolve(newFilename);
            file.transferTo(filePath.toFile());

            // 构建访问URL
            String url = "/uploads/" + newFilename;

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("filename", newFilename);

            return Result.success(result);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }
}
