package com.kata.backend.community;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class InviteCodeGenerator {

    // Excludes look-alike characters (0/O, 1/I/L) so codes are easy to read aloud and type
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 8;

    private final SecureRandom random = new SecureRandom();
    private final CommunityRepository communityRepository;

    public String generateUnique() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(LENGTH);
            for (int i = 0; i < LENGTH; i++) {
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            code = sb.toString();
        } while (communityRepository.existsByInviteCode(code));
        return code;
    }

    public static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }
}
