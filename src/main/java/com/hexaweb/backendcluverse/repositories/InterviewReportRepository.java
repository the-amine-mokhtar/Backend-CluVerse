package com.hexaweb.backendcluverse.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.hexaweb.backendcluverse.entities.election.InterviewReport;

public interface InterviewReportRepository extends JpaRepository<InterviewReport, Long> {
}
