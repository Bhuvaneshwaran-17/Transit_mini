package com.example.transitcard.controller;

import com.example.transitcard.model.TransitCard;
import com.example.transitcard.service.TransitCardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/transit")
@CrossOrigin(origins = "http://localhost:3002")
public class TransitCardController {

    private final TransitCardService service;

    public TransitCardController(TransitCardService service) {
        this.service = service;
    }

    @PostMapping("/buy")
    public ResponseEntity<?> buyCard(@RequestBody TransitCard card, Principal principal) {

        Long userId = Long.valueOf(principal.getName());

        Optional<TransitCard> result = service.buyCard(card, userId);

        if (result.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("You already own this type of card.");
        }

        return ResponseEntity.ok(result.get());
    }

    @GetMapping("/my-cards")
    public ResponseEntity<?> getMyCards(Principal principal) {

        Long userId = Long.valueOf(principal.getName());
        List<TransitCard> cards = service.getMyCards(userId);

        if (cards.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("You need to buy a card first.");
        }

        return ResponseEntity.ok(cards);
    }
}
