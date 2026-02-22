package com.fintrack.backend.repository;

import com.fintrack.backend.entity.MerchantCategoryMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantCategoryMapRepository extends JpaRepository<MerchantCategoryMap, Long> {

    Optional<MerchantCategoryMap> findByKeywordIgnoreCase(String keyword);

    @Query("SELECT m FROM MerchantCategoryMap m WHERE LOWER(:merchantName) LIKE LOWER(CONCAT('%', m.keyword, '%'))")
    List<MerchantCategoryMap> findMatchingKeywords(@Param("merchantName") String merchantName);

    boolean existsByKeywordIgnoreCase(String keyword);
}
