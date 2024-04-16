package runner.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import runner.dao.AppUserDAO;
import runner.dao.JokeDAO;
import runner.entity.AppUser;
import runner.entity.Joke;
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
    private final ProducerService producerService;
    private final JokeDAO jokeDAO;
    private final AppUserDAO appUserDAO;

    public MainServiceImpl(ProducerService producerService, JokeDAO jokeDAO, AppUserDAO appUserDAO) {
        this.producerService = producerService;
        this.jokeDAO = jokeDAO;
        this.appUserDAO = appUserDAO;
    }

    @Override
    public void proccessTextMessage(Update update){
        var text = update.getMessage().getText();
        var output = "Что-то пошло не так...";
        output = processServiceCommand(update, text);

        var chatId = update.getMessage().getChatId();
        sendAnswer(output, chatId);
    }

    private void sendAnswer(String output, Long chatId) {
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
            return postjoke(update);
        } else if (text.contains("/put")) {
            return putjoke(update);
        } else if (GETALL.equals(cmd)) {
            return getall(update);
        } else if (text.contains("/get")) {
            return getjoke(update);
        } else if (text.contains("/delete")) {
            return deletejoke(update);
        }else if (text.contains("/popular")) {
            return getMostPopular(update);
        }else if (text.contains("/random")) {
            return getRandom();
        }else if (text.contains("/allRequests")) {
            return allRequests(update);
        } else {
            return "Неизвестная команда! Чтобы посмотреть список доступных команд, введите /help";
        }
    }

    private String deletejoke(Update update) {
        try{
        String idjoke = update.getMessage().getText().substring(update.getMessage().getText().indexOf(" ") + 1);
        Optional <Joke> joke = jokeDAO.findById(Long.parseLong(idjoke));
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

    private String getjoke(Update update) {
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

    private String getall(Update update) {
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

    private String putjoke(Update update) {
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

    private String postjoke(Update update) {
        var text = update.getMessage().getText().substring(update.getMessage().getText().indexOf(" ") + 1);
        if (text.contains("/post")) return "Вы не ввели шутку!";
        return "Ваша шутка: " + text + "\nId шутки: " + saveJoke(text).toString() + "\nДата создания " + LocalDate.now() + " " + LocalTime.now().truncatedTo(ChronoUnit.SECONDS)
                .format(DateTimeFormatter.ISO_LOCAL_TIME);
    }

    private String getMostPopular(Update update){
        var chatId = update.getMessage().getChatId();
        List<Joke> jokes= jokeDAO.findAll();
        if (jokes.isEmpty()) {
            return "Нет доступных шуток";
        }

        // Сортируем шутки по рейтингу в порядке убывания и берём первые 5 шуток
        List<Joke> top5Jokes = jokes.stream()
                .sorted(Comparator.comparing(Joke::getRating).reversed())
                .limit(5)
                .toList();

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

    private String getRandom() {
        List<Joke> jokes = jokeDAO.findAll();
        if (jokes.isEmpty()) {
            return "Нет доступных шуток";
        }
        try{
        Random random = new Random();
        Joke randomJoke = jokes.get(random.nextInt(jokes.size()));
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
        }}
        catch (Exception e){
            return "Произошла ошибка! Скорее всего была удалена одна или несколько шуток, из-за чего случайно выбранное удаленное id не было найдено...";
        }
    }

    private String allRequests(Update update){
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
    private String help() {
        return "Список доступных команд:\n"
                + "/post <шутка> - добавить шутку\n"
                + "/get <id шутки> - вывести шутку по id\n"
                + "/getall - вывести все шутки\n"
                + "/put <id шутки> <Изменённая шутка>- изменить шутку\n"
                + "/delete <id шутки> - удалить шутку\n"
                + "/popular - вывести топ 5 популярных шуток\n"
                + "/random - вывести случайную шутку\n"
                + "/allRequests - вывести все совершенные запросы";
    }

    private Long saveJoke(String text){
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
