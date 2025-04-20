package com.atcumt.oss.utils;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtil {
    public static InputStream normalizeAvatar(InputStream originalImage) throws IOException {
        return normalizeAvatar(ImageIO.read(originalImage));
    }

    public static InputStream normalizeAvatar(BufferedImage originalImage) throws IOException {
        if (originalImage == null) {
            throw new IOException("无法读取图片");
        }

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        int length = Math.min(width, height);

        // 创建输出流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 使用 Thumbnails 进行 JPG 转换 + 无损压缩
        Thumbnails.of(originalImage)
                .size(length, length)
                .sourceRegion(Positions.CENTER, length, length)
                .outputFormat("jpg")
                .outputQuality(0.85f)
                .toOutputStream(outputStream);

        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    public static BufferedImage compressImage(InputStream fileStream, int width, int height, String format) throws IOException {
        BufferedImage bufferedImage = Thumbnails.of(fileStream)
                .size(width, height)
                .outputFormat(format)
                .outputQuality(0.7f)
                .asBufferedImage();
        // 轻微锐化（温和）
        float[] sharpenMatrix = {
                0f, -0.2f, 0f,
                -0.2f,  1.8f, -0.2f,
                0f, -0.2f, 0f
        };
        Kernel sharpenKernel = new Kernel(3, 3, sharpenMatrix);
        ConvolveOp sharpenOp = new ConvolveOp(sharpenKernel, ConvolveOp.EDGE_NO_OP, null);

        return sharpenOp.filter(bufferedImage, null);
    }

    public static InputStream bufferedImageToInputStream(BufferedImage image, String formatName) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, formatName, os);
        return new ByteArrayInputStream(os.toByteArray());
    }

    public static long getImageSize(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        inputStream.transferTo(outputStream);
        inputStream.reset();
        return outputStream.size();
    }

    public static InputStream copyInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        inputStream.transferTo(outputStream);
        inputStream.reset();
        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}