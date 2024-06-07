package runner.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import runner.dao.UserRepository;
import runner.dao.UserRolesRepository;
import runner.entity.User;
import runner.entity.UserAuthority;
import runner.entity.UserRole;
import runner.exception.UsernameAlreadyExistsException;
import runner.service.UserService;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRolesRepository userRolesRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Override
    public void registration(String username, String password) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User user = User.builder()
                    .id(null)
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .enabled(true)
                    .expired(false)
                    .locked(false)
                    .build();
            userRolesRepository.save(new UserRole(null, user, UserAuthority.USER));
        }
        else {
            throw new UsernameAlreadyExistsException();
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));
    }

    public String deleteUser(String username, String loadName) {
        Optional<User> user = userRepository.findByUsername(loadName);
        Optional<UserRole> userRole = Optional.ofNullable(userRolesRepository.findByUserId(user.get().getId()));
        if(user.isPresent() && userRole.isPresent()) {
            if(userRole.get().getUserAuthority().equals(UserAuthority.ADMIN) || user.get().getUsername().equals(username)) {
                userRepository.deleteById(userRepository.findByUsername(username).get().getId());
                return "Пользователь успешно удалён";
            } else return "Убедитесь, что Вы  вводите верный username";
        } else return "Убедитесь, что Вы вошли в аккаунт";
    }

    public String updateUser(String username, String password, boolean enabled, boolean expired, boolean locked, String loadName) {
        Optional<User> user = userRepository.findByUsername(loadName);
        Optional<UserRole> userRole = Optional.ofNullable(userRolesRepository.findByUserId(user.get().getId()));
        if(user.get().getUsername().equals(username) || userRole.get().getUserAuthority().equals(UserAuthority.ADMIN)) {
            user.get().setUsername(username);
            user.get().setPassword(passwordEncoder.encode(password));
            user.get().setEnabled(enabled);
            user.get().setExpired(expired);
            user.get().setLocked(locked);
            return "Пользователь успешно изменён";
        } else return "Убедитесь, что Вы  вводите верный username или обладаете правами на изменение других пользователей";
    }
}
