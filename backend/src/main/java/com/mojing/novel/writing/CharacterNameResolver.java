package com.mojing.novel.writing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 将正文中的简称、别名安全地归一到同一个人物档案；有歧义时拒绝猜测。 */
@Component
class CharacterNameResolver {
    private static final Logger log = LoggerFactory.getLogger(CharacterNameResolver.class);
    private final StoryCharacterRepository repository;

    CharacterNameResolver(StoryCharacterRepository repository) {
        this.repository = repository;
    }

    StoryCharacterEntity resolve(long novelId, String mention) {
        if (!hasText(mention)) return null;
        List<StoryCharacterEntity> active = repository.findByNovelIdAndStatusOrderByIdAsc(novelId, "ACTIVE");
        String key = normalize(mention);

        List<StoryCharacterEntity> exact = active.stream()
                .filter(character -> normalize(character.getName()).equals(key))
                .toList();
        if (exact.size() == 1) return exact.get(0);

        List<StoryCharacterEntity> aliases = active.stream()
                .filter(character -> explicitAliases(character).stream().map(CharacterNameResolver::normalize)
                        .anyMatch(key::equals))
                .toList();
        if (aliases.size() == 1) return aliases.get(0);
        if (aliases.size() > 1) {
            log.warn("人物别名存在歧义 novelId={} mention={} candidates={}", novelId, mention,
                    aliases.stream().map(StoryCharacterEntity::getName).toList());
            return null;
        }

        List<StoryCharacterEntity> derived = active.stream()
                .filter(character -> derivedAliases(character.getName()).stream()
                        .map(CharacterNameResolver::normalize).anyMatch(key::equals))
                .toList();
        if (derived.size() == 1) return derived.get(0);
        if (derived.size() > 1) log.warn("人物简称存在歧义 novelId={} mention={} candidates={}", novelId, mention,
                derived.stream().map(StoryCharacterEntity::getName).toList());
        return null;
    }

    List<String> routingNames(StoryCharacterEntity character) {
        Set<String> names = new LinkedHashSet<>();
        names.add(character.getName());
        names.addAll(explicitAliases(character));
        names.addAll(derivedAliases(character.getName()));
        return names.stream().filter(CharacterNameResolver::hasText).toList();
    }

    boolean matchesKnownName(long novelId, String mention) {
        if (!hasText(mention)) return false;
        String key = normalize(mention);
        return repository.findByNovelIdAndStatusOrderByIdAsc(novelId, "ACTIVE").stream()
                .anyMatch(character -> routingNames(character).stream()
                        .map(CharacterNameResolver::normalize).anyMatch(key::equals));
    }

    String promptGuide(long novelId) {
        StringBuilder guide = new StringBuilder();
        for (StoryCharacterEntity character : repository.findByNovelIdAndStatusOrderByIdAsc(novelId, "ACTIVE")) {
            List<String> aliases = routingNames(character).stream()
                    .filter(value -> !value.equals(character.getName()))
                    .filter(value -> {
                        StoryCharacterEntity resolved = resolve(novelId, value);
                        return resolved != null && resolved.getId().equals(character.getId());
                    }).toList();
            guide.append("- ").append(character.getName());
            if (!aliases.isEmpty()) guide.append("（别名/简称：").append(String.join("、", aliases)).append("）");
            guide.append('\n');
        }
        return guide.toString().stripTrailing();
    }

    private List<String> explicitAliases(StoryCharacterEntity character) {
        if (!hasText(character.getAliases())) return List.of();
        List<String> result = new ArrayList<>();
        for (String value : character.getAliases().split("[，,、\\n]")) {
            if (hasText(value)) result.add(value.trim());
        }
        return result;
    }

    private List<String> derivedAliases(String name) {
        if (!hasText(name)) return List.of();
        List<String> parts = new ArrayList<>();
        for (String value : name.split("[·•・\\s-]+")) {
            String part = value.trim();
            if (part.length() >= 2 && !part.equals(name)) parts.add(part);
        }
        return parts;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace("•", "·").replace("・", "·").replaceAll("\\s+", "");
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
}
