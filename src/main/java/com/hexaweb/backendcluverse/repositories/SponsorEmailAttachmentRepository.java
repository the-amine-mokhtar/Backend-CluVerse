package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.sponsoring.SponsorEmailAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SponsorEmailAttachmentRepository extends JpaRepository<SponsorEmailAttachment, Long> {
}
