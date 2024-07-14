package com.shashank.demoscim.controller;

import com.shashank.demoscim.model.ScimUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/scim/v2")
public class ScimController {
    private List<ScimUser> users = new ArrayList<>();

    @GetMapping("/Users")
    public ResponseEntity<List<ScimUser>> getUsers() {
        return ResponseEntity.ok(users);
    }

    @PostMapping("/Users")
    public ResponseEntity<ScimUser> createUser(@RequestBody ScimUser user) {
        user.setId(String.valueOf(users.size() + 1));
        users.add(user);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/Users/{id}")
    public ResponseEntity<ScimUser> getUser(@PathVariable String id) {
        return users.stream()
                .filter(user -> user.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/Users/{id}")
    public ResponseEntity<ScimUser> updateUser(@PathVariable String id, @RequestBody ScimUser updatedUser) {
        return users.stream()
                .filter(user -> user.getId().equals(id))
                .findFirst()
                .map(user -> {
                    user.setUserName(updatedUser.getUserName());
                    user.setName(updatedUser.getName());
                    return ResponseEntity.ok(user);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/Users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        users.removeIf(user -> user.getId().equals(id));
        return ResponseEntity.noContent().build();
    }
}
