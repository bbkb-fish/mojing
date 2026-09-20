package com.mojing.novel.writing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 仅在正文或创作要求命中组织名称/别名时，装载该组织的完整档案。 */
@Service
public class OrganizationContextService {
    private static final int PROFILE_CONTEXT_LIMIT = 12_000;
    private static final Logger log = LoggerFactory.getLogger(OrganizationContextService.class);
    private final StoryOrganizationRepository organizationRepository;

    public OrganizationContextService(StoryOrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public Selection select(Long novelId, String... signals) {
        if (novelId == null) return Selection.empty();
        List<StoryOrganizationEntity> organizations = organizationRepository.findByNovelIdOrderByIdAsc(novelId);
        List<OrganizationMetadata> metadata = organizations.stream()
                .map(item -> new OrganizationMetadata(item.getId(), item.getName(), splitAliases(item.getAliases()), item.getType()))
                .toList();
        List<Long> selectedIds = selectIds(metadata, signals);
        List<StoryOrganizationEntity> selected = organizations.stream()
                .filter(item -> selectedIds.contains(item.getId()))
                .toList();

        StringBuilder context = new StringBuilder();
        for (StoryOrganizationEntity organization : selected) {
            StringBuilder profile = new StringBuilder("- ").append(organization.getName());
            if (hasText(organization.getAliases())) profile.append("（别名：").append(organization.getAliases()).append("）");
            if (hasText(organization.getType())) profile.append("；类型：").append(organization.getType());
            append(profile, "概况", organization.getDescription());
            append(profile, "目标", organization.getGoal());
            append(profile, "结构与成员", organization.getStructure());
            append(profile, "关系", organization.getRelationships());
            profile.append('\n');
            appendWithinLimit(context, profile.toString());
        }
        List<String> names = selected.stream().map(StoryOrganizationEntity::getName).toList();
        log.info("组织档案按需装载 novelId={} metadataCount={} selectedCount={} selectedNames={} contextChars={}",
                novelId, metadata.size(), names.size(), names, context.length());
        return new Selection(context.toString().stripTrailing(), names);
    }

    private List<Long> selectIds(List<OrganizationMetadata> metadata, String... signals) {
        List<String> usableSignals = signals == null ? List.of() : Arrays.stream(signals)
                .filter(OrganizationContextService::hasText)
                .toList();
        List<Long> matched = metadata.stream()
                .filter(item -> usableSignals.stream().anyMatch(signal -> matches(item, signal)))
                .map(OrganizationMetadata::id)
                .toList();
        if (!matched.isEmpty()) return matched;

        boolean genericReference = usableSignals.stream().anyMatch(this::containsOrganizationReference);
        if (genericReference && metadata.size() == 1) return List.of(metadata.getFirst().id());
        return List.of();
    }

    private boolean matches(OrganizationMetadata metadata, String signal) {
        if (hasText(metadata.name()) && signal.contains(metadata.name())) return true;
        return metadata.aliases().stream().anyMatch(signal::contains);
    }

    private List<String> splitAliases(String aliases) {
        if (!hasText(aliases)) return List.of();
        Set<String> values = new LinkedHashSet<>();
        for (String alias : aliases.split("[，,、\\n]")) {
            String normalized = alias.trim();
            if (!normalized.isEmpty()) values.add(normalized);
        }
        return List.copyOf(values);
    }

    private boolean containsOrganizationReference(String text) {
        return text.contains("组织") || text.contains("势力") || text.contains("宗门") || text.contains("门派")
                || text.contains("帮派") || text.contains("家族") || text.contains("公会") || text.contains("军团")
                || text.contains("教会") || text.contains("公司");
    }

    private void appendWithinLimit(StringBuilder target, String value) {
        if (!hasText(value) || target.length() >= PROFILE_CONTEXT_LIMIT) return;
        int remaining = PROFILE_CONTEXT_LIMIT - target.length();
        target.append(value, 0, Math.min(remaining, value.length()));
    }

    private void append(StringBuilder target, String label, String value) {
        if (hasText(value)) target.append("；").append(label).append("：").append(value);
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }

    /** 名称、别名和类型只参与本地路由，不会直接进入模型提示词。 */
    private record OrganizationMetadata(Long id, String name, List<String> aliases, String type) {}

    public record Selection(String promptContext, List<String> organizationNames) {
        static Selection empty() { return new Selection("", List.of()); }
    }
}
