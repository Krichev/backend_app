package com.my.challenger.service.vibration;

import com.my.challenger.dto.vibration.*;
import com.my.challenger.entity.enums.SongStatus;
import com.my.challenger.entity.vibration.VibrationSong;
import com.my.challenger.exception.SongNotFoundException;
import com.my.challenger.mapper.VibrationQuizMapper;
import com.my.challenger.repository.vibration.VibrationSongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VibrationSongService {

    private final VibrationSongRepository repository;
    private final VibrationQuizMapper mapper = VibrationQuizMapper.INSTANCE;

    public Page<VibrationSongDTO> getSongs(int page, int size, String difficulty, String category, String sortBy, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // This is a simplified version, should handle nulls and proper filtering
        return repository.findAll(pageable).map(mapper::toDTO);
    }

    public List<VibrationSongDTO> getRandomSongs(int count, String difficulty, String category, List<Long> excludeIds) {
        List<VibrationSong> songs = repository.findRandomSongs(count, difficulty, category, excludeIds);
        return mapper.toDTOList(songs);
    }

    public VibrationSongDTO createSong(CreateVibrationSongRequest request, String creatorId) {
        VibrationSong song = mapper.toEntity(request);
        song.setCreatorId(creatorId);
        song.setStatus(SongStatus.PENDING);
        return mapper.toDTO(repository.save(song));
    }

    public VibrationSongDTO updateSong(Long id, UpdateVibrationSongRequest request) {
        VibrationSong song = repository.findById(id)
                .orElseThrow(() -> new SongNotFoundException(id));
        
        if (request.getSongTitle() != null) song.setSongTitle(request.getSongTitle());
        if (request.getArtist() != null) song.setArtist(request.getArtist());
        if (request.getCategory() != null) song.setCategory(request.getCategory());
        if (request.getReleaseYear() != null) song.setReleaseYear(request.getReleaseYear());
        if (request.getDifficulty() != null) song.setDifficulty(request.getDifficulty());
        if (request.getRhythmPattern() != null) song.setRhythmPattern(request.getRhythmPattern());
        if (request.getExcerptDurationMs() != null) song.setExcerptDurationMs(request.getExcerptDurationMs());
        if (request.getWrongAnswers() != null) song.setWrongAnswers(request.getWrongAnswers());
        if (request.getHint() != null) song.setHint(request.getHint());
        if (request.getVisibility() != null) song.setVisibility(request.getVisibility());
        
        return mapper.toDTO(repository.save(song));
    }

    public void deleteSong(Long id) {
        repository.deleteById(id);
    }

    public List<CategoryDTO> getCategories() {
        return repository.getCategoryCounts().stream()
                .map(objs -> CategoryDTO.builder()
                        .name((String) objs[0])
                        .songCount((Long) objs[1])
                        .build())
                .collect(Collectors.toList());
    }

    public void approveSong(Long id) {
        VibrationSong song = repository.findById(id)
                .orElseThrow(() -> new SongNotFoundException(id));
        song.setStatus(SongStatus.APPROVED);
        repository.save(song);
    }

    public void rejectSong(Long id) {
        VibrationSong song = repository.findById(id)
                .orElseThrow(() -> new SongNotFoundException(id));
        song.setStatus(SongStatus.REJECTED);
        repository.save(song);
    }
}
