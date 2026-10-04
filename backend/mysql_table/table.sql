create schema bbkb_novel;

CREATE TABLE app_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME,
    token_version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_app_user_username UNIQUE (username)
);
CREATE TABLE novel (
   id BIGINT PRIMARY KEY AUTO_INCREMENT,
   owner_user_id BIGINT NOT NULL,
   title VARCHAR(200) NOT NULL,
   description VARCHAR(1000),
   outline LONGTEXT,
   cover_url VARCHAR(500),
   status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
   total_words INT NOT NULL DEFAULT 0,
   version BIGINT NOT NULL DEFAULT 0,
   created_at DATETIME NOT NULL,
   updated_at DATETIME NOT NULL,
   INDEX idx_novel_owner_updated (owner_user_id, updated_at),
   CONSTRAINT fk_novel_owner FOREIGN KEY (owner_user_id) REFERENCES app_user(id)
 );
CREATE TABLE novel_project_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    novel_id BIGINT NOT NULL,
    inspiration TEXT,
    genre VARCHAR(100),
    channel VARCHAR(100),
    target_audience VARCHAR(500),
    platform VARCHAR(100),
    core_selling_point TEXT,
    protagonist_hook TEXT,
    growth_route TEXT,
    reader_expectations TEXT,
    opening_three_chapters TEXT,
    expected_words INT,
    expected_volumes INT,
    chapter_word_target INT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_novel_project_profile_novel FOREIGN KEY (novel_id) REFERENCES novel(id) ON DELETE CASCADE,
    CONSTRAINT uk_novel_project_profile_novel UNIQUE (novel_id)
);
CREATE TABLE novel_project_chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    novel_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    memory_json LONGTEXT,
    memory_updated_through_message_id BIGINT,
    CONSTRAINT fk_novel_project_chat_session_novel FOREIGN KEY (novel_id) REFERENCES novel(id) ON DELETE CASCADE,
    CONSTRAINT uk_novel_project_chat_session_novel UNIQUE (novel_id)
);
CREATE TABLE novel_project_chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content LONGTEXT NOT NULL,
    suggestion_json LONGTEXT,
    source_novel_version BIGINT,
    source_profile_version BIGINT,
    analyzed_through_chapter_no INT,
    source_content_version_sum BIGINT,
    applied BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_novel_project_chat_message_session FOREIGN KEY (session_id) REFERENCES novel_project_chat_session(id) ON DELETE CASCADE,
    INDEX idx_novel_project_chat_message_session (session_id, id)
);
CREATE TABLE chapter (
     id BIGINT PRIMARY KEY AUTO_INCREMENT,
     novel_id BIGINT NOT NULL,
     chapter_no INT NOT NULL,
     title VARCHAR(200) NOT NULL,
     content LONGTEXT NOT NULL,
     word_count INT NOT NULL DEFAULT 0,
     status VARCHAR(30) NOT NULL DEFAULT 'UNPLANNED',
     content_version BIGINT NOT NULL DEFAULT 0,
     completed_at DATETIME,
     version BIGINT NOT NULL DEFAULT 0,
     created_at DATETIME NOT NULL,
     updated_at DATETIME NOT NULL,
     CONSTRAINT fk_chapter_novel
         FOREIGN KEY (novel_id) REFERENCES novel(id),
     CONSTRAINT uk_novel_chapter_no
         UNIQUE (novel_id, chapter_no)
);

CREATE TABLE story_character (
                                 id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                 novel_id BIGINT NOT NULL,
                                 name VARCHAR(100) NOT NULL,
                                 aliases VARCHAR(500),
                                 role VARCHAR(100),
                                 affiliations VARCHAR(500),
                                 description TEXT,
                                 personality TEXT,
                                 goal TEXT,
                                 current_state TEXT,
                                 relationships TEXT,
                                 status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                                 merged_into_character_id BIGINT,
                                 version BIGINT NOT NULL DEFAULT 0,
                                 created_at DATETIME NOT NULL,
                                 updated_at DATETIME NOT NULL,
                                 CONSTRAINT fk_story_character_novel FOREIGN KEY (novel_id) REFERENCES novel(id) ON DELETE CASCADE,
                                 CONSTRAINT fk_story_character_merged_into FOREIGN KEY (merged_into_character_id) REFERENCES story_character(id) ON DELETE SET NULL,
                                 INDEX idx_story_character_novel (novel_id)
);

