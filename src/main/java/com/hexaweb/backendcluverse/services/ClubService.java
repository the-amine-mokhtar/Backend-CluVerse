package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.Membership;
import com.hexaweb.backendcluverse.enumerations.RoleType;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.dto.MembershipDto;
import com.hexaweb.backendcluverse.repositories.ClubRepository;
import com.hexaweb.backendcluverse.repositories.MembershipRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ClubService extends EntityServiceImpl<Club, Long> {
    
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final ClubRepository clubRepository;


    public Club save(Club club) {
        return clubRepository.save(club);
    }

    @Autowired
    public ClubService(ClubRepository repository, UserRepository userRepository, MembershipRepository membershipRepository) {
        super(repository);
        this.clubRepository = repository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    public MembershipDto addMember(Long clubId, Long userId, RoleType roleType) {
        Club club = clubRepository.findById(clubId).orElseThrow(() -> new RuntimeException("Club not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        
        Membership membership = new Membership();
        membership.setClub(club);
        membership.setUser(user);
        membership.setRole(roleType);
        membership.setJoinDate(LocalDate.now());
        membership.setActive(true);
        membership = membershipRepository.save(membership);
        
        MembershipDto dto = new MembershipDto();
        dto.setId(membership.getId());
        dto.setClubId(club.getId());
        dto.setClubName(club.getName());
        dto.setRole(membership.getRole().name());
        dto.setJoinDate(membership.getJoinDate());
        dto.setActive(membership.isActive());
        return dto;
    }
}

