package com.my.challenger.service.impl.photo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class PhotoOptimizationService {
    
    @Async
    public CompletableFuture<byte[]> generateThumbnail(byte[] originalImage, int width, int height) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(originalImage));
            BufferedImage thumbnail = resizeImage(original, width, height);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "jpg", baos);
            
            return CompletableFuture.completedFuture(baos.toByteArray());
        } catch (IOException e) {
            log.error("Failed to generate thumbnail", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Async
    public CompletableFuture<byte[]> compressImage(byte[] originalImage, float quality) {
        try {
            // Implement JPEG compression with specified quality
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(originalImage));
            
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            
            // Use ImageIO with compression settings
            var writers = ImageIO.getImageWritersByFormatName("jpg");
            var writer = writers.next();
            var ios = ImageIO.createImageOutputStream(compressed);
            writer.setOutput(ios);
            
            var param = writer.getDefaultWriteParam();
            param.setCompressionMode(param.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            
            writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
            writer.dispose();
            ios.close();
            
            return CompletableFuture.completedFuture(compressed.toByteArray());
        } catch (IOException e) {
            log.error("Failed to compress image", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return resizedImage;
    }
}