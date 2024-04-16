package runner.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import runner.entity.Joke;

import java.util.Optional;

public interface JokeDAO extends PagingAndSortingRepository<Joke, Long> {
    Optional<Joke> findById(Long id);
};