CREATE TABLE story_organization (
                                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                    novel_id BIGINT NOT NULL,
                                    name VARCHAR(100) NOT NULL,
                                    aliases VARCHAR(500),
                                    organization_type VARCHAR(100),
                                    description TEXT,
                                    goal TEXT,
                                    organization_structure TEXT,
                                    relationships TEXT,
                                    version BIGINT NOT NULL DEFAULT 0,
                                    created_at DATETIME NOT NULL,
                                    updated_at DATETIME NOT NULL,
                                    CONSTRAINT fk_story_organization_novel FOREIGN KEY (novel_id) REFERENCES novel(id) ON DELETE CASCADE,
                                    INDEX idx_story_organization_novel (novel_id)
);

CREATE TABLE world_setting (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               novel_id BIGINT NOT NULL,
                               category VARCHAR(50) NOT NULL,
                               title VARCHAR(200) NOT NULL,
                               content TEXT NOT NULL,
                               version BIGINT NOT NULL DEFAULT 0,
                               created_at DATETIME NOT NULL,
                               updated_at DATETIME NOT NULL,
                               CONSTRAINT fk_world_setting_novel FOREIGN KEY (novel_id) REFERENCES novel(id) ON DELETE CASCADE,
                               INDEX idx_world_setting_novel (novel_id)
);

CREATE TABLE story_volume (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    novel_id BIGINT NOT NULL,
    volume_no INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    chapter_start INT NOT NULL,
    chapter_end INT,
    objective TEXT,
    retrospective LONGTEXT,
    chapter_facts_json LONGTEXT,
    future_plan LONGTEXT,
    key_turning_points TEXT,
    climax TEXT,
    ending_hook TEXT,
    foreshadows TEXT,
    locked_beats TEXT,
    analyzed_through_chapter_no INT,
    source_content_version_sum BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'GENERATED',
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_story_volume_novel FOREIGN KEY (novel_id) REFERENCES novel(id) ON DELETE CASCADE,
    CONSTRAINT uk_story_volume_no UNIQUE (novel_id, volume_no),
    INDEX idx_story_volume_novel_range (novel_id, chapter_start, chapter_end)
);

CREATE TABLE story_part (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    novel_id BIGINT NOT NULL,
    volume_id BIGINT NOT NULL,
    part_no INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    chapter_start INT NOT NULL,
    chapter_end INT,
    objective TEXT,
    plan LONGTEXT,
    retrospective LONGTEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'GENERATED',
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_story_part_novel FOREIGN KEY (novel_id) REFERENCES novel(id) ON DELETE CASCADE,
    CONSTRAINT fk_story_part_volume FOREIGN KEY (volume_id) REFERENCES story_volume(id) ON DELETE CASCADE,
    CONSTRAINT uk_story_part_no UNIQUE (volume_id, part_no),
    INDEX idx_story_part_volume_range (volume_id, chapter_start, chapter_end)
);

CREATE TABLE story_bible_chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    novel_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_story_bible_chat_novel FOREIGN KEY (novel_id) REFERENCES novel(id) ON DELETE CASCADE,
    CONSTRAINT uk_story_bible_chat_novel UNIQUE (novel_id)
);

CREATE TABLE story_bible_chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    suggestions_json LONGTEXT,
    applied_indexes_json TEXT,
    recalled_message_ids_json TEXT,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_story_bible_chat_message_session FOREIGN KEY (session_id) REFERENCES story_bible_chat_session(id) ON DELETE CASCADE,
    INDEX idx_story_bible_chat_message_session (session_id, id)
);

