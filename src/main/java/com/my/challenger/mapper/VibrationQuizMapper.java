package com.my.challenger.mapper;

import com.my.challenger.dto.vibration.*;
import com.my.challenger.entity.vibration.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface VibrationQuizMapper {

    VibrationQuizMapper INSTANCE = Mappers.getMapper(VibrationQuizMapper.class);

    VibrationSongDTO toDTO(VibrationSong entity);

    List<VibrationSongDTO> toDTOList(List<VibrationSong> entities);

    VibrationSongSummaryDTO toSummaryDTO(VibrationSong entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @Mapping(target = "playCount", ignore = true)
    @Mapping(target = "correctGuesses", ignore = true)
    @Mapping(target = "totalAttempts", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    VibrationSong toEntity(CreateVibrationSongRequest request);

    @Mapping(target = "questions", source = "questions")
    GameSessionDTO toDTO(VibrationGameSession entity);

    default VibrationSongDTO map(VibrationSessionQuestion sessionQuestion) {
        if (sessionQuestion == null || sessionQuestion.getSong() == null) {
            return null;
        }
        return toDTO(sessionQuestion.getSong());
    }

    @Mapping(target = "songId", source = "song.id")
    @Mapping(target = "songTitle", source = "song.songTitle")
    @Mapping(target = "artist", source = "song.artist")
    RoundResultDTO toRoundResultDTO(VibrationSessionQuestion entity);

    @Mapping(target = "song", source = "entity")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "session", ignore = true)
    @Mapping(target = "questionOrder", ignore = true)
    @Mapping(target = "selectedAnswer", ignore = true)
    @Mapping(target = "isCorrect", ignore = true)
    @Mapping(target = "responseTimeMs", ignore = true)
    @Mapping(target = "replaysUsed", ignore = true)
    @Mapping(target = "points_earned", ignore = true)
    @Mapping(target = "answeredAt", ignore = true)
    VibrationSessionQuestion toSessionQuestion(VibrationSong entity);
}
