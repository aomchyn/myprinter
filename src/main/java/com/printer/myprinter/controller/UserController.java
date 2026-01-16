package com.printer.myprinter.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.printer.myprinter.entity.UserEntity;
import com.printer.myprinter.repository.UserRepository;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Date;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/printer/user")
public class UserController {
    private final UserRepository userRepository;

    private static final long EXPIRATION_TIME = 60 * 60 * 1000 * 24 *7;

    public UserController (UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<UserEntity> getAllUsers(){
        return userRepository.findAll();
    }

    @PostMapping
    public UserEntity createUser (@RequestBody UserEntity user){
        return userRepository.save(user);
    }

    @GetMapping("/{id}")
    public UserEntity getUserById (@PathVariable Long id){
    return userRepository.findById(id).orElse(null);
    }

    @PutMapping("/{id}")
    public UserEntity updateUser (@PathVariable Long id, @RequestBody UserEntity user){
        UserEntity userToUpdate = userRepository.findById(id).orElse(null);

        if (userToUpdate == null){
            throw new IllegalArgumentException("Not found");
        }

        userToUpdate.setName(user.getName());
        userToUpdate.setEmail(user.getEmail());

        return userRepository.save(userToUpdate);
    }

    @DeleteMapping("/{id}")
    public  void dropUser (@PathVariable Long id){
        userRepository.deleteById(id);
    }
 
    @PostMapping("/signin")
    public UserEntity signin (@RequestBody UserEntity user){
        String name = user.getName();
        String email = user.getEmail();

        UserEntity userToSignin = userRepository.findByNameAndEmail(name, email);

        if (userToSignin == null){
            throw new IllegalArgumentException("User not found");
        }

        return userToSignin;

    }

    private String getSecret (){
        Dotenv dotenv = Dotenv.configure()
                        .directory(System.getProperty("user.dir"))
                        .load();

                        return dotenv.get("JWT_SECRET");
    }

    private Algorithm getAlgorithm(){
        return Algorithm.HMAC256(getSecret());
    }

    @PostMapping("/admin-signin")
    public Object adminSignin(@RequestBody UserEntity user){
        try {
            String u = user.getName();
            String p = user.getPassword();

            UserEntity userForCreateToken = userRepository.findByNameAndPassword(u, p);

            String token = JWT.create()
                           .withSubject(String.valueOf(userForCreateToken.getId()))
                           .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                           .withIssuedAt(new Date())
                           .sign(getAlgorithm());

                           String role = userForCreateToken.getRole();

                           record UserResponse (String token, String role){

                           }
                           return new UserResponse(token, role);
                 } catch (IllegalArgumentException e){
                    throw new IllegalArgumentException("Error Create token");
                 }
    }

    @GetMapping("/admin-info")
    public Object adminInfo (@RequestHeader ("Authorization") String token){
        try{
            if (token == null || !token.startsWith("Bearer ")){
                throw new IllegalArgumentException("Invalid Token Format with 'Bearer '");
            }
            
            String tokenWithoutBearer = token.replace("Bearer ", "");
            if (tokenWithoutBearer.trim().isEmpty()){
                throw new IllegalArgumentException("Token is Empty");
            }

            String subject = JWT.require(getAlgorithm())
               .build()
               .verify(tokenWithoutBearer)
               .getSubject();

               Long userId = Long.valueOf(subject);
               UserEntity user = userRepository.findById(userId).orElse(null);

               if (user == null){
                throw new IllegalArgumentException("User not found");
               }

               String role = user.getRole();

               record UserResponse(Long id, String name, String email, String role){

               }

               return new UserResponse(user.getId(),user.getName(),user.getEmail(),role);


        }catch (IllegalArgumentException e){
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private Long getUserIdFromToken (String token){
        String tokenWithoutBearer = token.replace("Bearer ", "");

        if (tokenWithoutBearer.trim().isEmpty()){
            throw new IllegalArgumentException("Token is null");
        }

        return Long.valueOf(JWT.require(getAlgorithm())
               .build()
               .verify(tokenWithoutBearer)
               .getSubject()
        );
    }

    @PostMapping("/admin-edit-profile")
    public UserEntity adminEditProfile(@RequestHeader ("Authorization") String token , @RequestBody UserEntity user){

        Long userId = getUserIdFromToken(token);
        UserEntity userToupdate = userRepository.findById(userId).orElse(null);

        if (user.getPassword() != null && !user.getPassword().isEmpty()){
            userToupdate.setPassword(user.getPassword());
        }

        userRepository.save(userToupdate);

        return userToupdate;
    }

    @PostMapping("/admin-create")
    public UserEntity adminCreate(
        @RequestHeader("Authorization") String token,
        @RequestBody UserEntity user){
            try {
                userRepository.save(user);
                return user;
            } catch (IllegalArgumentException e){
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        @DeleteMapping("/admin-delete/{id}")
        public void adminDelete (@RequestHeader ("Authorization") String token , @PathVariable Long id){
            try {
                  UserEntity userToUpdate = userRepository.findById(id).orElse(null);

                  if (userToUpdate == null){
                    throw new IllegalArgumentException("User not found");
                  }

                  userRepository.deleteById(id);

            } catch(IllegalArgumentException e){
                throw new IllegalArgumentException(e.getMessage());

            }
        }

        @PostMapping("/admin-update-profile")
        public UserEntity adminUpdateProfile(@RequestHeader ("Authorization") String token, @RequestBody UserEntity user){
            try {
                UserEntity usertoupdate = userRepository.findById(user.getId()).orElse(null);
               if (usertoupdate == null){
                throw new IllegalArgumentException("User not found");
               }

               usertoupdate.setName(user.getName());
               usertoupdate.setEmail(user.getEmail());

               if (user.getPassword() != null && !user.getPassword().isEmpty()){
                usertoupdate.setPassword(user.getPassword());
               }

               usertoupdate.setRole(user.getRole());

               userRepository.save(usertoupdate);

               return usertoupdate;
                

            } catch (IllegalArgumentException e){
               throw new IllegalArgumentException(e.getMessage());
            }
        }

}