CREATE TABLE story_bible_message_reference (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    entity_type VARCHAR(30) NOT NULL,
    entity_id BIGINT NOT NULL,
    reference_type VARCHAR(20) NOT NULL,
    CONSTRAINT fk_story_bible_reference_message FOREIGN KEY (message_id) REFERENCES story_bible_chat_message(id) ON DELETE CASCADE,
    CONSTRAINT uk_story_bible_message_reference UNIQUE (message_id, entity_type, entity_id),
    INDEX idx_story_bible_reference_entity (entity_type, entity_id, message_id)
);

CREATE TABLE agent_run (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                           novel_id BIGINT NOT NULL,
                           chapter_id BIGINT NOT NULL,
                           status VARCHAR(30) NOT NULL,
                           current_step VARCHAR(50) NOT NULL,
                           guidance TEXT,
                           plan_json LONGTEXT,
                           draft LONGTEXT,
                           provider_mode VARCHAR(20),
                           operation VARCHAR(50),
                           model VARCHAR(100),
                           total_duration_ms BIGINT,
                           error_message VARCHAR(2000),
                           created_at DATETIME NOT NULL,
                           updated_at DATETIME NOT NULL,
                           CONSTRAINT fk_agent_run_novel FOREIGN KEY (novel_id) REFERENCES novel(id) ON DELETE CASCADE,
                           CONSTRAINT fk_agent_run_chapter FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE CASCADE,
                           INDEX idx_agent_run_novel_created (novel_id, created_at),
                           INDEX idx_agent_run_chapter (chapter_id)
);

CREATE TABLE agent_step (
                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            run_id BIGINT NOT NULL,
                            step_no INT NOT NULL,
                            type VARCHAR(50) NOT NULL,
                            status VARCHAR(30) NOT NULL,
                            summary VARCHAR(1000),
                            duration_ms BIGINT,
                            created_at DATETIME NOT NULL,
                            CONSTRAINT fk_agent_step_run FOREIGN KEY (run_id) REFERENCES agent_run(id) ON DELETE CASCADE,
                            CONSTRAINT uk_agent_step_no UNIQUE (run_id, step_no)
);

CREATE TABLE ai_call_log (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             run_id BIGINT NOT NULL,
                             step_type VARCHAR(50) NOT NULL,
                             model VARCHAR(100),
                             status VARCHAR(30) NOT NULL,
                             prompt_tokens INT NOT NULL DEFAULT 0,
                             completion_tokens INT NOT NULL DEFAULT 0,
                             total_tokens INT NOT NULL DEFAULT 0,
                             duration_ms BIGINT,
                             retry_count INT NOT NULL DEFAULT 0,
                             error_message VARCHAR(2000),
                             created_at DATETIME NOT NULL,
                             CONSTRAINT fk_ai_call_log_run FOREIGN KEY (run_id) REFERENCES agent_run(id) ON DELETE CASCADE,
                             INDEX idx_ai_call_log_run (run_id)
);

-- 章节导演：每个章节对应一个持久化多轮会话。
CREATE TABLE chapter_director_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    novel_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,
    current_run_id BIGINT,
    current_plan_json LONGTEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_director_session_chapter UNIQUE (chapter_id),
    CONSTRAINT fk_director_session_novel FOREIGN KEY (novel_id) REFERENCES novel(id) ON DELETE CASCADE,
    CONSTRAINT fk_director_session_chapter FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE CASCADE,
    CONSTRAINT fk_director_session_run FOREIGN KEY (current_run_id) REFERENCES agent_run(id) ON DELETE SET NULL,
    INDEX idx_director_session_novel (novel_id)
);

CREATE TABLE chapter_director_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    run_id BIGINT,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    plan_json LONGTEXT,
    change_summary_json TEXT,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_director_message_session FOREIGN KEY (session_id) REFERENCES chapter_director_session(id) ON DELETE CASCADE,
    CONSTRAINT fk_director_message_run FOREIGN KEY (run_id) REFERENCES agent_run(id) ON DELETE SET NULL,
    INDEX idx_director_message_session (session_id, id)
);

