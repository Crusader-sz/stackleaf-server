package com.crusader.stackleafserver.controller;

import com.crusader.stackleafserver.constant.MessageConstant;
import com.crusader.stackleafserver.constant.ResultCodeConstant;
import com.crusader.stackleafserver.exception.BusinessException;
import com.crusader.stackleafserver.handler.GlobalExceptionHandler;
import com.crusader.stackleafserver.model.dto.UserLoginDTO;
import com.crusader.stackleafserver.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private UserService userService;
    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"username\":\"\",\"password\":\"secret\"}",
            "{\"username\":\"   \",\"password\":\"secret\"}",
            "{\"username\":\"tester\",\"password\":\"\"}",
            "{\"username\":\"tester\",\"password\":\"   \"}"
    })
    void invalidLoginIsRejectedBeforeCallingService(String body) throws Exception {
        mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeConstant.BAD_REQUEST))
                .andExpect(jsonPath("$.message", containsString("不能为空")))
                .andExpect(jsonPath("$.data").value(nullValue()));
        verifyNoInteractions(userService);
    }

    @Test
    void successfulLoginReturnsTokenInData() throws Exception {
        when(userService.login(any(UserLoginDTO.class))).thenReturn("test-sa-token");
        mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tester\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeConstant.SUCCESS))
                .andExpect(jsonPath("$.message").value(MessageConstant.SUCCESS))
                .andExpect(jsonPath("$.data").value("test-sa-token"));
    }

    @Test
    void incorrectCredentialsKeepUnauthorizedResponse() throws Exception {
        when(userService.login(any(UserLoginDTO.class))).thenThrow(new BusinessException(
                ResultCodeConstant.UNAUTHORIZED, MessageConstant.USERNAME_OR_PASSWORD_ERROR));
        mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tester\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeConstant.UNAUTHORIZED))
                .andExpect(jsonPath("$.message").value(MessageConstant.USERNAME_OR_PASSWORD_ERROR));
    }
}
