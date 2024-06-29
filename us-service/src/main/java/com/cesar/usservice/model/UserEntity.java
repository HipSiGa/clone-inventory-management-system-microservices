package com.cesar.usservice.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document(collection = "users")
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity {
    @MongoId
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String role;

   public String getRoleWithPrefix() {
       return "ROLE_" + this.role;
   }
}
