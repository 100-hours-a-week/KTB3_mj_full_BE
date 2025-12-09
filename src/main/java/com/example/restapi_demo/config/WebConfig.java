package com.example.restapi_demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 지금은 CORS는 SecurityConfig에서만 관리합니다.
    // 나중에 정적 리소스나 포맷터, 인터셉터 등을 추가하고 싶으면
    // 여기 WebMvcConfigurer 메서드를 자유롭게 확장해도 됩니다.

}
