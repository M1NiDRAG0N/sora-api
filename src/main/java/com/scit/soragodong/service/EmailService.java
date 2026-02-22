package com.scit.soragodong.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.scit.soragodong.config.EmailProperties;
import com.scit.soragodong.domain.dto.EmailRequest;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@AllArgsConstructor
@Service
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;
    
    /**
     * 단순 텍스트 이메일 발송 (비동기)
     */
    @Async
    public void sendSimpleEmail(EmailRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailProperties.getFrom());
            message.setTo(request.to());
            message.setSubject(request.subject());
            message.setText(request.content());
            
            mailSender.send(message);
            log.info("Simple email sent to: {}", request.to());
        } catch (Exception e) {
            log.error("Failed to send simple email to: {}", request.to(), e);
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }
    
    /**
     * HTML 이메일 발송 (비동기)
     */
    @Async
    public void sendHtmlEmail(EmailRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(emailProperties.getFrom());
            helper.setTo(request.to());
            helper.setSubject(request.subject());
            helper.setText(request.content(), true);
            
            mailSender.send(message);
            log.info("HTML email sent to: {}", request.to());
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to: {}", request.to(), e);
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }
    
    /**
     * 일반 이메일 발송 (타입 자동 판단, 비동기)
     */
    @Async
    public void sendEmail(EmailRequest request) {
        if (request.isHtml()) {
            sendHtmlEmail(request);
        } else {
            sendSimpleEmail(request);
        }
    }
    
    /**
     * 인증 이메일 발송 (비동기)
     */
    @Async
    public void sendAuthenticationEmail(String email, String verificationCode) {
        String htmlContent = String.format(
            "<!DOCTYPE html>" +
            "<html lang='ko'>" +
            "<head>" +
            "    <meta charset='UTF-8'>" +
            "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "</head>" +
            "<body style='margin: 0; padding: 0; font-family: \"Segoe UI\", Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, ##667eea 0%%, ##764ba2 100%%);'>" +
            "    <div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
            "        <div style='background: white; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); overflow: hidden;'>" +
            "            <div style='background: linear-gradient(135deg, ##667eea 0%%, ##764ba2 100%%); padding: 30px; text-align: center; color: white;'>" +
            "                <h1 style='margin: 0; font-size: 28px; font-weight: 600;'>SORA</h1>" +
            "                <p style='margin: 10px 0 0 0; font-size: 14px; opacity: 0.9;'>이메일 인증</p>" +
            "            </div>" +
            "            <div style='padding: 40px 30px;'>" +
            "                <h2 style='margin: 0 0 20px 0; color: ##333; font-size: 22px;'>이메일 인증 코드</h2>" +
            "                <p style='margin: 0 0 30px 0; color: ##666; line-height: 1.6; font-size: 14px;'>안녕하세요,<br/>아래 인증 코드를 입력하여 이메일을 인증해주세요.</p>" +
            "                <div style='background: ##f8f9fa; border: 2px solid ##667eea; border-radius: 8px; padding: 25px; text-align: center; margin: 30px 0;'>" +
            "                    <p style='margin: 0; font-size: 12px; color: ##999;'>인증 코드</p>" +
            "                    <h1 style='margin: 10px 0 0 0; font-size: 42px; font-weight: 700; color: ##667eea; letter-spacing: 5px;'>%s</h1>" +
            "                </div>" +
            "                <p style='margin: 0; color: ##999; font-size: 12px; line-height: 1.6;'>⏱️ 이 코드는 <strong>10분</strong> 동안 유효합니다.<br/>코드를 공유하지 마세요.</p>" +
            "            </div>" +
            "            <div style='background: ##f8f9fa; padding: 20px 30px; text-align: center; border-top: 1px solid ##eee;'>" +
            "                <p style='margin: 0; color: ##999; font-size: 12px;'>© 2024 SORA. All rights reserved.</p>" +
            "            </div>" +
            "        </div>" +
            "    </div>" +
            "</body>" +
            "</html>",
            verificationCode
        );
        
        EmailRequest request = new EmailRequest(email, "[SORA] 이메일 인증 코드", htmlContent);
        sendHtmlEmail(request);
    }
    
    /**
     * 비밀번호 재설정 이메일 발송 (비동기)
     */
    @Async
    public void sendPasswordResetEmail(String email, String resetLink) {
        String htmlContent = String.format(
            "<!DOCTYPE html>" +
            "<html lang='ko'>" +
            "<head>" +
            "    <meta charset='UTF-8'>" +
            "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "</head>" +
            "<body style='margin: 0; padding: 0; font-family: \"Segoe UI\", Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, ##667eea 0%%, ##764ba2 100%%);'>" +
            "    <div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
            "        <div style='background: white; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); overflow: hidden;'>" +
            "            <div style='background: linear-gradient(135deg, ##667eea 0%%, ##764ba2 100%%); padding: 30px; text-align: center; color: white;'>" +
            "                <h1 style='margin: 0; font-size: 28px; font-weight: 600;'>SORA</h1>" +
            "                <p style='margin: 10px 0 0 0; font-size: 14px; opacity: 0.9;'>비밀번호 재설정</p>" +
            "            </div>" +
            "            <div style='padding: 40px 30px;'>" +
            "                <h2 style='margin: 0 0 20px 0; color: ##333; font-size: 22px;'>비밀번호를 재설정하세요</h2>" +
            "                <p style='margin: 0 0 30px 0; color: ##666; line-height: 1.6; font-size: 14px;'>안녕하세요,<br/>비밀번호를 재설정하려면 아래 버튼을 클릭하세요.</p>" +
            "                <div style='text-align: center; margin: 40px 0;'>" +
            "                    <a href='%s' style='background: linear-gradient(135deg, ##667eea 0%%, ##764ba2 100%%); color: white; padding: 14px 40px; text-decoration: none; border-radius: 8px; display: inline-block; font-weight: 600; font-size: 16px; transition: transform 0.2s;'>비밀번호 재설정</a>" +
            "                </div>" +
            "                <p style='margin: 0; color: ##999; font-size: 12px; line-height: 1.6;'>🔒 이 링크는 <strong>1시간</strong> 동안 유효합니다.<br/>링크를 공유하지 마세요. 본인이 요청하지 않았다면 무시하세요.</p>" +
            "            </div>" +
            "            <div style='background: ##f8f9fa; padding: 20px 30px; text-align: center; border-top: 1px solid ##eee;'>" +
            "                <p style='margin: 0; color: ##999; font-size: 12px;'>© 2024 SORA. All rights reserved.</p>" +
            "            </div>" +
            "        </div>" +
            "    </div>" +
            "</body>" +
            "</html>",
            resetLink
        );
        
        EmailRequest request = new EmailRequest(email, "[SORA] 비밀번호 재설정", htmlContent);
        sendHtmlEmail(request);
    }
    
    /**
     * 환영 이메일 발송 (비동기)
     */
    @Async
    public void sendWelcomeEmail(String email, String userName) {
        String htmlContent = String.format(
            "<!DOCTYPE html>" +
            "<html lang='ko'>" +
            "<head>" +
            "    <meta charset='UTF-8'>" +
            "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "</head>" +
            "<body style='margin: 0; padding: 0; font-family: \"Segoe UI\", Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, ##667eea 0%%, ##764ba2 100%%);'>" +
            "    <div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
            "        <div style='background: white; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); overflow: hidden;'>" +
            "            <div style='background: linear-gradient(135deg, ##667eea 0%%, ##764ba2 100%%); padding: 50px 30px; text-align: center; color: white;'>" +
            "                <h1 style='margin: 0; font-size: 36px; font-weight: 700;'>🎉</h1>" +
            "                <h2 style='margin: 15px 0 0 0; font-size: 28px; font-weight: 600;'>환영합니다!</h2>" +
            "                <p style='margin: 10px 0 0 0; font-size: 16px; opacity: 0.95;'>SORA 커뮤니티에 가입해주셨습니다</p>" +
            "            </div>" +
            "            <div style='padding: 40px 30px;'>" +
            "                <h3 style='margin: 0 0 15px 0; color: ##333; font-size: 20px;'>안녕하세요, %s님!</h3>" +
            "                <div style='background: ##f8f9fa; border-left: 4px solid ##667eea; padding: 20px; border-radius: 6px; margin: 25px 0;'>" +
            "                    <p style='margin: 0; color: ##555; line-height: 1.8; font-size: 14px;'>" +
            "                        SORA 커뮤니티에 가입해주셔서 감사합니다!<br/>" +
            "                        <br/>" +
            "                        이제 다양한 커뮤니티 활동을 시작할 수 있습니다:<br/>" +
            "                        ✓ 지역 사람들과 연결<br/>" +
            "                        ✓ 관심사 공유<br/>" +
            "                        ✓ 타임세일 참여<br/>" +
            "                        ✓ 금융 커뮤니티 활동" +
            "                    </p>" +
            "                </div>" +
            "                <div style='text-align: center; margin: 35px 0;'>" +
            "                    <a href='https://soragodong.com' style='background: linear-gradient(135deg, ##667eea 0%%, ##764ba2 100%%); color: white; padding: 12px 35px; text-decoration: none; border-radius: 8px; display: inline-block; font-weight: 600; font-size: 15px;'>지금 시작하기</a>" +
            "                </div>" +
            "                <p style='margin: 0; color: ##999; font-size: 13px; line-height: 1.6; text-align: center;'>문제나 질문이 있으신가요?<br/>support@soragodong.com으로 연락주세요.</p>" +
            "            </div>" +
            "            <div style='background: ##f8f9fa; padding: 25px 30px; text-align: center; border-top: 1px solid ##eee;'>" +
            "                <div style='margin-bottom: 15px;'>" +
            "                    <a href='#' style='color: ##667eea; text-decoration: none; margin: 0 10px; font-size: 12px;'>프로필 수정</a>" +
            "                    <span style='color: ##ddd;'>|</span>" +
            "                    <a href='#' style='color: ##667eea; text-decoration: none; margin: 0 10px; font-size: 12px;'>설정</a>" +
            "                    <span style='color: ##ddd;'>|</span>" +
            "                    <a href='#' style='color: ##667eea; text-decoration: none; margin: 0 10px; font-size: 12px;'>도움말</a>" +
            "                </div>" +
            "                <p style='margin: 15px 0 0 0; color: ##999; font-size: 11px;'>© 2024 SORA. 모든 권리 보유</p>" +
            "            </div>" +
            "        </div>" +
            "    </div>" +
            "</body>" +
            "</html>",
            userName
        );
        
        EmailRequest request = new EmailRequest(email, "[SORA] 환영합니다! 🎉", htmlContent);
        sendHtmlEmail(request);
    }

    /**
     * 이메일 인증 코드 발송 (비동기)
     */
    @Async
    public void sendVerificationEmail(String email, String verificationCode) {
        String htmlContent = String.format(
            "<!DOCTYPE html>" +
            "<html lang='ko'>" +
            "<head>" +
            "    <meta charset='UTF-8'>" +
            "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "</head>" +
            "<body style='margin: 0; padding: 0; font-family: \"Segoe UI\", Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, ##667eea 0%%, ##764ba2 100%%);'>" +
            "    <div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
            "        <div style='background: white; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); overflow: hidden;'>" +
            "            <div style='background: linear-gradient(135deg, ##667eea 0%%, ##764ba2 100%%); padding: 50px 30px; text-align: center; color: white;'>" +
            "                <h1 style='margin: 0; font-size: 36px; font-weight: 700;'>🔐</h1>" +
            "                <p style='margin: 10px 0 0 0; font-size: 24px; font-weight: 600;'>이메일 인증</p>" +
            "            </div>" +
            "            <div style='padding: 40px 30px;'>" +
            "                <h2 style='margin: 0 0 20px 0; color: ##333; font-size: 20px;'>인증 코드</h2>" +
            "                <p style='margin: 0 0 30px 0; color: ##666; line-height: 1.6; font-size: 14px;'>소라고동 가입을 위해 아래 인증 코드를 입력해주세요.</p>" +
            "                <div style='background: ##f8f9fa; border: 2px solid ##667eea; border-radius: 8px; padding: 25px; text-align: center; margin: 30px 0;'>" +
            "                    <p style='margin: 0 0 10px 0; font-size: 12px; color: ##999;'>인증 코드</p>" +
            "                    <h1 style='margin: 10px 0 0 0; font-size: 48px; font-weight: 700; color: ##667eea; letter-spacing: 5px;'>%s</h1>" +
            "                </div>" +
            "                <p style='margin: 0; color: ##999; font-size: 12px; line-height: 1.6;'>⏱️ 이 코드는 <strong>10분</strong> 동안 유효합니다.<br/>코드를 공유하지 마세요.</p>" +
            "            </div>" +
            "            <div style='background: ##f8f9fa; padding: 25px 30px; text-align: center; border-top: 1px solid ##eee;'>" +
            "                <p style='margin: 0; color: ##999; font-size: 12px;'>© 2024 SORA. 모든 권리 보유</p>" +
            "            </div>" +
            "        </div>" +
            "    </div>" +
            "</body>" +
            "</html>",
            verificationCode
        );
        
        EmailRequest request = new EmailRequest(email, "[SORA] 이메일 인증 코드", htmlContent);
        sendHtmlEmail(request);
    }
}
