package runner.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import runner.dao.AppUserDAO;
import runner.dao.JokeDAO;
import runner.dao.UserRepository;
import runner.entity.*;
import runner.service.MainService;
import runner.service.ProducerService;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static runner.service.enums.ServiceCommands.*;

@Service
@Log4j
public class MainServiceImpl implements MainService{
    private final UserRepository userRepository;
    private final ProducerService producerService;
    private final JokeDAO jokeDAO;
    private final AppUserDAO appUserDAO;
    private UserServiceImpl userService;
    private PasswordEncoder encoder;
    @Autowired
    private EntityManager entityManager;

    public MainServiceImpl(ProducerService producerService, JokeDAO jokeDAO, AppUserDAO appUserDAO,
                           UserRepository userRepository) {
        this.producerService = producerService;
        this.jokeDAO = jokeDAO;
        this.appUserDAO = appUserDAO;
        this.userRepository = userRepository;
    }

    @Override
    public void proccessTextMessage(Update update){
        var text = update.getMessage().getText();
        var output = "Что-то пошло не так...";
        output = processServiceCommand(update, text);

        var chatId = update.getMessage().getChatId();
        sendAnswer(output, chatId);
    }

    public void sendAnswer(String output, Long chatId) {
        var sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(output);
        producerService.produceAnswer(sendMessage);
    }

    private String processServiceCommand(Update update, String cmd) {
        String text = update.getMessage().getText();

        if (HELP.equals(cmd)){
            return help();
        } else if (START.equals(cmd)) {
            return "Доброго времени суток! Чтобы посмотреть список команд, введите /help";
        } else if (text.contains("/post")) {
            return postJoke(update);
        } else if (text.contains("/put")) {
            return putJoke(update);
        } else if (GETALL.equals(cmd)) {
            return getAll(update);
        } else if (text.contains("/get")) {
            return getJoke(update);
        } else if (text.contains("/delete")) {
            return deleteJoke(update);
        }else if (text.contains("/popular")) {
            return getMostPopular(update);
        }else if (text.contains("/random")) {
            return getRandom();
        }else if (text.contains("/allRequests")) {
            return allRequests(update);
        } else if (text.contains("/registration")) {
            return registration(update);
        }else if (text.contains("/login")) {
            return login(update);
        }else if (text.contains("/updateuser")) {
            return updateUser(update);
        }else if (text.contains("/deleteuser")) {
            return deleteUser(update);
        }else {
            return "Неизвестная команда! Чтобы посмотреть список доступных команд, введите /help";
        }
    }
    public String registration(Update update){
        var text = update.getMessage().getText().substring(update.getMessage().getText().indexOf(" ") + 1);
        if (text.contains("/registration")) return "Вы не ввели username и password!";
        String[] parts = text.split(" ");
        var username = parts[0];
        var password = parts[1];
        userService.registration(username, password);
        return "Регистрация успешна";
    }
    public String login(Update update){
        var text = update.getMessage().getText().substring(update.getMessage().getText().indexOf(" ") + 1);
        if (text.contains("/login")) return "Вы не ввели username и password!";
        String[] parts = text.split(" ");
        var username = parts[0];
        var password = parts[1];
        try{userService.loadUserByUsername(username);
            Optional<User> user = userRepository.findByUsername(username);
            if (user.get().getPassword().equals(password)) return "Вход успешен";
            else return "Неверный пароль";}
        catch (UsernameNotFoundException e){
            return "Пользователь с таким username не найден";
        }
    }
    public String deleteUser(Update update){
        var username = update.getMessage().getText().substring(update.getMessage().getText().indexOf(" ") + 1);
        if (username.contains("/delete")) return "Вы не ввели username!";
        Optional<User> user = userRepository.findByUsername(username);
        if(user.isPresent()) {
            userRepository.deleteById(userRepository.findByUsername(username).get().getId());
            return "Пользователь успешно удалён";
        } else return "Убедитесь, что Вы  вводите верный username";
    }
    public String updateUser(Update update){
        try{
        var text = update.getMessage().getText().substring(update.getMessage().getText().indexOf(" ") + 1);
        if (text.contains("/delete")) return "Вы не ввели username!";
        String[] parts = text.split(" ");
        var username = parts[0];
        var password = parts[1];
        boolean enabled = Boolean.parseBoolean(parts[2]);
        boolean expired = Boolean.parseBoolean(parts[3]);
        boolean locked = Boolean.parseBoolean(parts[4]);
        Optional<User> user = userRepository.findByUsername(username);
        user.get().setUsername(username);
        user.get().setPassword(encoder.encode(password));
        user.get().setEnabled(enabled);
        user.get().setExpired(expired);
        user.get().setLocked(locked);
        return "Пользователь успешно изменён";}
        catch (Exception e){
            return "Убедитесь , что пытаетесь изменить существующего пользователя и вводите новые данные в правильном парядке";
        }
    }
    public String start(){
        return "Доброго времени суток! Чтобы посмотреть список команд, введите /help";
    }
    public String deleteJoke(Update update) {
        try{
        String idjoke = update.getMessage().getText().substring(update.getMessage().getText().indexOf(" ") + 1);
        Optional <Joke> joke = jokeDAO.findById(Long.parseLong(idjoke));
            if(joke.isPresent()) {
                jokeDAO.delete(joke.get());
                var output = "Шутка успешно удалена";
                var chatId = update.getMessage().getChatId();
                sendAnswer(output, chatId);
                return output;
            } else {
                return "Шутка с указанным id не найдена";
            }
        }
        catch (NumberFormatException e){
            return "Произошла ошибка!\nУбедитесь, что написали в команде лишь 1 пробел";
        }
    }

