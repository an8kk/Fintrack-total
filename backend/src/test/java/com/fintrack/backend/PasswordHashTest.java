package com.fintrack.backend;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.nio.file.Files;
import java.nio.file.Path;

public class PasswordHashTest {
    @Test
    public void generateHash() throws Exception {
        String hash = new BCryptPasswordEncoder().encode("123");
        Files.writeString(Path.of("hash_output_clean.txt"), hash);
    }
}
