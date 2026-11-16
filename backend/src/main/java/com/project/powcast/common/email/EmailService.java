package com.project.powcast.common.email;

import com.project.powcast.common.exception.EmailSendException;
import com.project.powcast.common.exception.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String body){

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body.replace("\n", "<br/>"), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new EmailSendException(ErrorCode.EMAIL_SEND_ERROR);
        }
    }
}