    public String deleteJoke(Long id) {
        try{
            Optional <Joke> joke = jokeDAO.findById(id);
            if(joke.isPresent()) {
                jokeDAO.delete(joke.get());
                return "Шутка успешно удалена";
            } else {
                return "Шутка с указанным id не найдена";
            }
        }
        catch (NumberFormatException e){
            return "Произошла ошибка!\nУбедитесь, что написали в команде лишь 1 пробел";
        }
    }

    public String getJoke(Update update) {
        try{
        String id = update.getMessage().getText().substring(update.getMessage().getText().indexOf(" ") + 1);
        Optional <Joke> joke = jokeDAO.findById(Long.parseLong(id));
        saveAppUser(update, Long.parseLong(id));
        var changeDate = joke.get().getChangeDate();
        var rating = joke.get().getRating() + 1;
        Optional<Joke> jokeOptional = jokeDAO.findById(Long.valueOf(id));
        if(jokeOptional.isPresent()) {
                Joke joke1 = jokeOptional.get();
                joke1.setRating(rating);
                jokeDAO.save(joke1);}
        if (changeDate == null) return joke.get().getText() + "\nДата создания " +  joke.get().getCreationDate() + "\nДата изменения ---" + "\nКоличество вызовов: " + rating;
        else return joke.get().getText() + "\nДата создания " +  joke.get().getCreationDate() + "\nДата изменения " + joke.get().getChangeDate() + "\nКоличество вызовов: " + rating;}
        catch (NumberFormatException e){
            return "Произошла ошибка!\nУбедитесь, что написали в команде лишь 1 пробел и что введённый id существует";
        }
    }
    public ResponseEntity<Optional<Joke>> getJoke(Long id) {
        try{
            Optional <Joke> joke = jokeDAO.findById(id);
            var changeDate = joke.get().getChangeDate();
            var rating = joke.get().getRating() + 1;
            Optional<Joke> jokeOptional = jokeDAO.findById(id);
            if(jokeOptional.isPresent()) {
                return ResponseEntity.ok(joke);
            } else return (ResponseEntity<Optional<Joke>>) ResponseEntity.notFound();
            }
        catch (NumberFormatException e){
            return (ResponseEntity<Optional<Joke>>) ResponseEntity.badRequest();
        }
    }

    public String getAll(Update update) {
        var chatId = update.getMessage().getChatId();
        List<Joke> jokes = jokeDAO.findAll();
        if (jokes.isEmpty()) {
            return "Нет доступных шуток";
        }
        for (Joke joke : jokes){
            var changeDate = joke.getChangeDate();
            var rating = joke.getRating();
            if (rating == null) rating = 0;
            if (changeDate == null) sendAnswer("Id: " + joke.getId().toString() + "\n" + joke.getText() + "\nДата создания " + joke.getCreationDate() + "\nДата изменения ---" + "\nКоличество вызовов: " + rating, chatId);
            else sendAnswer("Id: " + joke.getId().toString() + "\n" + joke.getText() + "\nДата создания " + joke.getCreationDate() + "\nДата изменения " + joke.getChangeDate() + "\nКоличество вызовов: " + rating, chatId);
        }
        return "\nВыведены все шутки";
    }

