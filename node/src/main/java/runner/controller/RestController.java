package runner.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import runner.entity.AppUser;
import runner.entity.Joke;
import runner.service.impl.MainServiceImpl;

import java.util.List;
import java.util.Optional;

@org.springframework.web.bind.annotation.RestController
@RequestMapping("/jokes")
public class RestController {

    private final MainServiceImpl mainServiceImpl;

    public RestController(MainServiceImpl mainServiceImpl) {
        this.mainServiceImpl = mainServiceImpl;
    }

    @PostMapping("/post")
    public ResponseEntity<String> postJoke(@RequestParam String text) {
        mainServiceImpl.saveJoke(text);
        String response = "Шутка сохранена";
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteJoke(@RequestParam Long id) {
        String response = mainServiceImpl.deleteJoke(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get")
    public ResponseEntity<Optional<Joke>> getJoke(@RequestParam Long id) {
        return mainServiceImpl.getJoke(id);
    }

    @GetMapping("/getall")
    public ResponseEntity<List<Joke>> getAll() {
        return mainServiceImpl.getAll();
    }

    @GetMapping("/getMostPopular")
    public ResponseEntity<List<Joke>> getMostPopular() {
        return mainServiceImpl.getMostPopular();
    }

    @PostMapping("/put")
    public ResponseEntity<String> putJoke(@RequestParam Long id,
                                          @RequestParam String text) {
        return ResponseEntity.ok(mainServiceImpl.putJoke(id, text));
    }

    @GetMapping("/getRandom")
    public ResponseEntity<Joke> getRandomJoke() {
        return mainServiceImpl.getRandomJoke();
    }

    @GetMapping("/allRequests")
    public ResponseEntity<List<AppUser>> allRequests() {
        return mainServiceImpl.allRequests();
    }

    @GetMapping("/help")
    public ResponseEntity<String> help() {
        String response = mainServiceImpl.help();
        return ResponseEntity.ok(response);
    }
    @GetMapping("/start")
    public ResponseEntity<String> start() {
        String response = mainServiceImpl.start();
        return ResponseEntity.ok(response);
    }
}
