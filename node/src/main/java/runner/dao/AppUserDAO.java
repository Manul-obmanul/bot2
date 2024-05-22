package runner.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import runner.entity.AppUser;

import java.util.List;

public interface AppUserDAO extends PagingAndSortingRepository<AppUser, Long> {

    void save(AppUser appUser);

    List<AppUser> findAll();
}
