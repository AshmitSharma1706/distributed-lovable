package com.project.distributed_lovable.account_service.mapper;

import com.project.distributed_lovable.account_service.dto.subscription.SubscriptionResponse;
import com.project.distributed_lovable.account_service.entity.Plan;
import com.project.distributed_lovable.account_service.entity.Subscription;
import com.project.distributed_lovable.common_lib.dto.PlanDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "Spring")
public interface SubscriptionMapper {

    SubscriptionResponse toSubscriptionResponse(Subscription subscription);

    PlanDto toPlanResponse(Plan plan);
}
