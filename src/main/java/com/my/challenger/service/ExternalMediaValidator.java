package com.my.challenger.service;

import com.my.challenger.dto.quiz.CreateQuizQuestionRequest;
import com.my.challenger.entity.enums.MediaSourceType;
import com.my.challenger.util.YouTubeUrlParser;
import org.springframework.stereotype.Component;

@Component
public class ExternalMediaValidator {

    public void validate(CreateQuizQuestionRequest request) {
        if (request.getMediaSourceType() == MediaSourceType.YOUTUBE) {
            validateYouTubeUrl(request.getExternalMediaUrl());
        }
        
        validateTimeRanges(request);
    }

    private void validateYouTubeUrl(String url) {
        if (url == null || !YouTubeUrlParser.isYouTubeUrl(url)) {
            throw new IllegalArgumentException("Invalid YouTube URL provided");
        }
    }

    private void validateTimeRanges(CreateQuizQuestionRequest request) {
        if (request.getQuestionVideoStartTime() != null && request.getQuestionVideoEndTime() != null) {
            if (request.getQuestionVideoEndTime() <= request.getQuestionVideoStartTime()) {
                throw new IllegalArgumentException("Question video end time must be greater than start time");
            }
        }

        if (request.getAnswerVideoStartTime() != null && request.getAnswerVideoEndTime() != null) {
            if (request.getAnswerVideoEndTime() <= request.getAnswerVideoStartTime()) {
                throw new IllegalArgumentException("Answer video end time must be greater than start time");
            }
        }
    }
}
