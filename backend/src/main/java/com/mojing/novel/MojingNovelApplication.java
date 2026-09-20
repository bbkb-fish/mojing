package com.mojing.novel;

import com.mojing.novel.config.AiProperties;
import com.mojing.novel.config.EmbeddingProperties;
import com.mojing.novel.config.QdrantProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AiProperties.class, EmbeddingProperties.class, QdrantProperties.class})
public class MojingNovelApplication {

    public static void main(String[] args) {
        SpringApplication.run(MojingNovelApplication.class, args);
    }
}
