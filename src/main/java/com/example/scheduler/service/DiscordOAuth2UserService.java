// service/DiscordOAuth2UserService.java
package com.example.scheduler.service;

import com.example.scheduler.domain.User;
import com.example.scheduler.repository.UserRepository;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DiscordOAuth2UserService
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepo;
    private final FriendCodeService friendCodeService;

    public DiscordOAuth2UserService(UserRepository userRepo, FriendCodeService friendCodeService) {
        this.userRepo = userRepo;
        this.friendCodeService = friendCodeService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        OAuth2User oauthUser = delegate.loadUser(req);

        String discordId = oauthUser.getAttribute("id");
        String username  = oauthUser.getAttribute("username");
        String email     = oauthUser.getAttribute("email");  // Discord 이메일

        // DB에서 User 조회 또는 생성
        User user = userRepo.findByDiscordId(discordId)
                .orElseGet(() -> {
                    User u = new User();
                    u.setDiscordId(discordId);
                    u.setUsername(username);
                    u.setNickname(username);
                    u.setEmail(email);
                    u.setFriendCode(friendCodeService.generateUniqueFriendCode());
                    return userRepo.save(u);
                });

        // 기존 유저의 이메일이 없거나 바뀌었으면 업데이트
        boolean dirty = false;
        if (email != null && !email.equals(user.getEmail())) {
            user.setEmail(email);
            dirty = true;
        }
        if (user.getFriendCode() == null || user.getFriendCode().isBlank()) {
            user.setFriendCode(friendCodeService.generateUniqueFriendCode());
            dirty = true;
        }
        if (dirty) userRepo.save(user);

        // 권한 세팅 및 id, username, nickname, email을 OAuth2User attribute에 포함
        return new DefaultOAuth2User(
                AuthorityUtils.createAuthorityList("ROLE_USER"),
                Map.of(
                        "id",       discordId,
                        "username", username,
                        "nickname", user.getNickname(),
                        "email",    user.getEmail()
                ),
                "id"
        );
    }
}
