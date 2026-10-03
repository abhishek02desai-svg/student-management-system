package com.example.sms.service;

import com.example.sms.dto.AuthResponseDto;
import com.example.sms.dto.LoginRequestDto;
import com.example.sms.dto.RegisterRequestDto;
import com.example.sms.dto.StudentResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {

    StudentResponseDto register(RegisterRequestDto dto, MultipartFile photo);

    AuthResponseDto login(LoginRequestDto dto);
}
