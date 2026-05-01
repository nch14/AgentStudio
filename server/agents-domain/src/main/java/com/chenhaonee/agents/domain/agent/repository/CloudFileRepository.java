package com.chenhaonee.agents.domain.agent.repository;

import com.chenhaonee.agents.domain.agent.model.CloudFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * CloudFile 仓储（OSS 备份记录）。
 */
public interface CloudFileRepository extends JpaRepository<CloudFile, Long> {

    Optional<CloudFile> findByCode(String code);

    List<CloudFile> findByAgentCodeAndValidIsTrue(String agentCode);

    Optional<CloudFile> findByAgentCodeAndPathAndNameAndValidIsTrue(String agentCode, String path, String name);

    Page<CloudFile> findByAgentCodeAndValidIsTrueOrderByUpdateTimeDesc(String agentCode, Pageable pageable);

    Optional<CloudFile> findByAgentCodeAndPathAndName(String agentCode, String path, String name);

    List<CloudFile> findByAgentCodeAndPathAndValidIsTrue(String agentCode, String path);

    @Query("SELECT DISTINCT f.path FROM CloudFile f WHERE f.agentCode = :agentCode AND f.path LIKE CONCAT(:path, '_%') AND f.valid = true")
    List<String> findSubPaths(@Param("agentCode") String agentCode, @Param("path") String path);

    @Query("SELECT f FROM CloudFile f WHERE f.agentCode = :agentCode AND f.valid = true AND (:path IS NULL OR f.path LIKE CONCAT(:path, '%'))")
    List<CloudFile> findByAgentCodeAndPathPrefixAndValidIsTrue(@Param("agentCode") String agentCode,
                                                               @Param("path") String path);
}
