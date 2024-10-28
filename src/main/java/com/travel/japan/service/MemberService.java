package com.travel.japan.service;

import com.travel.japan.dto.MemberProfileUpdateDto;
import com.travel.japan.dto.MemberSignInDto;
import com.travel.japan.dto.MemberSignUpDto;
import com.travel.japan.entity.Member;
import com.travel.japan.jwt.JwtUtil;
import com.travel.japan.jwt.TokenInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public interface MemberService {
    public Long signup(MemberSignUpDto memberSignUpDto) throws Exception;

    public TokenInfo signIn(MemberSignInDto requestDto) throws Exception;

    public Member findByEmail(String email) ;
    public void updateProfileImage(String email, String imageUrl);

    public void updateProfile(String email, MemberProfileUpdateDto profileDto);

}


