package com.atcumt.oss.controller.v1;

import com.atcumt.model.common.Result;
import com.atcumt.model.oss.vo.FileInfoVO;
import com.atcumt.oss.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
            @Parameter(name = "file", description = "媒体文件", required = true)
    })
    public Result<FileInfoVO> uploadFile(MultipartFile file) throws Exception {
        log.info("上传媒体文件, {}", file.getOriginalFilename());
        FileInfoVO fileInfoVOs = fileService.uploadMediaFile(file);
        return Result.success(fileInfoVOs);
    }

    // 上传文件列表并返回相关信息
    @PostMapping("/upload/media/list")
    @Operation(summary = "上传媒体文件列表", description = "上传媒体文件列表，仅能上传广义上的图片和视频")
    @Parameters({
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
