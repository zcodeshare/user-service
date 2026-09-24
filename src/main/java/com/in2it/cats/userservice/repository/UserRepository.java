package com.in2it.cats.userservice.repository;

import com.in2it.cats.userservice.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
}