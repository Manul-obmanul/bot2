package runner.dao;

import org.springframework.data.repository.CrudRepository;
import runner.entity.User;
import runner.entity.UserRole;

import java.util.Optional;

public interface UserRepository extends CrudRepository<UserRole, Long> {
    Optional<User> findByUsername(String username);
}
