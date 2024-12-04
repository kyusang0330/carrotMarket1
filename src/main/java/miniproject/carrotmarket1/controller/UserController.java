package miniproject.carrotmarket1.controller;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import miniproject.carrotmarket1.entity.User;
import miniproject.carrotmarket1.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class UserController {

    private final UserService userService;

    //profile upload folder
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;

    }

    //========================로그인(위치정보 수집)===============================//
    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String location,
            HttpSession session) {
        // 회원 인증
        User user = userService.authenticate(email, password);
        if (user != null) {
            if ("N".equals(user.getLockedYn())) {
                // 위치 정보 업데이트
                if (latitude != null && longitude != null && location != null) {
                    userService.updateUserLocation(user.getId(), latitude, longitude, location);
                    // 로그인 시 변경된 주소로 세션에 전달
                    user.setLocation(location);
                    user.setLatitude(latitude);
                    user.setLongitude(longitude);
                }
                session.setAttribute("loggedInUser", user); // 세션에 사용자 정보 저장
                return "redirect:/products";
            } else if ("Y".equals(user.getLockedYn())) {
                return "redirect:/products?accountLocked=true"; // 정지된 계정 상태 전달
            }
        }
        // 로그인 실패 시
        return "redirect:/products?loginError=true";
    }


    //로그아웃
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/products";
    }


    //=================회원가입==========================//
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "user/register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user,
                               @RequestParam("profileImageFile") MultipartFile profileImageFile,
                               HttpSession session) throws IOException {

        try {
            userService.saveOrUpdateUser(user, profileImageFile);
            session.setAttribute("loggedInUser", user);
            return "redirect:/"; // 회원가입 완료 후 로그인 페이지로 리디렉션
        } catch (IOException e) {
            e.printStackTrace();
            return "redirect:/register?error=true";
        }
    }
    //이메일 중복가입 체크
    @PostMapping("/check-email")
    @ResponseBody
    public Map<String, Boolean> checkEmail(@RequestParam("email") String email) {
        boolean exists = userService.emailExists(email);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return response;
    }
    //=============== 프로필=================//
    //프로필 수정하기.
    @GetMapping("/edit-profile")
    public String editProfile(Model model, HttpSession session) {
        User user = userService.getLoggedInUser(session);
        model.addAttribute("user", user);
        return "user/edit-profile";
    }
    // 프로필 업데이트
    @PostMapping("/edit-profile")
    public String updateProfile(@ModelAttribute User user,
                                @RequestParam("currentPassword") String currentPassword,
                                @RequestParam("profileImageFile") MultipartFile profileImageFile,
                                HttpSession session) {
        try {
            // 기존 사용자의 ID, group 설정
            user.setId(userService.getLoggedInUser(session).getId());
            user.setUserGroup(userService.getLoggedInUser(session).getUserGroup());

            User loggedInUser = userService.getLoggedInUser(session);

            // 새 비밀번호가 입력되지 않은 경우 현재 비밀번호로 유지
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                user.setPassword(loggedInUser.getPassword());
            }

            // 프로필 이미지와 기타 정보 업데이트
            userService.saveOrUpdateUser(user, profileImageFile);

            // 세션에 업데이트된 사용자 정보 저장
            session.setAttribute("loggedInUser", user);
            return "redirect:/"; // 프로필 페이지로 리디렉션

        } catch (IOException e) {
            e.printStackTrace();
            return "redirect:/edit-profile?error=true";
        }
    }

    @PostMapping("/check-password")
    @ResponseBody
    public boolean checkPassword(@RequestParam("currentPassword") String currentPassword, HttpSession session) {
        User loggedInUser = userService.getLoggedInUser(session);
        return loggedInUser != null && loggedInUser.getPassword().equals(currentPassword);
    }

}
