package runner.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import runner.entity.Joke;

import java.util.List;
import java.util.Optional;

public interface JokeDAO extends PagingAndSortingRepository<Joke, Long> {
    Optional<Joke> findById(Long id);

    void delete(Joke joke);

    void save(Joke joke1);

    List<Joke> findAll();
};
