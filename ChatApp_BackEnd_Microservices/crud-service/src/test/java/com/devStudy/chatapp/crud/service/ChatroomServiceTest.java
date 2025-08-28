package com.devStudy.chatapp.crud.service;

import com.devStudy.chatapp.crud.dto.*;
import com.devStudy.chatapp.crud.events.ChangeChatroomMemberEvent;
import com.devStudy.chatapp.crud.events.RemoveChatroomEvent;
import com.devStudy.chatapp.crud.model.Chatroom;
import com.devStudy.chatapp.crud.model.User;
import com.devStudy.chatapp.crud.repository.ChatroomRepository;
import com.devStudy.chatapp.crud.repository.UserRepository;
import com.devStudy.chatapp.crud.service.Implementation.ChatroomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatroomServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatroomRepository chatroomRepository;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private ChatroomService chatroomService;

    private User creator;
    private User member1;
    private User member2;
    private Chatroom chatroom;
    private ChatroomRequestDTO chatroomRequestDTO;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chatroomService, "DefaultPageSize_Chatrooms", 10);

        // 创建测试用户
        creator = new User();
        creator.setId(1L);
        creator.setFirstName("Creator");
        creator.setLastName("User");
        creator.setMail("creator@test.com");
        creator.setCreatedRooms(new HashSet<>());

        member1 = new User();
        member1.setId(2L);
        member1.setFirstName("Member");
        member1.setLastName("One");
        member1.setMail("member1@test.com");
        member1.setJoinedRooms(new HashSet<>());

        member2 = new User();
        member2.setId(3L);
        member2.setFirstName("Member");
        member2.setLastName("Two");
        member2.setMail("member2@test.com");
        member2.setJoinedRooms(new HashSet<>());

        // 创建测试聊天室
        chatroom = new Chatroom();
        chatroom.setId(100L);
        chatroom.setTitre("Test Chatroom");
        chatroom.setDescription("Test Description");
        chatroom.setCreator(creator);
        chatroom.setHoraireCommence(LocalDateTime.now().plusHours(1));
        chatroom.setHoraireTermine(LocalDateTime.now().plusDays(1));
        chatroom.setActive(true);
        chatroom.setMembers(new HashSet<>());

        // 创建聊天室请求DTO
        chatroomRequestDTO = new ChatroomRequestDTO();
        chatroomRequestDTO.setTitre("New Chatroom");
        chatroomRequestDTO.setDescription("New Description");
        chatroomRequestDTO.setStartDate(LocalDateTime.now().plusHours(1).toString());
        chatroomRequestDTO.setDuration_days(7);
        chatroomRequestDTO.setUsersInvited(Arrays.asList(
                DTOMapper.toUserDTO(member1),
                DTOMapper.toUserDTO(member2)
        ));
    }

    @Test
    void testFindChatroom_Success() {
        // 测试查找聊天室成功
        when(chatroomRepository.findById(chatroom.getId())).thenReturn(Optional.of(chatroom));

        Optional<ModifyChatroomDTO> result = chatroomService.findChatroom(chatroom.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getTitre()).isEqualTo(chatroom.getTitre());
        assertThat(result.get().getDescription()).isEqualTo(chatroom.getDescription());

        verify(chatroomRepository).findById(chatroom.getId());
    }

    @Test
    void testFindChatroom_NotFound() {
        // 测试查找不存在的聊天室
        when(chatroomRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<ModifyChatroomDTO> result = chatroomService.findChatroom(999L);

        assertThat(result).isEmpty();
        verify(chatroomRepository).findById(999L);
    }

    @Test
    void testCreateChatroom_Success() {
        // 测试创建聊天室成功
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(userRepository.findAllById(Arrays.asList(member1.getId(), member2.getId())))
                .thenReturn(Arrays.asList(member1, member2));
        when(chatroomRepository.findAll()).thenReturn(Collections.emptyList());
        when(chatroomRepository.save(any(Chatroom.class))).thenReturn(chatroom);

        boolean result = chatroomService.createChatroom(chatroomRequestDTO, creator.getId());

        assertThat(result).isTrue();
        verify(userRepository).findById(creator.getId());
        verify(userRepository).findAllById(Arrays.asList(member1.getId(), member2.getId()));
        verify(chatroomRepository).save(any(Chatroom.class));
    }

    @Test
    void testCreateChatroom_UserNotFound() {
        // 测试创建者不存在的情况
        when(userRepository.findById(creator.getId())).thenReturn(Optional.empty());

        boolean result = chatroomService.createChatroom(chatroomRequestDTO, creator.getId());

        assertThat(result).isFalse();
        verify(chatroomRepository, never()).save(any(Chatroom.class));
    }

    @Test
    void testGetChatroomsOwnedOfUserByPage() {
        // 测试获取用户拥有的聊天室
        Page<Chatroom> chatroomPage = new PageImpl<>(Collections.singletonList(chatroom));
        when(chatroomRepository.findChatroomsCreatedByUserByPage(eq(creator.getId()), any(Pageable.class)))
                .thenReturn(chatroomPage);

        Page<ChatroomDTO> result = chatroomService.getChatroomsOwnedOfUserByPage(creator.getId(), 0);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitre()).isEqualTo(chatroom.getTitre());

        verify(chatroomRepository).findChatroomsCreatedByUserByPage(eq(creator.getId()), any(Pageable.class));
    }

    @Test
    void testGetChatroomsJoinedOfUserByPage() {
        // 测试获取用户加入的聊天室
        Page<Chatroom> chatroomPage = new PageImpl<>(Collections.singletonList(chatroom));
        when(chatroomRepository.findChatroomsJoinedOfUserByPage(eq(member1.getId()), any(Pageable.class)))
                .thenReturn(chatroomPage);

        Page<ChatroomWithOwnerAndStatusDTO> result = chatroomService.getChatroomsJoinedOfUserByPage(member1.getId(), false, 0);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitre()).isEqualTo(chatroom.getTitre());

        verify(chatroomRepository).findChatroomsJoinedOfUserByPage(eq(member1.getId()), any(Pageable.class));
    }

    @Test
    void testGetAllUsersInChatroom() {
        // 测试获取聊天室中的所有用户
        chatroom.getMembers().add(member1);
        chatroom.getMembers().add(member2);
        
        when(chatroomRepository.findById(chatroom.getId())).thenReturn(Optional.of(chatroom));

        List<UserDTO> result = chatroomService.getAllUsersInChatroom(chatroom.getId());

        assertThat(result).hasSize(3); // 2个成员 + 1个创建者
        assertThat(result).extracting(UserDTO::getId)
                .containsExactlyInAnyOrder(creator.getId(), member1.getId(), member2.getId());

        verify(chatroomRepository).findById(chatroom.getId());
    }

    @Test
    void testDeleteChatRoom_Success() {
        // 测试删除聊天室成功
        Set<User> members = new HashSet<>(Arrays.asList(member1, member2));
        chatroom.setMembers(members);
        creator.getCreatedRooms().add(chatroom);
        member1.getJoinedRooms().add(chatroom);
        member2.getJoinedRooms().add(chatroom);
        
        when(chatroomRepository.findById(chatroom.getId())).thenReturn(Optional.of(chatroom));

        boolean result = chatroomService.deleteChatRoom(chatroom.getId());

        assertThat(result).isTrue();
        verify(chatroomRepository).findById(chatroom.getId());
        verify(chatroomRepository).delete(chatroom);
        verify(publisher).publishEvent(any(RemoveChatroomEvent.class));
    }

    @Test
    void testSetStatusOfChatroom() {
        // 测试设置聊天室状态
        when(chatroomRepository.findById(chatroom.getId())).thenReturn(Optional.of(chatroom));

        chatroomService.setStatusOfChatroom(chatroom.getId(), false);

        verify(chatroomRepository).findById(chatroom.getId());
        verify(chatroomRepository).updateActive(chatroom.getId(), false);
    }

    @Test
    void testDeleteUserInvited_Success() {
        // 测试删除邀请用户成功
        chatroom.getMembers().add(member1);
        member1.getJoinedRooms().add(chatroom);
        
        when(chatroomRepository.findById(chatroom.getId())).thenReturn(Optional.of(chatroom));

        boolean result = chatroomService.deleteUserInvited(chatroom.getId(), member1.getId());

        assertThat(result).isTrue();
        verify(chatroomRepository).findById(chatroom.getId());
        verify(publisher).publishEvent(any(ChangeChatroomMemberEvent.class));
    }

    @Test
    void testUpdateChatroom_Success() {
        // 测试更新聊天室成功
        ModifyChatroomRequestDTO updateRequest = new ModifyChatroomRequestDTO();
        updateRequest.setTitre("Updated Title");
        updateRequest.setDescription("Updated Description");
        updateRequest.setStartDate(LocalDateTime.now().plusHours(2).toString());
        updateRequest.setDuration(10);
        updateRequest.setListAddedUsers(List.of(
                DTOMapper.toUserDTO(member1)
        ));
        updateRequest.setListRemovedUsers(Collections.emptyList());

        when(chatroomRepository.findById(chatroom.getId())).thenReturn(Optional.of(chatroom));
        when(userRepository.findById(member1.getId())).thenReturn(Optional.of(member1));
        when(chatroomRepository.save(any(Chatroom.class))).thenReturn(chatroom);

        boolean result = chatroomService.updateChatroom(updateRequest, chatroom.getId());

        assertThat(result).isTrue();
        verify(chatroomRepository).findById(chatroom.getId());
        verify(chatroomRepository).save(chatroom);
        verify(publisher).publishEvent(any(ChangeChatroomMemberEvent.class));
    }

    @Test
    void testCheckUserIsOwnerOfChatroom_True() {
        // 测试用户是聊天室所有者
        when(chatroomRepository.findByIdAndCreatorId(chatroom.getId(), creator.getId()))
                .thenReturn(Optional.of(chatroom));

        boolean result = chatroomService.checkUserIsOwnerOfChatroom(creator.getId(), chatroom.getId());

        assertThat(result).isTrue();
        verify(chatroomRepository).findByIdAndCreatorId(chatroom.getId(), creator.getId());
    }

    @Test
    void testCheckUserIsOwnerOfChatroom_False() {
        // 测试用户不是聊天室所有者
        when(chatroomRepository.findByIdAndCreatorId(chatroom.getId(), member1.getId()))
                .thenReturn(Optional.empty());

        boolean result = chatroomService.checkUserIsOwnerOfChatroom(member1.getId(), chatroom.getId());

        assertThat(result).isFalse();
        verify(chatroomRepository).findByIdAndCreatorId(chatroom.getId(), member1.getId());
    }

    @Test
    void testEventPublishing() {
        // 测试事件发布
        ArgumentCaptor<ChangeChatroomMemberEvent> eventCaptor = ArgumentCaptor.forClass(ChangeChatroomMemberEvent.class);
        
        chatroom.getMembers().add(member1);
        member1.getJoinedRooms().add(chatroom);
        when(chatroomRepository.findById(chatroom.getId())).thenReturn(Optional.of(chatroom));

        chatroomService.deleteUserInvited(chatroom.getId(), member1.getId());

        verify(publisher).publishEvent(eventCaptor.capture());
        ChangeChatroomMemberEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getChatroomId()).isEqualTo(chatroom.getId());
        assertThat(capturedEvent.getRemovedMembers()).hasSize(1);
        assertThat(capturedEvent.getRemovedMembers().get(0).getId()).isEqualTo(member1.getId());
    }

    @Test
    void testCreateChatroom_DuplicateChatroom() {
        // 测试创建重复聊天室
        Chatroom existingChatroom = new Chatroom();
        existingChatroom.setTitre("New Chatroom");
        existingChatroom.setDescription("New Description");
        
        when(chatroomRepository.findAll()).thenReturn(List.of(existingChatroom));

        boolean result = chatroomService.createChatroom(chatroomRequestDTO, creator.getId());

        assertThat(result).isFalse();
        verify(chatroomRepository, never()).save(any(Chatroom.class));
    }
}