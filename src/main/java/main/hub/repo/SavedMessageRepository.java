package main.hub.repo;

import java.util.List;
import java.util.Optional;
import main.hub.entity.SavedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedMessageRepository extends JpaRepository<SavedMessage, Long> {

    Optional<SavedMessage> findByNameAndUserId(String name, Long userId);

    List<SavedMessage> findByUserId(Long userId);

    boolean existsByNameAndUserId(String name, Long userId);
}
