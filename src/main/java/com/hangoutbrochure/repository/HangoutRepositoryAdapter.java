package com.hangoutbrochure.repository;

import com.hangoutbrochure.domain.Hangout;
import com.hangoutbrochure.domain.HangoutCategory;
import com.hangoutbrochure.domain.HangoutCategoryListCriteria;
import com.hangoutbrochure.domain.HangoutListCriteria;
import com.hangoutbrochure.repository.jpa.HangoutJpaRepository;
import com.hangoutbrochure.repository.jpa.HangoutCategoryJpaRepository;
import com.hangoutbrochure.repository.jpa.entity.HangoutEntity;
import com.hangoutbrochure.repository.jpa.entity.HangoutCategoryEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.UUID;

@Repository
public class HangoutRepositoryAdapter implements HangoutRepository {

    private final HangoutJpaRepository hangoutJpaRepository;
    private final HangoutCategoryJpaRepository hangoutCategoryJpaRepository;

    public HangoutRepositoryAdapter(
            HangoutJpaRepository hangoutJpaRepository,
            HangoutCategoryJpaRepository hangoutCategoryJpaRepository
    ) {
        this.hangoutJpaRepository = hangoutJpaRepository;
        this.hangoutCategoryJpaRepository = hangoutCategoryJpaRepository;
    }

    @Override
    public List<Hangout> findHangouts(HangoutListCriteria criteria) {
        List<UUID> ids = criteria.categories().isEmpty()
                ? hangoutJpaRepository.findMatchingIds(
                        criteria.from(), criteria.to(), criteria.cursorId(), PageRequest.of(0, criteria.limit()))
                : hangoutJpaRepository.findMatchingIdsByCategories(
                        criteria.categories(), criteria.from(), criteria.to(), criteria.cursorId(),
                        PageRequest.of(0, criteria.limit()));
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<UUID, HangoutEntity> hangoutsById = hangoutJpaRepository.findWithCategoriesByIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(HangoutEntity::getId, hangout -> hangout));
        List<Hangout> hangouts = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            HangoutEntity entity = hangoutsById.get(id);
            if (entity != null) {
                hangouts.add(toHangout(entity));
            }
        }
        return hangouts;
    }

    @Override
    public List<HangoutCategory> findHangoutCategories(HangoutCategoryListCriteria criteria) {
        PageRequest pageRequest = PageRequest.of(0, criteria.pageSize());
        List<HangoutCategoryEntity> entities = criteria.keyword() == null
                ? hangoutCategoryJpaRepository.findCategories(criteria.cursor(), pageRequest)
                : hangoutCategoryJpaRepository.findCategoriesByKeyword(
                        criteria.keyword(), criteria.cursor(), pageRequest);

        return entities.stream()
                .map(HangoutRepositoryAdapter::toHangoutCategory)
                .toList();
    }

    private static HangoutCategory toHangoutCategory(HangoutCategoryEntity entity) {
        return new HangoutCategory(entity.getId(), entity.getCode(), entity.getName());
    }

    private static Hangout toHangout(HangoutEntity entity) {
        return new Hangout(
                entity.getId(),
                entity.getTitle(),
                entity.getCategories().stream().map(HangoutCategoryEntity::getCode).sorted().toList(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getEventTimeDesc(),
                entity.getLocationName(),
                entity.getAddress(),
                entity.getDistrict(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getOrganizer(),
                entity.getOrganizerUrl(),
                entity.getImageUrl(),
                entity.getDetailUrl(),
                entity.getIsFree(),
                entity.getFeeDesc(),
                entity.getContact(),
                entity.getTargetAudience(),
                entity.getProgramDesc(),
                entity.getPerformers(),
                entity.getEtcDesc()
        );
    }
}
