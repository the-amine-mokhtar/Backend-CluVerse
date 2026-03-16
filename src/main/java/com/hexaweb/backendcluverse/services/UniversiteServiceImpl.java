package com.koussay.foyer.services;

import com.koussay.foyer.entities.Foyer;
import com.koussay.foyer.entities.Universite;
import com.koussay.foyer.repositories.UniversiteRepo;
import com.koussay.foyer.repositories.FoyerRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UniversiteServiceImpl implements IUniversiteService {

    private final UniversiteRepo universiteRepo;
    private final FoyerRepo foyerRepo;

    @Override
    public List<Universite> retrieveAllUniversites(){
        return universiteRepo.findAll();
    }

    @Override
    public Universite retrieveUniversite(Long universiteId){
        return universiteRepo.findById(universiteId).get();
    }

    @Override
    public Universite addUniversite(Universite universite){
        return universiteRepo.save(universite);
    }

    @Override
    public void removeUniversite(Long universiteId){
        universiteRepo.deleteById(universiteId);
    }

    @Override
    public Universite modifyUniversite(Universite universite){
        return universiteRepo.save(universite);
    }

    @Override
    public Universite addUniversiteWithFoyer(Universite universite){
        if(universite.getFoyer() != null){
            universite.getFoyer().setUniversite(universite);
        }
        return universiteRepo.save(universite);
    }

    @Override
    public Universite affectFoyerToUniversite(Long universiteId, Long foyerId){
        Universite universite = universiteRepo.findById(universiteId).get();
        Foyer foyer = foyerRepo.findById(foyerId).get();
        universite.setFoyer(foyer);
        return universiteRepo.save(universite);
    }

    @Override
    public Universite addUniversiteAndAssignFoyerToUniversite(Universite universite, Long foyerId){
        Foyer foyer = foyerRepo.findById(foyerId).get();
        universite.setFoyer(foyer);
        return universiteRepo.save(universite);
    }

    @Override
    public Universite desaffecterFoyerFromUniversite(Long universiteId){
        Universite universite = universiteRepo.findById(universiteId).get();
        universite.setFoyer(null);
        return universiteRepo.save(universite);
    }

}