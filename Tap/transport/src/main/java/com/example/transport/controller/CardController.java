package com.example.transport.controller;

import com.example.transport.entity.TransitCard;
import com.example.transport.dto.TapRequest;
import com.example.transport.dto.TopUpRequest;
import com.example.transport.dto.CardResponse;
import com.example.transport.entity.TransitCard;
import com.example.transport.service.CardService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
@RestController
@RequestMapping("/api/card")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/tap")
    public ResponseEntity<CardResponse> tap(@Valid @RequestBody TapRequest request, Principal principal) {
        Long userId = Long.valueOf(principal.getName());

        // Pass request.getCardType() here!
        TransitCard updated = cardService.deductFare(userId, request.getCardType(), request.getFare());

        return ResponseEntity.ok(new CardResponse(true, "Fare deducted", updated.getBalance()));
    }

    @PostMapping("/topup")
    public ResponseEntity<CardResponse> topUp(@Valid @RequestBody TopUpRequest request, Principal principal) {
        try {
            // The Subject from JWT is now our userId
            Long userId = Long.valueOf(principal.getName());

            // You MUST pass the cardType from the request body here
            TransitCard updated = cardService.topUp(userId, request.getCardType(), request.getAmount());

            return ResponseEntity.ok(new CardResponse(true, "Top-up successful", updated.getBalance()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new CardResponse(false, e.getMessage(), null));
        }
    }
}