    public ResponseEntity<List<Joke>> getAll() {
        List<Joke> jokes = jokeDAO.findAll();
        if (jokes.isEmpty()) {
            return (ResponseEntity<List<Joke>>) ResponseEntity.notFound();
        }
        return ResponseEntity.ok(jokes);
    }
    public String putJoke(Update update) {
        try {
            String[] data = update.getMessage().getText().split(" ");
            if(data.length < 3) {
                return "Произошла ошибка!\nУбедитесь, что в команде есть id и новый текст шутки, разделенные пробелом";
            }
            Long id = Long.parseLong(data[1]);
            String newText = update.getMessage().getText().substring(update.getMessage().getText().indexOf(" ", update.getMessage().getText().indexOf(" ") + 1) + 1);

            Optional<Joke> jokeOptional = jokeDAO.findById(id);
            if(jokeOptional.isPresent()) {
                Joke joke = jokeOptional.get();
                joke.setText(newText);
                joke.setChangeDate(LocalDate.now()+ " " + LocalTime.now().truncatedTo(ChronoUnit.SECONDS)
                        .format(DateTimeFormatter.ISO_LOCAL_TIME));
                jokeDAO.save(joke);

                return "Шутка с id " + id + " успешно изменена";
            } else {
                return "Шутка с id " + id + " не найдена";
            }
        } catch (NumberFormatException e) {
            return "Произошла ошибка!\nУбедитесь, что написали в команде id числом";
        }
    }

    public String putJoke(Long id, String newText) {
            Optional<Joke> jokeOptional = jokeDAO.findById(id);
            if(jokeOptional.isPresent()) {
                Joke joke = jokeOptional.get();
                joke.setText(newText);
                joke.setChangeDate(LocalDate.now()+ " " + LocalTime.now().truncatedTo(ChronoUnit.SECONDS)
                        .format(DateTimeFormatter.ISO_LOCAL_TIME));
                jokeDAO.save(joke);

                return "Шутка с id " + id + " успешно изменена";
            } else {
                return "Шутка с id " + id + " не найдена";
            }
    }

    public String postJoke(Update update) {
        var text = update.getMessage().getText().substring(update.getMessage().getText().indexOf(" ") + 1);
        if (text.contains("/post")) return "Вы не ввели шутку!";
        return "Ваша шутка: " + text + "\nId шутки: " + saveJoke(text).toString() + "\nДата создания " + LocalDate.now() + " " + LocalTime.now().truncatedTo(ChronoUnit.SECONDS)
                .format(DateTimeFormatter.ISO_LOCAL_TIME);
    }

    public String getMostPopular(Update update){
        var chatId = update.getMessage().getChatId();
        List<Joke> jokes= jokeDAO.findAll();
        if (jokes.isEmpty()) {
            return "Нет доступных шуток";
        }
        String query = "SELECT * FROM jokes ORDER BY rating DESC LIMIT 5";
        List<Joke> top5Jokes = jokeDAO.findByCustomQuery(query);
        if (top5Jokes.isEmpty()) {
            return "Нет доступных шуток";
        }

//        List<Joke> top5Jokes = jokes.stream()
//                .sorted(Comparator.comparing(Joke::getRating).reversed())
//                .limit(5)
//                .toList();

        StringBuilder response = new StringBuilder("Топ 5 самых популярных шуток:\n");
        for (Joke joke : top5Jokes) {
            var changeDate = joke.getChangeDate();
            var rating = joke.getRating();
            if (rating == null) rating = 0;
            if (changeDate == null) response.append("Id: ")
                    .append(joke.getId())
                    .append("\n")
                    .append(joke.getText())
                    .append("\nДата создания ")
                    .append(joke.getCreationDate())
                    .append("\nДата изменения ---")
                    .append("\nКоличество вызовов: ")
                    .append(rating)
                    .append("\n\n");
            else response.append("Id: ")
                    .append(joke.getId())
                    .append("\n")
                    .append(joke.getText())
                    .append("\nДата создания ")
                    .append(joke.getCreationDate())
                    .append("\nДата изменения ")
                    .append(joke.getChangeDate())
                    .append("\nКоличество вызовов: ")
                    .append(rating)
                    .append("\n\n");
        }
        // Отправка ответа в чат
        sendAnswer(response.toString(), chatId);

        return "\nТоп 5 самых популярных шуток выведены";
    }
    public ResponseEntity<List<Joke>> getMostPopular(){
        List<Joke> jokes= jokeDAO.findAll();
        if (jokes.isEmpty()) {
            return ResponseEntity.ok(jokes);
        }

        String query = "SELECT * FROM jokes ORDER BY rating DESC LIMIT 5";
        List<Joke> top5Jokes = jokeDAO.findByCustomQuery(query);

//        List<Joke> top5Jokes = jokes.stream()
//                .sorted(Comparator.comparing(Joke::getRating).reversed())
//                .limit(5)
//                .toList();

        return ResponseEntity.ofNullable(top5Jokes);
    }
    private void saveAppUser(Update update, Long id) {
            var telegramUser = update.getMessage().getFrom();
            AppUser appUser= AppUser.builder()
                    .telegramUserId(telegramUser.getId())
                    .userName(telegramUser.getUserName())
                    .jokeId(id)
                    .requestDate(LocalDate.now() + " " + LocalTime.now().truncatedTo(ChronoUnit.SECONDS)
                            .format(DateTimeFormatter.ISO_LOCAL_TIME))
                    .build();
            appUserDAO.save(appUser);
    }

