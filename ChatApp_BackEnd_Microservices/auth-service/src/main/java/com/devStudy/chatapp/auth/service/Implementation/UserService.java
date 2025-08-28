package com.devStudy.chatapp.auth.service.Implementation;

import com.devStudy.chatapp.auth.service.Interface.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devStudy.chatapp.auth.repository.UserRepository;
import com.devStudy.chatapp.auth.dto.CreateCompteDTO;
import com.devStudy.chatapp.auth.dto.DTOMapper;
import com.devStudy.chatapp.auth.dto.UserDTO;
import com.devStudy.chatapp.auth.model.User;

import java.util.NoSuchElementException;
import java.util.Optional;

import static com.devStudy.chatapp.auth.utils.ConstantValues.CompteExist;
import static com.devStudy.chatapp.auth.utils.ConstantValues.CreationSuccess;

@Service
public class UserService implements UserDetailsService, IUserService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtTokenService tokenService;

    @Autowired
    public UserService(PasswordEncoder passwordEncoder, UserRepository userRepository, JwtTokenService tokenService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    /**
     * 获取登录用户信息
     */
    @Override
    public UserDTO getLoggedUser(String email) {
        return DTOMapper.toUserDTO(
        	findUserOrAdmin(email,false).orElse(new User())
        );
    }

    /**
     * 添加用户
     */
    @Transactional
    @Override
    public CreateCompteDTO addUser(CreateCompteDTO user) {
        try {
            Optional<User> existingUser = findUserOrAdmin(user.getMail(), false);
            if (existingUser.isPresent()) {
                user.setCreateMsg(CompteExist);
                return user;
            }
            User newUser = new User();
            newUser.setFirstName(user.getFirstName());
            newUser.setLastName(user.getLastName());
            newUser.setMail(user.getMail());
            newUser.setPwd(passwordEncoder.encode(user.getPassword()));
            newUser.setAdmin(false);
            userRepository.save(newUser);
            user.setCreateMsg(CreationSuccess);
            return user;
        }catch(DataIntegrityViolationException e){
            LOGGER.error("Data integrity violation while creating user: {}", e.getMessage());
            user.setCreateMsg(CompteExist);
            return user;
        }
    }

    /**
     * 更新用户失败登录次数
     */
    @Transactional
    @Override
    public int incrementFailedAttemptsOfUser(String userEmail) throws NoSuchElementException {
    	int failedAttempts = findUserOrAdmin(userEmail, false).orElseThrow().getFailedAttempts();
        userRepository.updateFailedAttempts(userEmail,failedAttempts+1);
        return failedAttempts+1;
    }

    /**
     * 锁定用户并重置失败次数
     */
    @Transactional
    @Override
    public void lockUserAndResetFailedAttempts(String userEmail) {
        userRepository.updateActive(userEmail,false);
        resetFailedAttemptsOfUser(userEmail);
    }

    /**
     * 查找用户或管理员
     */
    @Override
    public Optional<User> findUserOrAdmin(String email, boolean isAdmin) {
        return userRepository.findByMailAndAdmin(email, isAdmin);
    }

    /**
     * 重置密码
     */
    @Transactional
    @Override
    public boolean resetPassword(String jwtToken, String password) {
		String email = tokenService.validateTokenAndGetEmail(jwtToken);
		if (email != null && !email.isEmpty()) {
			userRepository.updatePwd(email, passwordEncoder.encode(password));
			return true;
		}
		return false;
    }

	@Override
	public UserDetails loadUserByUsername(String userEmail) throws UsernameNotFoundException {
		Optional<User> account = findUserOrAdmin(userEmail, false);
		if (account.isEmpty()) {
			LOGGER.info("Identifiants incorrects");
			throw new UsernameNotFoundException("Identifiants incorrects");
		}

        if(!account.get().isActive()){
            LOGGER.info("Compté bloqué");
            throw new UsernameNotFoundException("Compte bloqué");
        }
        return account.get();
	}

	@Transactional
    @Override
	public void resetFailedAttemptsOfUser(String username) {
        userRepository.updateFailedAttempts(username, 0);		
	}
}