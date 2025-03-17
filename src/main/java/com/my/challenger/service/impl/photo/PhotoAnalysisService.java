package com.my.challenger.service.impl.photo;

import org.opencv.core.*;
import org.opencv.features2d.*;
import org.opencv.imgcodecs.Imgcodecs;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class PhotoAnalysisService {

    private final RekognitionClient rekognitionClient;

    /** Constructor to initialize the AWS Rekognition client */
    public PhotoAnalysisService(String awsAccessKey, String awsSecretKey, Region awsRegion) {
        this.rekognitionClient = RekognitionClient.builder()
                .region(awsRegion)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(awsAccessKey, awsSecretKey)))
                .build();
    }

    /** Load OpenCV native library */
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    /**
     * Verifies if a photo matches a text description.
     * @param photoPath Path to the photo file
     * @param description Space-separated keywords (e.g., "person shirt")
     * @return Map with success status, result, and message
     */
    public Map<String, Object> verifyPhotoWithDescription(String photoPath, String description) {
        try {
            // Read photo bytes
            byte[] photoBytes = Files.readAllBytes(Paths.get(photoPath));

            // Detect labels using AWS Rekognition
            List<String> labels = detectLabels(photoBytes).stream()
                    .map(Label::name)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            // Split description into keywords
            String[] descKeywords = description.toLowerCase().split("\\s+");
            List<String> missingKeywords = new ArrayList<>();

            // Check if all keywords are present in the labels
            for (String keyword : descKeywords) {
                if (!labels.contains(keyword)) {
                    missingKeywords.add(keyword);
                }
            }

            if (missingKeywords.isEmpty()) {
                return createResult(true, true, "Photo matches description");
            } else {
                String message = "Photo does not match description: missing " + String.join(", ", missingKeywords);
                return createResult(true, false, message);
            }
        } catch (Exception e) {
            return createErrorResult("Error verifying photo with description: " + e.getMessage());
        }
    }

    /**
     * Compares two photos to determine if the shirts are different.
     * @param photoPath1 Path to the first photo
     * @param photoPath2 Path to the second photo
     * @return Map with success status, result (true if shirts are different), and message
     */
    public Map<String, Object> compareShirtsInPhotos(String photoPath1, String photoPath2) {
        try {
            // Read photo bytes
            byte[] photoBytes1 = Files.readAllBytes(Paths.get(photoPath1));
            byte[] photoBytes2 = Files.readAllBytes(Paths.get(photoPath2));

            // Load images with OpenCV
            Mat image1 = Imgcodecs.imdecode(new MatOfByte(photoBytes1), Imgcodecs.IMREAD_COLOR);
            Mat image2 = Imgcodecs.imdecode(new MatOfByte(photoBytes2), Imgcodecs.IMREAD_COLOR);

            // Get person's bounding box for both images
            BoundingBox bbox1 = getPersonBoundingBox(photoBytes1);
            BoundingBox bbox2 = getPersonBoundingBox(photoBytes2);

            if (bbox1 == null || bbox2 == null) {
                return createResult(true, false, "Cannot determine—missing person in one or both photos");
            }

            // Compute crop regions for shirt area (top 40% of person bounding box)
            int imgHeight1 = image1.rows();
            int imgWidth1 = image1.cols();
            int x1 = (int) (bbox1.left() * imgWidth1);
            int y1 = (int) (bbox1.top() * imgHeight1);
            int w1 = (int) (bbox1.width() * imgWidth1);
            int h1 = (int) (bbox1.height() * imgHeight1);
            int hCrop1 = (int) (0.4 * h1);

            int imgHeight2 = image2.rows();
            int imgWidth2 = image2.cols();
            int x2 = (int) (bbox2.left() * imgWidth2);
            int y2 = (int) (bbox2.top() * imgHeight2);
            int w2 = (int) (bbox2.width() * imgWidth2);
            int h2 = (int) (bbox2.height() * imgHeight2);
            int hCrop2 = (int) (0.4 * h2);

            // Crop shirt areas
            Mat shirt1 = image1.submat(y1, y1 + hCrop1, x1, x1 + w1);
            Mat shirt2 = image2.submat(y2, y2 + hCrop2, x2, x2 + w2);

            // Extract ORB features
            Feature2D orb = ORB.create();
            MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
            Mat descriptors1 = new Mat();
            orb.detectAndCompute(shirt1, new Mat(), keypoints1, descriptors1);

            MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
            Mat descriptors2 = new Mat();
            orb.detectAndCompute(shirt2, new Mat(), keypoints2, descriptors2);

            // Check if features could be extracted
            if (descriptors1.empty() || descriptors2.empty()) {
                return createResult(true, false, "Cannot determine—unable to extract features from one or both shirts");
            }

            // Match descriptors
            DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
            MatOfDMatch matches = new MatOfDMatch();
            matcher.match(descriptors1, descriptors2, matches);

            // Apply ratio test to find good matches
            List<DMatch> goodMatches = new ArrayList<>();
            for (DMatch match : matches.toList()) {
                if (match.distance < 75) { // Threshold for good matches
                    goodMatches.add(match);
                }
            }

            // Determine if shirts are different based on number of good matches
            int numGoodMatches = goodMatches.size();
            boolean shirtsDifferent = numGoodMatches < 50; // Adjust threshold as needed
            String message = shirtsDifferent ?
                    "The shirts in the two photos are different" :
                    "The shirts in the two photos are similar";

            return createResult(true, shirtsDifferent, message);
        } catch (Exception e) {
            return createErrorResult("Error comparing shirts in photos: " + e.getMessage());
        }
    }

    /** Helper method to detect labels using AWS Rekognition */
    private List<Label> detectLabels(byte[] imageBytes) {
        DetectLabelsRequest request = DetectLabelsRequest.builder()
                .image(Image.builder().bytes(SdkBytes.fromByteArray(imageBytes)).build())
                .maxLabels(10)
                .minConfidence(80F)
                .build();
        DetectLabelsResponse response = rekognitionClient.detectLabels(request);
        return response.labels();
    }

    /** Helper method to get the bounding box of a person in the photo */
    private BoundingBox getPersonBoundingBox(byte[] imageBytes) {
        List<Label> labels = detectLabels(imageBytes);
        for (Label label : labels) {
            if (label.name().equalsIgnoreCase("Person") && !label.instances().isEmpty()) {
                return label.instances().get(0).boundingBox();
            }
        }
        return null;
    }

    /** Helper method to create a successful result map */
    private Map<String, Object> createResult(boolean success, boolean result, String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", success);
        map.put("result", result);
        map.put("message", message);
        return map;
    }

    /** Helper method to create an error result map */
    private Map<String, Object> createErrorResult(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("result", null);
        map.put("message", message);
        map.put("error", true);
        return map;
    }
}