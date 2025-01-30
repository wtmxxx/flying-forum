package com.atcumt.oss.controller.v1;

import cn.dev33.satoken.stp.StpUtil;
import com.atcumt.common.enums.PermAction;
import com.atcumt.common.enums.PermModule;
import com.atcumt.common.utils.PermissionUtil;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.oss.dto.FileInfoDTO;
import com.atcumt.model.oss.vo.FileInfoVO;
import com.atcumt.oss.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController("fileControllerV1")
@RequestMapping("/api/file/v1")
@Tag(name = "File", description = "文件相关接口")
@RequiredArgsConstructor
@Slf4j
public class FileController {
    private final FileService fileService;

    // 上传文件并返回相关信息
    @PostMapping("/upload/media")
    @Operation(summary = "上传媒体文件", description = "上传媒体文件，仅能上传广义上的图片和视频")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "file", description = "媒体文件", required = true)
    })
    public Result<FileInfoVO> uploadFile(MultipartFile file) throws Exception {
        log.info("上传媒体文件, {}", file.getOriginalFilename());
        FileInfoVO fileInfoVO = fileService.uploadMediaFile(file);
        return Result.success(fileInfoVO);
    }

    // 上传文件列表并返回相关信息
    @PostMapping("/upload/media/list")
    @Operation(summary = "上传媒体文件列表", description = "上传媒体文件列表，仅能上传广义上的图片和视频")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "files", description = "媒体文件列表", required = true)
    })
    public Result<List<FileInfoVO>> uploadFile(List<MultipartFile> files) throws Exception {
        log.info("上传媒体文件列表, {}", files.stream()
                .map(MultipartFile::getOriginalFilename)
                .filter(filename -> filename != null && !filename.isEmpty())
                .collect(Collectors.joining(", ")));
        List<FileInfoVO> fileInfoVOs = fileService.uploadMediaFiles(files);
        return Result.success(fileInfoVOs);
    }

    // 上传文件并返回相关信息
    @PostMapping("/upload/avatar")
    @Operation(summary = "上传头像", description = "上传头像，仅能上传图片")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "file", description = "头像图片", required = true)
    })
    public Result<FileInfoVO> uploadAvatar(MultipartFile file) throws Exception {
        log.info("上传头像, {}", file.getOriginalFilename());
        FileInfoVO fileInfoVO = fileService.uploadAvatar(file);
        return Result.success(fileInfoVO);
    }

    @GetMapping("/{bucket}/{filename}")
    @Operation(summary = "获取文件", description = "获取文件")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "bucket", description = "存储桶", required = true),
            @Parameter(name = "filename", description = "文件名", required = true),
            @Parameter(name = "download", description = "是否下载")
    })
    public void getFile(HttpServletResponse response,
                        @PathVariable(name = "bucket") String bucket,
                        @PathVariable(name = "filename") String filename,
                        @RequestParam(name = "download", defaultValue = "false") boolean download
    ) {
        log.info("获取文件, bucket: {}, filename: {}", bucket, filename);
        if (!download) {
            fileService.getFile(response, bucket, filename);
        } else {
            fileService.downloadFile(response, bucket, filename);
        }
    }

    @DeleteMapping("/{bucket}/{filename}")
    @Operation(summary = "删除文件", description = "删除文件")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "bucket", description = "存储桶", required = true),
            @Parameter(name = "filename", description = "文件名", required = true)
    })
    public void getFile(@PathVariable(name = "bucket") String bucket,
                        @PathVariable(name = "filename") String filename
    ) throws Exception {
        log.info("删除文件, bucket: {}, filename: {}", bucket, filename);
        // 权限鉴定
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.FILE, PermAction.DELETE));

        fileService.deleteFile(FileInfoDTO
                .builder()
                .bucket(bucket)
                .fileName(filename)
                .build());
    }

    // 上传文件并返回相关信息
//    @PostMapping("/upload/all")
//    @Operation(summary = "上传文件", description = "上传文件")
//    @Parameters({
//            @Parameter(name = "file", description = "文件", required = true)
//    })
//    public Result<FileInfoVO> uploadFile(MultipartFile file) throws Exception {
//        log.info("上传文件, {}", file.getOriginalFilename());
//        FileInfoVO fileInfoVOs = fileService.uploadAllFile(file);
//        return Result.success(fileInfoVOs);
//    }

    // 上传文件列表并返回相关信息
//    @PostMapping("/upload/all/list")
//    @Operation(summary = "上传文件列表", description = "上传文件列表")
//    @Parameters({
//            @Parameter(name = "files", description = "文件列表", required = true)
//    })
//    public Result<List<FileInfoVO>> uploadFile(List<MultipartFile> files) throws Exception {
//        log.info("上传文件列表, {}", files.stream()
//                .map(MultipartFile::getOriginalFilename)
//                .filter(filename -> filename != null && !filename.isEmpty())
//                .collect(Collectors.joining(", ")));
//        List<FileInfoVO> fileInfoVOs = fileService.uploadAllFiles(files);
//        return Result.success(fileInfoVOs);
//    }
}
