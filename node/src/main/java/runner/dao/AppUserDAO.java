package runner.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import runner.entity.AppUser;

public interface AppUserDAO extends PagingAndSortingRepository<AppUser, Long> {

}
