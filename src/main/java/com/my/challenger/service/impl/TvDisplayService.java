package com.my.challenger.service.impl;

import com.my.challenger.config.JwtTokenUtil;
import com.my.challenger.dto.tv.TvDisplayClaimDTO;
import com.my.challenger.dto.tv.TvDisplayRegistrationDTO;
import com.my.challenger.dto.tv.TvDisplayStatusDTO;
import com.my.challenger.entity.TvDisplay;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.TvDisplayStatus;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.QuizSessionRepository;
import com.my.challenger.repository.TvDisplayRepository;
import com.my.challenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TvDisplayService {

    private final TvDisplayRepository tvDisplayRepository;
    private final UserRepository userRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;

    private static final String PAIRING_CODE_CHARSET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int PAIRING_CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    public TvDisplayRegistrationDTO register() {
        String pairingCode = generatePairingCode();
        
        // Create throwaway TV user
        String randomId = UUID.randomUUID().toString().substring(0, 8);
        String username = "tv_display_" + randomId;
        String email = username + "@challenger.tv";
        String password = UUID.randomUUID().toString();

        User tvUser = new User();
        tvUser.setUsername(username);
        tvUser.setEmail(email);
        tvUser.setPassword(passwordEncoder.encode(password));
        tvUser = userRepository.save(tvUser);

        String token = jwtTokenUtil.generateToken(username, tvUser.getId());

        TvDisplay tvDisplay = TvDisplay.builder()
                .pairingCode(pairingCode)
                .status(TvDisplayStatus.WAITING)
                .tvUserId(tvUser.getId())
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        tvDisplay = tvDisplayRepository.save(tvDisplay);

        log.info("📺 TV display registered: displayId={}, pairingCode={}", tvDisplay.getId(), pairingCode);

        return TvDisplayRegistrationDTO.builder()
                .displayId(tvDisplay.getId())
                .pairingCode(pairingCode)
                .token(token)
                .build();
    }

    @Transactional(readOnly = true)
    public TvDisplayStatusDTO getStatus(Long displayId) {
        TvDisplay tvDisplay = tvDisplayRepository.findById(displayId)
                .orElseThrow(() -> new ResourceNotFoundException("TV Display not found"));

        if (tvDisplay.getStatus() == TvDisplayStatus.WAITING && LocalDateTime.now().isAfter(tvDisplay.getExpiresAt())) {
            tvDisplay.setStatus(TvDisplayStatus.EXPIRED);
            tvDisplayRepository.save(tvDisplay);
        }

        return TvDisplayStatusDTO.builder()
                .status(tvDisplay.getStatus())
                .roomCode(tvDisplay.getRoomCode())
                .build();
    }

    public TvDisplayClaimDTO claim(String pairingCode, String roomCode, Long hostUserId) {
        // Validate room exists and user is the host
        quizSessionRepository.findByRoomCode(roomCode)
                .filter(session -> session.getHostUser().getId().equals(hostUserId))
                .orElseThrow(() -> new ResourceNotFoundException("Room not found or you are not the host"));

        Optional<TvDisplay> optionalDisplay = tvDisplayRepository.findByPairingCodeAndStatus(
                pairingCode.toUpperCase(), TvDisplayStatus.WAITING);

        if (optionalDisplay.isEmpty()) {
            throw new ResourceNotFoundException("Invalid or expired pairing code");
        }

        TvDisplay tvDisplay = optionalDisplay.get();
        if (LocalDateTime.now().isAfter(tvDisplay.getExpiresAt())) {
            tvDisplay.setStatus(TvDisplayStatus.EXPIRED);
            tvDisplayRepository.save(tvDisplay);
            throw new IllegalStateException("Pairing code expired");
        }

        tvDisplay.setStatus(TvDisplayStatus.CLAIMED);
        tvDisplay.setRoomCode(roomCode);
        tvDisplay.setClaimedByUserId(hostUserId);
        tvDisplay.setClaimedAt(LocalDateTime.now());
        tvDisplayRepository.save(tvDisplay);

        log.info("📺 TV display claimed: displayId={}, roomCode={}, claimedBy={}", 
                tvDisplay.getId(), roomCode, hostUserId);

        return TvDisplayClaimDTO.builder()
                .success(true)
                .roomCode(roomCode)
                .displayId(tvDisplay.getId())
                .build();
    }

    @Scheduled(fixedRate = 60000) // Every minute
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now();
        // Expire waiting displays
        List<TvDisplay> expired = tvDisplayRepository.findByStatusAndExpiresAtBefore(TvDisplayStatus.WAITING, now);
        if (!expired.isEmpty()) {
            expired.forEach(d -> d.setStatus(TvDisplayStatus.EXPIRED));
            tvDisplayRepository.saveAll(expired);
            log.info("📺 Expired {} TV displays", expired.size());
        }
    }

    private String generatePairingCode() {
        StringBuilder sb = new StringBuilder(PAIRING_CODE_LENGTH);
        for (int i = 0; i < PAIRING_CODE_LENGTH; i++) {
            sb.append(PAIRING_CODE_CHARSET.charAt(random.nextInt(PAIRING_CODE_CHARSET.length())));
        }
        String code = sb.toString();
        // Ensure uniqueness (simple retry)
        if (tvDisplayRepository.findByPairingCodeAndStatus(code, TvDisplayStatus.WAITING).isPresent()) {
            return generatePairingCode();
        }
        return code;
    }
}
