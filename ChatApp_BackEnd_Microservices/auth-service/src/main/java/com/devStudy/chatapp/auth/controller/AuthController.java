package com.devStudy.chatapp.auth.controller;

import com.devStudy.chatapp.auth.dto.DTOMapper;
import com.devStudy.chatapp.auth.model.User;
import com.devStudy.chatapp.auth.service.Implementation.BlackListService;
import com.devStudy.chatapp.auth.utils.RabbitMQUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devStudy.chatapp.auth.dto.CreateCompteDTO;
import com.devStudy.chatapp.auth.dto.UserDTO;
import com.devStudy.chatapp.auth.service.Implementation.JwtTokenService;
import com.devStudy.chatapp.auth.service.Implementation.UserService;

import java.util.Map;
import java.util.Optional;

import jakarta.annotation.Resource;

@RestController
@RequestMapping(value = "/api/auth")
public class AuthController {
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

	@Resource
	private UserService userService;

	@Resource
	private JwtTokenService jwtTokenService;

	@Resource
	private BlackListService blackListService;

	@Resource
	private RabbitMQUtil rabbitMQUtil;
	
	/**
	 * 检查用户登录状态
	 */
    @GetMapping("/check-login")
    public ResponseEntity<UserDTO> getLoggedUser(HttpServletRequest request){
		String token = jwtTokenService.getTokenFromCookie(request);
		if(token != null && !blackListService.isTokenInBlackList(token)){
			final String email = jwtTokenService.validateTokenAndGetEmail(token);
			if(email != null) {
				UserDTO user = userService.getLoggedUser(email);
				if(user.getId() == 0) {
					LOGGER.error("User not found, but token is valid");
					blackListService.addTokenToBlackList(token, jwtTokenService.getExpirationDate(token).getTime());
				}
				return ResponseEntity.ok(user);
			}
		}

		return ResponseEntity.ok(new UserDTO());
    }

	/**
	 * 获取验证码
	 */
	@GetMapping("/verification-code")
	public ResponseEntity<Map<String,String>> getVerificationCode(@RequestParam String email) {
		Optional<User> user = userService.findUserOrAdmin(email, false);
		if (user.isEmpty()) {
			return ResponseEntity.ok(Map.ofEntries(
					Map.entry("status", "error"),
					Map.entry("msg", "User n'existe pas, veuillez vous inscrire"))
			);
		}
		return ResponseEntity.ok(Map.ofEntries(Map.entry("status", "success"),
				Map.entry("msg", rabbitMQUtil.sendVerificationCodeRequestToMQ(email))));
	}

	/**
	 * 忘记密码
	 */
	@PostMapping(value = "/forget-password")
	public ResponseEntity<Map<String, String>> postForgetPasswordPage(@RequestParam(value = "email") String email) {
		return ResponseEntity.ok(rabbitMQUtil.sendResetPwdEmailRequestToMQ(email));
	}

	/**
	 * 验证重置密码token
	 */
	@GetMapping(value = "/validate-token")
	public ResponseEntity<Boolean> validateToken(@RequestParam(value = "token") String token) {
	    return ResponseEntity.ok(jwtTokenService.validateToken(token));
	}
	
	/**
	 * 重置密码
	 */
	@PutMapping(value = "/reset-password")
	public ResponseEntity<Boolean> resetPassword(@RequestParam(value = "token") String token,
            @RequestParam(value = "password") String password) {
        return ResponseEntity.ok(userService.resetPassword(token, password));		
	}
	
	/**
	 * 用户注册
	 */
	@PostMapping(value = "/register")
	public ResponseEntity<CreateCompteDTO> createUserCompte(@RequestBody CreateCompteDTO createCompteDTO){
		return ResponseEntity.ok(userService.addUser(createCompteDTO));
	}

	/**
	 * 用户登出
	 */
	@PostMapping(value = "/logout")
	public ResponseEntity<?> logout(HttpServletRequest request){
		String jwtToken = jwtTokenService.getTokenFromCookie(request);
		String resultMsg;
		if(jwtToken != null) {
			if(jwtTokenService.validateToken(jwtToken))
				blackListService.addTokenToBlackList(jwtToken, jwtTokenService.getExpirationDate(jwtToken).getTime());
			resultMsg = "Logout successful";
		}else{
			resultMsg = "Unnecessary logout, you are not logged in";
		}
		return ResponseEntity.ok(Map.ofEntries(
				Map.entry("message", resultMsg)
		));
	}

	/**
	 * 通过邮箱获取用户信息（供网关使用）
	 */
	@GetMapping("/user-info")
	public ResponseEntity<UserDTO> getUserInfo(@RequestParam String email) {
		Optional<User> user = userService.findUserOrAdmin(email, false);
		UserDTO userDTO = user.map(DTOMapper::toUserDTO).orElse(new UserDTO());
		return ResponseEntity.ok(userDTO);
	}
}