CREATE TABLE eval_case (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                           novel_id BIGINT NOT NULL,
                           chapter_id BIGINT,
                           name VARCHAR(200) NOT NULL,
                           input_context LONGTEXT NOT NULL,
                           instruction VARCHAR(2000),
                           expected_characters VARCHAR(2000),
                           required_terms VARCHAR(2000),
                           forbidden_terms VARCHAR(2000),
                           min_length INT NOT NULL,
                           max_length INT NOT NULL,
                           enabled BOOLEAN NOT NULL DEFAULT TRUE,
                           version BIGINT NOT NULL DEFAULT 0,
                           created_at DATETIME NOT NULL,
                           updated_at DATETIME NOT NULL,
                           CONSTRAINT fk_eval_case_novel FOREIGN KEY (novel_id) REFERENCES novel(id) ON DELETE CASCADE,
                           CONSTRAINT fk_eval_case_chapter FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE SET NULL,
                           INDEX idx_eval_case_novel (novel_id, updated_at)
);

CREATE TABLE eval_run (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          case_id BIGINT NOT NULL,
                          status VARCHAR(30) NOT NULL,
                          generator_model VARCHAR(100),
                          generator_prompt_version VARCHAR(50) NOT NULL,
                          judge_prompt_version VARCHAR(50) NOT NULL,
                          generated_text LONGTEXT,
                          rule_score INT,
                          judge_score INT,
                          overall_score INT,
                          passed BOOLEAN,
                          violations_json TEXT,
                          judge_feedback VARCHAR(4000),
                          prompt_tokens INT NOT NULL DEFAULT 0,
                          completion_tokens INT NOT NULL DEFAULT 0,
                          total_tokens INT NOT NULL DEFAULT 0,
                          duration_ms BIGINT,
                          error_message VARCHAR(2000),
                          created_at DATETIME NOT NULL,
                          CONSTRAINT fk_eval_run_case FOREIGN KEY (case_id) REFERENCES eval_case(id) ON DELETE CASCADE,
                          INDEX idx_eval_run_case_created (case_id, created_at)
);

CREATE TABLE chapter_revision (
                                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                  novel_id BIGINT NOT NULL,
                                  chapter_id BIGINT NOT NULL,
                                  agent_run_id BIGINT,
                                  revision_type VARCHAR(30) NOT NULL,
                                  instruction VARCHAR(2000),
                                  original_text LONGTEXT NOT NULL,
                                  revised_text LONGTEXT NOT NULL,
                                  start_offset INT NOT NULL,
                                  end_offset INT NOT NULL,
                                  status VARCHAR(30) NOT NULL,
                                  change_summary_json TEXT,
                                  warnings_json TEXT,
                                  version BIGINT NOT NULL DEFAULT 0,
                                  created_at DATETIME NOT NULL,
                                  updated_at DATETIME NOT NULL,
                                  CONSTRAINT fk_chapter_revision_novel FOREIGN KEY (novel_id) REFERENCES novel(id) ON DELETE CASCADE,
                                  CONSTRAINT fk_chapter_revision_chapter FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE CASCADE,
                                  CONSTRAINT fk_chapter_revision_agent_run FOREIGN KEY (agent_run_id) REFERENCES agent_run(id) ON DELETE SET NULL,
                                  INDEX idx_chapter_revision_chapter_created (chapter_id, created_at),
                                  INDEX idx_chapter_revision_run (agent_run_id)
);

CREATE TABLE consistency_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    novel_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,
    agent_run_id BIGINT,
    source_content_version BIGINT NOT NULL,
    score INT NOT NULL,
    summary VARCHAR(2000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    provider_mode VARCHAR(20),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_consistency_report_novel FOREIGN KEY (novel_id) REFERENCES novel(id) ON DELETE CASCADE,
    CONSTRAINT fk_consistency_report_chapter FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE CASCADE,
    CONSTRAINT fk_consistency_report_run FOREIGN KEY (agent_run_id) REFERENCES agent_run(id) ON DELETE SET NULL,
    INDEX idx_consistency_report_chapter_created (chapter_id, created_at)
);

