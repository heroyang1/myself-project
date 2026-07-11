package com.backstage.system.service.internalmember.support;

import com.backstage.system.enums.internalmember.InternalMemberProgressStageEnum;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InternalMemberProgressStageResolverTest {

    @Test
    public void shouldResolveSeparatedWhenNoCareerSignalExists() {
        InternalMemberProgressStageEnum stage = InternalMemberProgressStageResolver.resolve(
                0, 0, 0, 0, 0, 0, null, null, null
        );

        assertEquals(InternalMemberProgressStageEnum.SEPARATED, stage);
    }

    @Test
    public void shouldResolvePreparingWhenOnlyLookingStatusExists() {
        InternalMemberProgressStageEnum stage = InternalMemberProgressStageResolver.resolve(
                1, 0, 0, 0, 0, 0, null, null, null
        );

        assertEquals(InternalMemberProgressStageEnum.PREPARING, stage);
    }

    @Test
    public void shouldResolveDeliveringWhenResumeWasDelivered() {
        InternalMemberProgressStageEnum stage = InternalMemberProgressStageResolver.resolve(
                1, 3, 0, 0, 0, 0, null, null, null
        );

        assertEquals(InternalMemberProgressStageEnum.DELIVERING, stage);
    }

    @Test
    public void shouldResolveInterviewingWhenInterviewSignalExists() {
        InternalMemberProgressStageEnum stage = InternalMemberProgressStageResolver.resolve(
                1, 6, 1, 1, 0, 2, "pending_interview", "pending", null
        );

        assertEquals(InternalMemberProgressStageEnum.INTERVIEWING, stage);
    }

    @Test
    public void shouldResolveOfferPendingWhenOfferWasSent() {
        InternalMemberProgressStageEnum stage = InternalMemberProgressStageResolver.resolve(
                1, 8, 2, 0, 1, 3, "finished", "offer_sent", null
        );

        assertEquals(InternalMemberProgressStageEnum.OFFER_PENDING, stage);
    }

    @Test
    public void shouldResolveOnboardedWhenEntryTimeExists() {
        InternalMemberProgressStageEnum stage = InternalMemberProgressStageResolver.resolve(
                1, 8, 2, 0, 1, 3, "finished", "offer_sent", "2026-07-05 12:00:00"
        );

        assertEquals(InternalMemberProgressStageEnum.ONBOARDED, stage);
    }
}
