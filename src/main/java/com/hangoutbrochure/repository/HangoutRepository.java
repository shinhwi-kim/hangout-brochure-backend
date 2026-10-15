package com.hangoutbrochure.repository;

import com.hangoutbrochure.domain.Hangout;
import com.hangoutbrochure.domain.HangoutCategory;
import com.hangoutbrochure.domain.HangoutCategoryListCriteria;
import com.hangoutbrochure.domain.HangoutListCriteria;

import java.util.List;

public interface HangoutRepository {
    List<Hangout> findHangouts(HangoutListCriteria criteria);

    List<HangoutCategory> findHangoutCategories(HangoutCategoryListCriteria criteria);
}
