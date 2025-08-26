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
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

	private final UserRepository userRepository;
	private final FriendCodeService friendCodeService;

	public CustomOAuth2UserService(UserRepository userRepository, FriendCodeService friendCodeService) {
		this.userRepository = userRepository;
		this.friendCodeService = friendCodeService;
	}

	@Override
	public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
		DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
		OAuth2User oauthUser = delegate.loadUser(request);

		String registrationId = request.getClientRegistration().getRegistrationId();

		if ("discord".equalsIgnoreCase(registrationId)) {
			return handleDiscord(oauthUser);
		} else if ("kakao".equalsIgnoreCase(registrationId)) {
			return handleKakao(oauthUser);
		}

		throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + registrationId);
	}

	private OAuth2User handleDiscord(OAuth2User oauthUser) {
		String discordId = oauthUser.getAttribute("id");
		String username = oauthUser.getAttribute("username");
		String email = oauthUser.getAttribute("email");

		// 1) discordId로 기존 연동 계정이 있으면 사용
		User user = userRepository.findByDiscordId(discordId).orElse(null);


		// 2) 없으면 이메일로 기존 계정을 조회하되, 이미 다른 소셜로 연결되어 있으면 거절(교차 병합 방지)
		if (user == null && email != null && !email.isBlank()) {
			User byEmail = userRepository.findByEmail(email).orElse(null);
			if (byEmail != null) {
				// 카카오로 이미 연결되어 있거나(혹은 다른 소셜) 이메일이 점유된 경우 실패 처리
				if (byEmail.getKakaoId() != null && !byEmail.getKakaoId().isBlank()) {
					throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException("oauth_email_linked:kakao");
				}
				if (byEmail.getDiscordId() != null && !byEmail.getDiscordId().isBlank()) {
					// 이 경우는 논리상 위에서 잡혔어야 하지만, 안전망
					throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException("oauth_email_linked:discord");
				}
				// 로컬 계정만 있어도 자동 병합하지 않음
				throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException("oauth_email_linked:local");
			}
		}

		// 3) 그래도 없으면 신규 생성 (username 고유성 보장)
		if (user == null) {
			String uniqueUsername = (discordId != null && !discordId.isBlank()) ? ("discord_" + discordId) : (username != null ? username : "discord_user");
			user = new User();
			user.setDiscordId(discordId);
			user.setUsername(uniqueUsername);
			user.setNickname(username != null ? username : uniqueUsername);
			user.setEmail(email);
			user.setFriendCode(friendCodeService.generateUniqueFriendCode());
			user = userRepository.save(user);
		}

		boolean dirty = false;
		if (email != null && !email.equals(user.getEmail())) {
			user.setEmail(email);
			dirty = true;
		}
		if (user.getFriendCode() == null || user.getFriendCode().isBlank()) {
			user.setFriendCode(friendCodeService.generateUniqueFriendCode());
			dirty = true;
		}
		if (dirty) userRepository.save(user);

		return new DefaultOAuth2User(
				AuthorityUtils.createAuthorityList("ROLE_USER"),
				Map.of(
						"id", discordId,
						"username", username,
						"nickname", user.getNickname(),
						"email", user.getEmail(),
						"provider", "discord"
				),
				"id"
		);
	}

	@SuppressWarnings("unchecked")
	private OAuth2User handleKakao(OAuth2User oauthUser) {
		// Kakao 응답 예시:
		// {
		//   id: 12345,
		//   properties: { nickname: "홍길동", ... },
		//   kakao_account: { email: "user@example.com", profile: { nickname: "홍길동" } }
		// }
		Object idObj = oauthUser.getAttribute("id");
		String kakaoId = idObj != null ? String.valueOf(idObj) : null;
		Map<String, Object> properties = oauthUser.getAttribute("properties");
		Map<String, Object> kakaoAccount = oauthUser.getAttribute("kakao_account");

		String nickname = null;
		if (properties != null) {
			nickname = (String) properties.get("nickname");
		}
		if ((nickname == null || nickname.isBlank()) && kakaoAccount != null) {
			Object profile = kakaoAccount.get("profile");
			if (profile instanceof Map<?, ?> profileMap) {
				Object n = profileMap.get("nickname");
				nickname = n != null ? String.valueOf(n) : null;
			}
		}
		String email = null;
		if (kakaoAccount != null) {
			Object e = kakaoAccount.get("email");
			email = e != null ? String.valueOf(e) : null;
		}

		// username은 고유해야 하므로 kakao_{id} 형태로 보장
		String username = (kakaoId != null) ? ("kakao_" + kakaoId) : null;
		if (nickname == null || nickname.isBlank()) {
			nickname = username;
		}

		final String nicknameFinal = nickname;
		final String emailFinal = email;


		// 1) kakaoId로 기존 연동 계정이 있으면 사용
		User user = userRepository.findByKakaoId(kakaoId).orElse(null);

		// 2) 없으면 이메일로 기존 계정을 조회하되, 이미 다른 소셜로 연결되어 있으면 거절(교차 병합 방지)
		if (user == null && email != null && !email.isBlank()) {
			User byEmail = userRepository.findByEmail(email).orElse(null);
			if (byEmail != null) {
				if (byEmail.getDiscordId() != null && !byEmail.getDiscordId().isBlank()) {
					throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException("oauth_email_linked:discord");
				}
				if (byEmail.getKakaoId() != null && !byEmail.getKakaoId().isBlank()) {
					throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException("oauth_email_linked:kakao");
				}
				throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException("oauth_email_linked:local");
			}
		}

		// 3) 신규 생성
		if (user == null) {
			User u = new User();
			u.setKakaoId(kakaoId);
			u.setUsername(username);
			u.setNickname(nicknameFinal);
			u.setEmail(emailFinal);
			u.setFriendCode(friendCodeService.generateUniqueFriendCode());
			user = userRepository.save(u);
		}

		boolean dirty = false;
		if (email != null && !email.equals(user.getEmail())) {
			user.setEmail(email);
			dirty = true;
		}
		if (nickname != null && !nickname.equals(user.getNickname())) {
			user.setNickname(nickname);
			dirty = true;
		}
		if (user.getFriendCode() == null || user.getFriendCode().isBlank()) {
			user.setFriendCode(friendCodeService.generateUniqueFriendCode());
			dirty = true;
		}
		if (dirty) userRepository.save(user);

		return new DefaultOAuth2User(
				AuthorityUtils.createAuthorityList("ROLE_USER"),
				Map.of(
						"id", kakaoId,
						"username", username,
						"nickname", user.getNickname(),
						"email", user.getEmail(),
						"provider", "kakao"
				),
				"id"
		);
	}
}


