package com.hexaweb.backendcluverse.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.hexaweb.backendcluverse.entities.sponsoring.Sponsorship;
import com.hexaweb.backendcluverse.enumerations.SponsorshipStatus;
import com.hexaweb.backendcluverse.enumerations.TransactionType;
import com.hexaweb.backendcluverse.repositories.SponsorshipRepository;
import com.hexaweb.backendcluverse.entities.finance.Transaction;
import com.hexaweb.backendcluverse.repositories.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService extends EntityServiceImpl<Transaction, Long> {
    private final TransactionRepository transactionRepository;
    private final SponsorshipRepository sponsorshipRepository;

    public TransactionService(TransactionRepository transactionRepository, SponsorshipRepository sponsorshipRepository) {
        super(transactionRepository);
        this.transactionRepository = transactionRepository;
        this.sponsorshipRepository = sponsorshipRepository;
    }

    @Override
    @Transactional
    public Transaction save(Transaction transaction) {
        hydrateAndValidateSponsorshipLink(transaction);
        Transaction saved = transactionRepository.save(transaction);
        applySponsorshipPaymentTransition(saved);
        return saved;
    }

    private void hydrateAndValidateSponsorshipLink(Transaction transaction) {
        if (transaction.getSponsorship() == null || transaction.getSponsorship().getId() == null) {
            return;
        }

        Sponsorship sponsorship = sponsorshipRepository.findById(transaction.getSponsorship().getId())
                .orElseThrow(() -> new RuntimeException("Sponsorship not found for linked transaction"));

        transaction.setSponsorship(sponsorship);

        if (sponsorship.getSponsor() != null) {
            if (transaction.getSponsor() != null
                    && transaction.getSponsor().getId() != null
                    && !transaction.getSponsor().getId().equals(sponsorship.getSponsor().getId())) {
                throw new RuntimeException("Transaction sponsor does not match linked sponsorship sponsor");
            }
            transaction.setSponsor(sponsorship.getSponsor());
        }

        if (sponsorship.getClub() != null) {
            if (transaction.getClub() != null
                    && transaction.getClub().getId() != null
                    && !transaction.getClub().getId().equals(sponsorship.getClub().getId())) {
                throw new RuntimeException("Transaction club does not match linked sponsorship club");
            }
            transaction.setClub(sponsorship.getClub());
        }
    }

    private void applySponsorshipPaymentTransition(Transaction transaction) {
        Sponsorship sponsorship = transaction.getSponsorship();
        if (sponsorship == null) {
            return;
        }

        if (transaction.getType() != TransactionType.INCOME) {
            return;
        }

        if (sponsorship.getStatus() != SponsorshipStatus.SIGNED) {
            return;
        }

        BigDecimal currentPaid = sponsorship.getPaidAmount() == null ? BigDecimal.ZERO : sponsorship.getPaidAmount();
        BigDecimal transactionAmount = BigDecimal.valueOf(Math.max(transaction.getAmount(), 0));

        sponsorship.setPaidAmount(currentPaid.add(transactionAmount));
        sponsorship.setStatus(SponsorshipStatus.PAID);
        if (sponsorship.getPaidAt() == null) {
            sponsorship.setPaidAt(LocalDateTime.now());
        }

        sponsorshipRepository.save(sponsorship);
    }
}

