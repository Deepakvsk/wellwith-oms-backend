package com.wellwithlife.omsbackend.repository;
import com.wellwithlife.omsbackend.entity.SkuMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface SkuMasterRepository extends JpaRepository<SkuMaster, Long> {


    List<SkuMaster> findByIsActiveTrueOrderByProductNameAsc();
}
