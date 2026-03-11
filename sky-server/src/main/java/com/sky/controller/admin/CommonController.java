package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用接口
 */
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {
    @Autowired
    private AliOssUtil aliOssUtil;
    /**
     * 文件上传
     * @param file
     * @return
     */
    @ApiOperation("文件上传")
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) {
        log.info("文件上传：{}", file);
        try {
            String originalFilename = file.getOriginalFilename();
            // 截取原始文件名的后缀（如：.jpg、.png等）
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            // 生成新的文件名（避免文件名冲突）
            String newFileName = System.currentTimeMillis() + extension;
            // 调用阿里云OSS工具类上传文件
            String url = aliOssUtil.upload(file.getBytes(), newFileName);
            return Result.success(url);
        } catch (Exception e) {
            log.error("文件上传失败：{}", e.getMessage());
            return Result.error("文件上传失败");
        }
    }
}
