package com.example.scheduler.config;

import com.example.scheduler.domain.DefaultGame;
import com.example.scheduler.repository.DefaultGameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
@RequiredArgsConstructor
public class DefaultGameInitializer implements ApplicationRunner {

    private final DefaultGameRepository defaultGameRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Resource resource = new ClassPathResource("default_games.txt");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String name = line.trim();
                if (name.isEmpty()) continue;
                // 존재하지 않으면 삽입
                defaultGameRepository.findByName(name)
                        .orElseGet(() -> defaultGameRepository.save(
                                DefaultGame.builder()
                                        .name(name)
                                        .build()
                        ));
            }
        }
    }
}
