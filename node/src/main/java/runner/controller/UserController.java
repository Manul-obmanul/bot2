package runner.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;
import runner.service.impl.UserServiceImpl;

@RestController
@RequestMapping("/users")
public class UserController {
    private UserServiceImpl userService;

    @PutMapping("/{id}")
    public String updateUser(@RequestParam("username") String username,
                             @RequestParam("password") String password,
                             @RequestParam("enabled") boolean enabled,
                             @RequestParam("expired") boolean expired,
                             @RequestParam("locked") boolean locked,
                             @AuthenticationPrincipal UserDetails userDetails){
        String loadName = userDetails.getUsername();
        return userService.updateUser(username, password, enabled, expired, locked, loadName);
    }

    @DeleteMapping("/delete/{username}")
    public String deleteUser(@PathVariable("username") String username, @AuthenticationPrincipal UserDetails userDetails){
        String loadName = userDetails.getUsername();
        return userService.deleteUser(username, loadName);
    }
}
