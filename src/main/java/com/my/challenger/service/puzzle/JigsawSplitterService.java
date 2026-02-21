package com.my.challenger.service.puzzle;

import com.my.challenger.config.StorageProperties;
import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.enums.MediaCategory;
import com.my.challenger.entity.enums.MediaType;
import com.my.challenger.entity.enums.ProcessingStatus;
import com.my.challenger.entity.enums.PuzzleEdgeType;
import com.my.challenger.entity.puzzle.PuzzleGame;
import com.my.challenger.entity.puzzle.PuzzlePiece;
import com.my.challenger.repository.MediaFileRepository;
import com.my.challenger.service.BucketResolver;
import com.my.challenger.service.impl.MinioMediaStorageService;
import com.my.challenger.util.S3KeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class JigsawSplitterService {

    private final S3Client s3Client;
    private final MediaFileRepository mediaFileRepository;
    private final MinioMediaStorageService storageService;
    private final S3KeyGenerator s3KeyGenerator;
    private final BucketResolver bucketResolver;
    private final StorageProperties storageProperties;

    private final Random random = new Random();

    @Async
    @Transactional
    public CompletableFuture<List<PuzzlePiece>> splitImage(PuzzleGame game) {
        log.info("Starting jigsaw split for game {} | Grid: {}x{}", game.getId(), game.getGridRows(), game.getGridCols());
        
        try {
            byte[] imageBytes = storageService.downloadFromMinio(game.getSourceImage());
            BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            
            if (sourceImage == null) {
                throw new IOException("Failed to read source image");
            }

            int rows = game.getGridRows();
            int cols = game.getGridCols();
            int imgWidth = sourceImage.getWidth();
            int imgHeight = sourceImage.getHeight();
            
            int cellWidth = imgWidth / cols;
            int cellHeight = imgHeight / rows;
            
            // Generate edges
            PuzzleEdgeType[][] horizontalEdges = new PuzzleEdgeType[rows + 1][cols];
            PuzzleEdgeType[][] verticalEdges = new PuzzleEdgeType[rows][cols + 1];
            
            // Fill horizontal edges (top and bottom are FLAT, internal are TAB/BLANK)
            for (int r = 0; r <= rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (r == 0 || r == rows) {
                        horizontalEdges[r][c] = PuzzleEdgeType.FLAT;
                    } else {
                        horizontalEdges[r][c] = random.nextBoolean() ? PuzzleEdgeType.TAB : PuzzleEdgeType.BLANK;
                    }
                }
            }
            
            // Fill vertical edges (left and right are FLAT, internal are TAB/BLANK)
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c <= cols; c++) {
                    if (c == 0 || c == cols) {
                        verticalEdges[r][c] = PuzzleEdgeType.FLAT;
                    } else {
                        verticalEdges[r][c] = random.nextBoolean() ? PuzzleEdgeType.TAB : PuzzleEdgeType.BLANK;
                    }
                }
            }

            List<PuzzlePiece> pieces = new ArrayList<>();
            String bucket = bucketResolver.getBucket(MediaType.IMAGE);

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    PuzzlePiece piece = createPiece(game, sourceImage, r, c, cellWidth, cellHeight, 
                                                  horizontalEdges, verticalEdges, bucket);
                    pieces.add(piece);
                }
            }

            log.info("Successfully split image into {} pieces for game {}", pieces.size(), game.getId());
            return CompletableFuture.completedFuture(pieces);

        } catch (Exception e) {
            log.error("Failed to split image for game {}", game.getId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private PuzzlePiece createPiece(PuzzleGame game, BufferedImage sourceImage, int row, int col, 
                                   int cellWidth, int cellHeight, 
                                   PuzzleEdgeType[][] hEdges, PuzzleEdgeType[][] vEdges,
                                   String bucket) throws IOException {
        
        PuzzleEdgeType top = hEdges[row][col];
        PuzzleEdgeType bottom = hEdges[row + 1][col];
        PuzzleEdgeType left = vEdges[row][col];
        PuzzleEdgeType right = vEdges[row][col + 1];
        
        // Tab size is approx 20% of cell size
        double tabSize = Math.min(cellWidth, cellHeight) * 0.2;
        
        // The piece image needs to be larger than the cell to accommodate tabs protruding outward
        int offsetX = (int) tabSize;
        int offsetY = (int) tabSize;
        int pieceWidth = cellWidth + 2 * offsetX;
        int pieceHeight = cellHeight + 2 * offsetY;
        
        BufferedImage pieceImage = new BufferedImage(pieceWidth, pieceHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = pieceImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Build the jigsaw path
        GeneralPath path = new GeneralPath();
        double startX = offsetX;
        double startY = offsetY;
        
        path.moveTo(startX, startY);
        
        // Top edge (left to right)
        appendEdge(path, startX, startY, startX + cellWidth, startY, top, true);
        
        // Right edge (top to bottom)
        appendEdge(path, startX + cellWidth, startY, startX + cellWidth, startY + cellHeight, right, true);
        
        // Bottom edge (right to left)
        appendEdge(path, startX + cellWidth, startY + cellHeight, startX, startY + cellHeight, bottom, true);
        
        // Left edge (bottom to top)
        appendEdge(path, startX, startY + cellHeight, startX, startY, left, true);
        
        path.closePath();
        
        // Set clip and draw the portion of the source image
        g2.setClip(path);
        
        int srcX = col * cellWidth - offsetX;
        int srcY = row * cellHeight - offsetY;
        
        g2.drawImage(sourceImage, 0, 0, pieceWidth, pieceHeight, 
                     srcX, srcY, srcX + pieceWidth, srcY + pieceHeight, null);
        
        g2.dispose();
        
        // Upload piece to MinIO
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(pieceImage, "png", baos);
        byte[] pieceBytes = baos.toByteArray();
        
        String s3Key = s3KeyGenerator.generateKey(
            storageProperties.getEnvironment(),
            game.getCreator().getId(),
            "user",
            game.getId(),
            (long) (row * game.getGridCols() + col),
            MediaType.IMAGE,
            "png"
        );
        
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType("image/png")
                .build();
        s3Client.putObject(putRequest, RequestBody.fromBytes(pieceBytes));
        
        // Create MediaFile
        MediaFile mediaFile = MediaFile.builder()
                .originalFilename("piece_" + row + "_" + col + ".png")
                .filename(s3Key.substring(s3Key.lastIndexOf('/') + 1))
                .bucketName(bucket)
                .s3Key(s3Key)
                .filePath(s3Key)
                .fileSize((long) pieceBytes.length)
                .contentType("image/png")
                .mediaType(MediaType.IMAGE)
                .mediaCategory(MediaCategory.PUZZLE_PIECE)
                .processingStatus(ProcessingStatus.COMPLETED)
                .width(pieceWidth)
                .height(pieceHeight)
                .uploadedBy(game.getCreator().getId())
                .entityId(game.getId())
                .build();
        mediaFile.generateStorageKey();
        mediaFile = mediaFileRepository.save(mediaFile);
        
        // Create PuzzlePiece entity
        return PuzzlePiece.builder()
                .puzzleGame(game)
                .pieceIndex(row * game.getGridCols() + col)
                .gridRow(row)
                .gridCol(col)
                .pieceImage(mediaFile)
                .edgeTop(top)
                .edgeRight(right)
                .edgeBottom(bottom)
                .edgeLeft(left)
                .svgClipPath(toSvgPath(path, offsetX, offsetY))
                .widthPx(pieceWidth)
                .heightPx(pieceHeight)
                .build();
    }

    private void appendEdge(GeneralPath path, double x1, double y1, double x2, double y2, 
                           PuzzleEdgeType type, boolean clockwise) {
        if (type == PuzzleEdgeType.FLAT) {
            path.lineTo(x2, y2);
            return;
        }
        
        double midX = (x1 + x2) / 2;
        double midY = (y1 + y2) / 2;
        
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);
        
        // Normal vector (perpendicular to edge)
        double nx = -dy / length;
        double ny = dx / length;
        
        // Flip normal if it's a blank (inward)
        if (type == PuzzleEdgeType.BLANK) {
            nx = -nx;
            ny = -ny;
        }
        
        double tabDepth = length * 0.2;
        double tabWidth = length * 0.3;
        
        // Control points for a smooth rounded tab
        double cp1x = x1 + dx * 0.35;
        double cp1y = y1 + dy * 0.35;
        
        double cp2x = cp1x + nx * tabDepth;
        double cp2y = cp1y + ny * tabDepth;
        
        double cp3x = x1 + dx * 0.65 + nx * tabDepth;
        double cp3y = y1 + dy * 0.65 + ny * tabDepth;
        
        double cp4x = x1 + dx * 0.65;
        double cp4y = y1 + dy * 0.65;
        
        path.lineTo(cp1x, cp1y);
        path.curveTo(cp1x + nx * tabDepth * 0.5, cp1y + ny * tabDepth * 0.5,
                     cp2x - (dx / length) * tabWidth * 0.2, cp2y - (dy / length) * tabWidth * 0.2,
                     midX + nx * tabDepth, midY + ny * tabDepth);
        path.curveTo(cp3x + (dx / length) * tabWidth * 0.2, cp3y + (dy / length) * tabWidth * 0.2,
                     cp4x + nx * tabDepth * 0.5, cp4y + ny * tabDepth * 0.5,
                     cp4x, cp4y);
        path.lineTo(x2, y2);
    }

    private String toSvgPath(GeneralPath path, int offsetX, int offsetY) {
        // Simplified SVG path generator from GeneralPath
        // In a real scenario, we might use a more robust library, 
        // but for this task we'll generate basic path data.
        StringBuilder sb = new StringBuilder();
        java.awt.geom.PathIterator pi = path.getPathIterator(null);
        double[] coords = new double[6];
        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            switch (type) {
                case java.awt.geom.PathIterator.SEG_MOVETO:
                    sb.append(String.format("M %.2f %.2f ", coords[0] - offsetX, coords[1] - offsetY));
                    break;
                case java.awt.geom.PathIterator.SEG_LINETO:
                    sb.append(String.format("L %.2f %.2f ", coords[0] - offsetX, coords[1] - offsetY));
                    break;
                case java.awt.geom.PathIterator.SEG_CUBICTO:
                    sb.append(String.format("C %.2f %.2f, %.2f %.2f, %.2f %.2f ", 
                        coords[0] - offsetX, coords[1] - offsetY,
                        coords[2] - offsetX, coords[3] - offsetY,
                        coords[4] - offsetX, coords[5] - offsetY));
                    break;
                case java.awt.geom.PathIterator.SEG_CLOSE:
                    sb.append("Z");
                    break;
            }
            pi.next();
        }
        return sb.toString().trim();
    }
}
