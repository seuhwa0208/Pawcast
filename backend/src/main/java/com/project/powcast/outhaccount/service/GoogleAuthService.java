package com.project.powcast.outhaccount.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleAuthService {

    @Value("{oauth.goolge.client.id}")
    private String clientId;

    // GoogleIdToken.Payload는 Google에서 제공하는 라이브러리에 포함된 클래스
    //https://developers.google.com/identity/gsi/web/guides/verify-google-id-token?utm_source=chatgpt.com&hl=ko
    public GoogleIdToken.Payload verify(String idToken) {

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                            .setAudience(Collections.singletonList(clientId))
                            .build();

            GoogleIdToken token = verifier.verify(idToken);

            if (token == null) {throw new BusinessException(ErrorCode.INVALID_TOKEN);}

            return token.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }
}
