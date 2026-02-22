package com.fintrack.backend.controller;

import com.fintrack.backend.dto.UserResponseDto;
import com.fintrack.backend.dto.UserUpdateDto;
import com.fintrack.backend.dto.PasswordChangeDto;
import com.fintrack.backend.service.UserService;
import com.fintrack.backend.security.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private UserService userService;

        @MockBean
        private UserDetailsService userDetailsService;

        @MockBean
        private JwtUtils jwtUtils;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        @WithMockUser
        void getUserProfile_success() throws Exception {
                UserResponseDto response = UserResponseDto.builder()
                                .id(1L)
                                .username("testuser")
                                .email("test@example.com")
                                .build();
                when(userService.getUserProfile(1L)).thenReturn(response);

                mockMvc.perform(get("/api/users/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("testuser"));
        }

        @Test
        @WithMockUser
        void updateUser_success() throws Exception {
                UserUpdateDto dto = new UserUpdateDto();
                dto.setUsername("newname");
                dto.setEmail("new@example.com");

                UserResponseDto response = UserResponseDto.builder()
                                .id(1L)
                                .username("newname")
                                .email("new@example.com")
                                .build();

                when(userService.updateUser(eq(1L), any(UserUpdateDto.class))).thenReturn(response);
                when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mock(UserDetails.class));
                when(jwtUtils.generateToken(any(UserDetails.class))).thenReturn("new-token");

                mockMvc.perform(put("/api/users/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("newname"))
                                .andExpect(jsonPath("$.token").value("new-token"));
        }

        @Test
        @WithMockUser
        void changePassword_success() throws Exception {
                PasswordChangeDto dto = new PasswordChangeDto();
                dto.setOldPassword("oldpass");
                dto.setNewPassword("newpass123");

                UserResponseDto response = UserResponseDto.builder()
                                .id(1L)
                                .username("testuser")
                                .email("test@example.com")
                                .build();

                doNothing().when(userService).changePassword(eq(1L), any(PasswordChangeDto.class));
                when(userService.getUserProfile(1L)).thenReturn(response);
                when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mock(UserDetails.class));
                when(jwtUtils.generateToken(any(UserDetails.class))).thenReturn("pass-token");

                mockMvc.perform(put("/api/users/1/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").value("pass-token"));
        }
}
