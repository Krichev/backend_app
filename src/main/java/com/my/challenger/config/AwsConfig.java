//package com.my.challenger.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
//import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
//import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
//import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
//import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.s3.S3AsyncClient;
//import software.amazon.awssdk.services.s3.S3Client;
//
//@Configuration
//public class AwsConfig {
//
//    @Value("${app.storage.s3.access-key}")
//    private String accessKey;
//
//    @Value("${app.storage.s3.secret-key}")
//    private String secretKey;
//
//    @Value("${app.storage.s3.region:us-east-1}")
//    private String region;
//
//    @Value("${app.storage.s3.endpoint:}")
//    private String endpoint;
//
//    @Bean
//    public S3Client s3Client() {
//        return S3Client.builder()
//                .region(Region.of(region))
//                .credentialsProvider(getCredentialsProvider())
//                .build();
//    }
//
//    private AwsCredentialsProvider getCredentialsProvider() {
//        // If access key and secret are provided, use them
//        if (!accessKey.isEmpty() && !secretKey.isEmpty()) {
//            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
//            return StaticCredentialsProvider.create(awsCredentials);
//        }
//        // Otherwise, use default credential chain (IAM roles, environment variables, etc.)
//        return DefaultCredentialsProvider.create();
//    }
//
//
//    //    @Bean
////    public S3Client s3Client() {
////        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
////
////        S3ClientBuilder builder = S3Client.builder()
////                .region(Region.of(region))
////                .credentialsProvider(StaticCredentialsProvider.create(credentials));
////
////        // For LocalStack or custom S3-compatible services
////        if (!endpoint.isEmpty()) {
////            builder.endpointOverride(URI.create(endpoint))
////                    .forcePathStyle(true); // Required for LocalStack
////        }
////
////        return builder.build();
////    }
////
//    @Bean
//    public S3AsyncClient s3AsyncClient() {
//        return S3AsyncClient.builder()
//                .region(Region.of(region))
//                .credentialsProvider(getCredentialsProvider())
//                .build();
//
////        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
////
////        S3AsyncClientBuilder builder = S3AsyncClient.builder()
////                .region(Region.of(region))
////                .credentialsProvider(StaticCredentialsProvider.create(credentials));
//
//        // For LocalStack or custom S3-compatible services
////        if (!endpoint.isEmpty()) {
////            builder.endpointOverride(URI.create(endpoint))
////                    .forcePathStyle(true);
////        }
//
////        return builder.build();
//    }
//
////    @Bean
////    public S3Presigner s3Presigner() {
////        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
////
////        S3Presigner.Builder builder = S3Presigner.builder()
////                .region(Region.of(region))
////                .credentialsProvider(StaticCredentialsProvider.create(credentials));
////
////        // For LocalStack or custom S3-compatible services
//////        if (!endpoint.isEmpty()) {
//////            builder.endpointOverride(URI.create(endpoint));
//////        }
////
////        return builder.build();
////    }
//}