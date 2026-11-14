package com.kinetiq.notification.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.notification.domain.output.ClientNames;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

// reads the names from the accounts, and no class of that feature
@Repository
@RequiredArgsConstructor
class JdbcClientNames implements ClientNames {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Map<AccountId, String> namesOf(Collection<AccountId> ids) {
        Map<AccountId, String> names = new HashMap<>();
        if (ids.isEmpty()) {
            return names;
        }
        jdbc.query(
                "select id, name from accounts where id in (:ids)",
                new MapSqlParameterSource("ids", ids.stream().map(AccountId::value).toList()),
                rows -> {
                    names.put(new AccountId(rows.getObject("id", UUID.class)), rows.getString("name"));
                });
        return names;
    }
}