CREATE TABLE consistency_issue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    quote_text TEXT NOT NULL,
    message VARCHAR(2000) NOT NULL,
    suggestion VARCHAR(2000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    revision_id BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_consistency_issue_report FOREIGN KEY (report_id) REFERENCES consistency_report(id) ON DELETE CASCADE,
    CONSTRAINT fk_consistency_issue_revision FOREIGN KEY (revision_id) REFERENCES chapter_revision(id) ON DELETE SET NULL,
    INDEX idx_consistency_issue_report (report_id),
    INDEX idx_consistency_issue_revision (revision_id)
);

CREATE TABLE chapter_memory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    novel_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,
    chapter_no INT NOT NULL,
    source_chapter_version BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    summary VARCHAR(2000) NOT NULL,
    time_info VARCHAR(500),
    content_json LONGTEXT NOT NULL,
    plot_progress VARCHAR(2000),
    importance VARCHAR(20) NOT NULL,
    provider_mode VARCHAR(20),
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_chapter_memory_novel FOREIGN KEY (novel_id) REFERENCES novel(id) ON DELETE CASCADE,
    CONSTRAINT fk_chapter_memory_chapter FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE CASCADE,
    INDEX idx_chapter_memory_novel_chapter_status (novel_id, chapter_no, status),
    INDEX idx_chapter_memory_chapter_created (chapter_id, created_at)
);

-- 用户私有文风档案。系统预设保存在代码中，不写入该表。
CREATE TABLE writing_style_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    novel_id BIGINT,
    name VARCHAR(100) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    description VARCHAR(2000),
    rules_text TEXT NOT NULL,
    forbidden_words TEXT,
    reference_excerpt LONGTEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_writing_style_novel FOREIGN KEY (novel_id) REFERENCES novel(id) ON DELETE CASCADE,
    INDEX idx_writing_style_owner_updated (owner_user_id, updated_at),
    INDEX idx_writing_style_novel (novel_id)
);

-- 由章节成稿记录确认后生成的人物历史。
CREATE TABLE character_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    novel_id BIGINT NOT NULL,
    character_id BIGINT NOT NULL,
    character_name VARCHAR(100) NOT NULL,
    chapter_id BIGINT NOT NULL,
    chapter_no INT NOT NULL,
    source_chapter_version BIGINT NOT NULL,
    actions TEXT,
    state_change TEXT,
    new_knowledge TEXT,
    foreshadowings_json TEXT,
    profile_changes_json LONGTEXT,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_character_history_novel FOREIGN KEY (novel_id) REFERENCES novel(id) ON DELETE CASCADE,
    CONSTRAINT fk_character_history_character FOREIGN KEY (character_id) REFERENCES story_character(id) ON DELETE CASCADE,
    CONSTRAINT fk_character_history_chapter FOREIGN KEY (chapter_id) REFERENCES chapter(id) ON DELETE CASCADE,
    INDEX idx_character_history_character_chapter (character_id, chapter_no, status),
    INDEX idx_character_history_chapter_status (chapter_id, status)
);

CREATE TABLE character_identity (
                                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                    novel_id BIGINT NOT NULL,
                                    canonical_character_id BIGINT NOT NULL,
                                    source_character_id BIGINT,
                                    identity_name VARCHAR(100) NOT NULL,
                                    identity_type VARCHAR(30) NOT NULL,
                                    first_appearance_chapter_no INT,
                                    reveal_chapter_no INT NOT NULL,
                                    description TEXT,
                                    created_at DATETIME NOT NULL,
                                    CONSTRAINT fk_character_identity_novel FOREIGN KEY (novel_id) REFERENCES novel(id) ON DELETE CASCADE,
                                    CONSTRAINT fk_character_identity_canonical FOREIGN KEY (canonical_character_id) REFERENCES story_character(id) ON DELETE CASCADE,
                                    CONSTRAINT fk_character_identity_source FOREIGN KEY (source_character_id) REFERENCES story_character(id) ON DELETE SET NULL,
                                    CONSTRAINT uk_character_identity_name UNIQUE (novel_id, identity_name),
                                    INDEX idx_character_identity_canonical (canonical_character_id, reveal_chapter_no)
);
