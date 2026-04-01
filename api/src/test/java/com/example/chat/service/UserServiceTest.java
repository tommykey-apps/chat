package com.example.chat.service;

import com.example.chat.model.entity.User;
import com.example.chat.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void ensureUser_createsNewUserWhenNotExists() {
        when(userRepository.findById("new-user")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.ensureUser("new-user", "alice@example.com");

        assertThat(result.getId()).isEqualTo("new-user");
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getDisplayName()).isEqualTo("alice");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getDisplayName()).isEqualTo("alice");
    }

    @Test
    void ensureUser_returnsExistingUser() {
        var existing = new User();
        existing.setId("existing-user");
        existing.setEmail("bob@example.com");
        existing.setDisplayName("Bob");

        when(userRepository.findById("existing-user")).thenReturn(Optional.of(existing));

        User result = userService.ensureUser("existing-user", "bob@example.com");

        assertThat(result.getId()).isEqualTo("existing-user");
        assertThat(result.getDisplayName()).isEqualTo("Bob");
        verify(userRepository, never()).save(any());
    }

    @Test
    void ensureUser_usesEmailPrefixAsDisplayName() {
        when(userRepository.findById("user1")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.ensureUser("user1", "john.doe@company.co.jp");

        assertThat(result.getDisplayName()).isEqualTo("john.doe");
    }

    @Test
    void getUser_returnsUserWhenFound() {
        var user = new User();
        user.setId("user1");
        user.setEmail("test@example.com");
        user.setDisplayName("Test");

        when(userRepository.findById("user1")).thenReturn(Optional.of(user));

        User result = userService.getUser("user1");

        assertThat(result.getId()).isEqualTo("user1");
        assertThat(result.getDisplayName()).isEqualTo("Test");
    }

    @Test
    void getUser_throwsRuntimeExceptionWhenNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser("missing"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void searchUsers_delegatesToRepository() {
        var user1 = new User();
        user1.setId("u1");
        user1.setDisplayName("Alice");

        var user2 = new User();
        user2.setId("u2");
        user2.setDisplayName("Alicia");

        when(userRepository.findByEmailContainingIgnoreCaseOrDisplayNameContainingIgnoreCase("ali", "ali"))
                .thenReturn(List.of(user1, user2));

        List<User> results = userService.searchUsers("ali");

        assertThat(results).hasSize(2);
        assertThat(results).extracting(User::getDisplayName).containsExactly("Alice", "Alicia");
        verify(userRepository).findByEmailContainingIgnoreCaseOrDisplayNameContainingIgnoreCase("ali", "ali");
    }

    @Test
    void searchUsers_returnsEmptyListWhenNoMatch() {
        when(userRepository.findByEmailContainingIgnoreCaseOrDisplayNameContainingIgnoreCase("xyz", "xyz"))
                .thenReturn(List.of());

        List<User> results = userService.searchUsers("xyz");

        assertThat(results).isEmpty();
    }
}
