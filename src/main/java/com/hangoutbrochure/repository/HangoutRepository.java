package com.hangoutbrochure.repository;

import com.hangoutbrochure.domain.Hangout;
import com.hangoutbrochure.domain.HangoutListCriteria;

import java.util.List;

public interface HangoutRepository {
    List<Hangout> findHangouts(HangoutListCriteria criteria);
}
