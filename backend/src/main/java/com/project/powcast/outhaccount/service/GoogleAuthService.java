package com.project.powcast.outhaccount.service;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthService {

    @Value("{oauth.goolge.client.id}")
    private String clientId;
}
