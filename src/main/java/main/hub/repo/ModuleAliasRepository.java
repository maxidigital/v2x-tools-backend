package main.hub.repo;

import java.util.List;
import java.util.Optional;
import main.hub.entity.ModuleAlias;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleAliasRepository extends JpaRepository<ModuleAlias, Long> {

    Optional<ModuleAlias> findByAliasAndUserId(String alias, Long userId);

    List<ModuleAlias> findByUserId(Long userId);

    List<ModuleAlias> findByModuleId(String moduleId);

    boolean existsByAliasAndUserId(String alias, Long userId);
}
