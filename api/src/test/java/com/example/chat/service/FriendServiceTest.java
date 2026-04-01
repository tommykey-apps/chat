package com.example.chat.service;

import com.example.chat.model.entity.Friendship;
import com.example.chat.model.entity.User;
import com.example.chat.repository.FriendshipRepository;
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
class FriendServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FriendService friendService;

    @Test
    void sendRequest_createsPendingFriendship() {
        when(friendshipRepository.findBetween("user1", "user2")).thenReturn(Optional.empty());
        when(friendshipRepository.save(any(Friendship.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Friendship result = friendService.sendRequest("user1", "user2");

        assertThat(result.getUserId()).isEqualTo("user1");
        assertThat(result.getFriendId()).isEqualTo("user2");
        assertThat(result.getStatus()).isEqualTo("PENDING");

        ArgumentCaptor<Friendship> captor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void sendRequest_throwsOnSelfRequest() {
        assertThatThrownBy(() -> friendService.sendRequest("user1", "user1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot send friend request to yourself");
    }

    @Test
    void sendRequest_throwsIfAlreadyExists() {
        var existing = new Friendship();
        existing.setUserId("user1");
        existing.setFriendId("user2");
        existing.setStatus("PENDING");

        when(friendshipRepository.findBetween("user1", "user2")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> friendService.sendRequest("user1", "user2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Friendship already exists");
    }

    @Test
    void acceptRequest_setsStatusToAccepted() {
        var friendship = new Friendship();
        friendship.setUserId("requester");
        friendship.setFriendId("recipient");
        friendship.setStatus("PENDING");

        when(friendshipRepository.findBetween("requester", "recipient")).thenReturn(Optional.of(friendship));
        when(friendshipRepository.save(any(Friendship.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Friendship result = friendService.acceptRequest("recipient", "requester");

        assertThat(result.getStatus()).isEqualTo("ACCEPTED");
        verify(friendshipRepository).save(friendship);
    }

    @Test
    void acceptRequest_throwsIfNotRecipient() {
        var friendship = new Friendship();
        friendship.setUserId("requester");
        friendship.setFriendId("someone-else");
        friendship.setStatus("PENDING");

        when(friendshipRepository.findBetween("requester", "not-recipient")).thenReturn(Optional.of(friendship));

        assertThatThrownBy(() -> friendService.acceptRequest("not-recipient", "requester"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only the recipient can accept");
    }

    @Test
    void acceptRequest_throwsIfNotFound() {
        when(friendshipRepository.findBetween("requester", "recipient")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendService.acceptRequest("recipient", "requester"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Friend request not found");
    }

    @Test
    void removeFriend_deletesFriendship() {
        var friendship = new Friendship();
        friendship.setUserId("user1");
        friendship.setFriendId("user2");

        when(friendshipRepository.findBetween("user1", "user2")).thenReturn(Optional.of(friendship));

        friendService.removeFriend("user1", "user2");

        verify(friendshipRepository).delete(friendship);
    }

    @Test
    void removeFriend_doesNothingIfNotFound() {
        when(friendshipRepository.findBetween("user1", "user2")).thenReturn(Optional.empty());

        friendService.removeFriend("user1", "user2");

        verify(friendshipRepository, never()).delete(any());
    }

    @Test
    void getFriends_returnsAcceptedFriendsAsUsers() {
        var friendship1 = new Friendship();
        friendship1.setUserId("user1");
        friendship1.setFriendId("user2");
        friendship1.setStatus("ACCEPTED");

        var friendship2 = new Friendship();
        friendship2.setUserId("user3");
        friendship2.setFriendId("user1");
        friendship2.setStatus("ACCEPTED");

        when(friendshipRepository.findAcceptedFriends("user1")).thenReturn(List.of(friendship1, friendship2));

        var user2 = new User();
        user2.setId("user2");
        user2.setEmail("user2@example.com");
        user2.setDisplayName("User2");

        var user3 = new User();
        user3.setId("user3");
        user3.setEmail("user3@example.com");
        user3.setDisplayName("User3");

        when(userRepository.findById("user2")).thenReturn(Optional.of(user2));
        when(userRepository.findById("user3")).thenReturn(Optional.of(user3));

        List<User> friends = friendService.getFriends("user1");

        assertThat(friends).hasSize(2);
        assertThat(friends).extracting(User::getId).containsExactly("user2", "user3");
    }

    @Test
    void getFriends_filtersOutNullUsers() {
        var friendship = new Friendship();
        friendship.setUserId("user1");
        friendship.setFriendId("deleted-user");
        friendship.setStatus("ACCEPTED");

        when(friendshipRepository.findAcceptedFriends("user1")).thenReturn(List.of(friendship));
        when(userRepository.findById("deleted-user")).thenReturn(Optional.empty());

        List<User> friends = friendService.getFriends("user1");

        assertThat(friends).isEmpty();
    }

    @Test
    void getPendingRequests_returnsPendingForUser() {
        var pending1 = new Friendship();
        pending1.setUserId("sender1");
        pending1.setFriendId("user1");
        pending1.setStatus("PENDING");

        var pending2 = new Friendship();
        pending2.setUserId("sender2");
        pending2.setFriendId("user1");
        pending2.setStatus("PENDING");

        when(friendshipRepository.findByFriendIdAndStatus("user1", "PENDING"))
                .thenReturn(List.of(pending1, pending2));

        List<Friendship> requests = friendService.getPendingRequests("user1");

        assertThat(requests).hasSize(2);
        assertThat(requests).allMatch(f -> f.getFriendId().equals("user1"));
        assertThat(requests).allMatch(f -> f.getStatus().equals("PENDING"));
    }
}