    public String getRandom() {
        List<Joke> jokes = jokeDAO.findAll();
        if (jokes.isEmpty()) {
            return "Нет доступных шуток";
        }
            Query query = entityManager.createNativeQuery("SELECT * FROM Joke ORDER BY RAND() LIMIT 1", Joke.class);
            Joke randomJoke = (Joke) query.getSingleResult();
            var changeDate = randomJoke.getChangeDate();
            var rating = randomJoke.getRating() + 1;
            Optional<Joke> jokeOptional = jokeDAO.findById(randomJoke.getId());
            if(jokeOptional.isPresent()) {
                Joke joke = jokeOptional.get();
                joke.setRating(rating);
                jokeDAO.save(joke);}
        if (changeDate == null) {
            return randomJoke.getText() + "\nДата создания " + randomJoke.getCreationDate() + "\nДата изменения ---" + "\nКоличество вызовов: " + rating;
        } else {
            return randomJoke.getText() + "\nДата создания " + randomJoke.getCreationDate() + "\nДата изменения " + randomJoke.getChangeDate() + "\nКоличество вызовов: " + rating;
        }
    }

    public ResponseEntity<Joke> getRandomJoke() {
        List<Joke> jokes = jokeDAO.findAll();
        if (jokes.isEmpty()) {
            return (ResponseEntity<Joke>) ResponseEntity.notFound();
        }
            Query query = entityManager.createNativeQuery("SELECT * FROM Joke ORDER BY RAND() LIMIT 1", Joke.class);
            Joke randomJoke = (Joke) query.getSingleResult();
            var changeDate = randomJoke.getChangeDate();
            var rating = randomJoke.getRating() + 1;
            Optional<Joke> jokeOptional = jokeDAO.findById(randomJoke.getId());
            if(jokeOptional.isPresent()) {
                Joke joke = jokeOptional.get();
                joke.setRating(rating);
                jokeDAO.save(joke);}
                return ResponseEntity.ok(randomJoke);

    }

    public String allRequests(Update update){
        var chatId = update.getMessage().getChatId();
        List<AppUser> appUsers = appUserDAO.findAll();
        if (appUsers.isEmpty()) {
            return "Пока нет совершенных вызовов";
        }
        for (AppUser appUser : appUsers){
            sendAnswer("Id: " + appUser.getId().toString() + "\n" + "Имя пользователя: " + appUser.getUserName() + "\nid вызванной шутки: " + appUser.getJokeId() + "\nДата и время вызова шутки: " + appUser.getRequestDate() , chatId);
        }
        return "\nВыведены все вызовы";
    }

    public ResponseEntity<List<AppUser>> allRequests(){
        List<AppUser> appUsers = appUserDAO.findAll();
        for (AppUser appUser : appUsers){
            appUsers.add(appUser);
        }
        return ResponseEntity.ok(appUsers);
    }
    public String help() {
        return "Список доступных команд:\n"
                + "/post <шутка> - добавить шутку\n"
                + "/get <id шутки> - вывести шутку по id\n"
                + "/getall - вывести все шутки\n"
                + "/put <id шутки> <Изменённая шутка>- изменить шутку\n"
                + "/delete <id шутки> - удалить шутку\n"
                + "/popular - вывести топ 5 популярных шуток\n"
                + "/random - вывести случайную шутку\n"
                + "/allRequests - вывести все совершённые запросы\n"
                + "/registration - регистрация пользователя";
    }

    public Long saveJoke(String text){
        Joke joke = Joke.builder()
                .text(text)
                .creationDate(LocalDate.now() + " " + LocalTime.now().truncatedTo(ChronoUnit.SECONDS)
                        .format(DateTimeFormatter.ISO_LOCAL_TIME))
                .changeDate(null)
                .Rating(0)
                .build();
        jokeDAO.save(joke);
        return joke.getId();
    }
}
