package com.chenhaonee.agents.domain.auth.model;

import com.chenhaonee.agents.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "api_token")
@EqualsAndHashCode(callSuper = true)
public class ApiToken extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String tokenHash;

    public ApiToken(String tokenHash) {
        this.tokenHash = tokenHash;
    }
}
