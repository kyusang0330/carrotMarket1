package miniproject.carrotmarket1.service;

import jakarta.servlet.http.HttpSession;
import miniproject.carrotmarket1.entity.User;
import miniproject.carrotmarket1.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Value("${file.upload-dir}") // application.properties의 값을 주입
    private String uploadDir;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    //로그인 시 패스워드 확인
    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    //로그인 시 사용자 위치 정보  update
    public void updateUserLocation(Long userId, Double latitude, Double longitude, String location) {
        userRepository.updateLocation(userId, latitude, longitude, location);
    }

    //프로필 수정시 사용
    public User getLoggedInUser(HttpSession session) {
        // 세션에서 로그인된 사용자 정보 가져오기
        return (User) session.getAttribute("loggedInUser");
    }

    //프로필 생성 및 업데이트
    public void saveOrUpdateUser(User user, MultipartFile profileImageFile) throws IOException {
        // 비밀번호 설정은 필요 시에만 수행
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            // 필요에 따라 비밀번호 암호화 추가 가능
        }

        // 사용자 조회
        User existingUser = userRepository.findByEmail(user.getEmail());

        if (existingUser == null) {
            // 신규 회원인 경우
            if (profileImageFile != null && !profileImageFile.isEmpty()) {
                String fileName = saveProfileImage(profileImageFile, user);
                user.setProfileImage(fileName);
            }
            user.setUserGroup("GENERAL"); // 기본 사용자 그룹 설정
            userRepository.insertUser(user); // 새 사용자 추가
        } else {
            // 기존 사용자 업데이트
            user.setId(existingUser.getId()); // 기존 ID 유지

            if (profileImageFile != null && !profileImageFile.isEmpty()) {
                // 새 프로필 이미지가 업로드된 경우만 저장
                String fileName = saveProfileImage(profileImageFile, user);
                user.setProfileImage(fileName);
            } else {
                // 새 이미지가 없으면 기존 이미지 유지
                user.setProfileImage(existingUser.getProfileImage());
            }

            userRepository.updateUser(user); // 기존 사용자 업데이트
        }
    }

    // 프로필 이미지 저장 메소드
    private String saveProfileImage(MultipartFile profileImageFile, User user) throws IOException {

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }


        String fileExtension = profileImageFile.getOriginalFilename()
                .substring(profileImageFile.getOriginalFilename().lastIndexOf("."));
        String fileName = user.getEmail() + fileExtension;

        Path filePath = uploadPath.resolve(fileName);

        // 동일한 이름의 파일이 있으면 삭제
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        // 파일 저장
        profileImageFile.transferTo(filePath.toFile());

        return "/profileImages/" + fileName; // 저장된 파일 경로 반환
    }

    public boolean emailExists(String email) {

        return userRepository.findByEmail(email) != null;
    }

    // USER 조회 By Id
    public User findById(Long id) {
        return userRepository.selectById(id);
    }
}
