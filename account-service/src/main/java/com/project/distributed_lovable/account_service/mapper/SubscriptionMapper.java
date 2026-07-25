package com.project.distributed_lovable.account_service.mapper;

import com.project.distributed_lovable.account_service.dto.subscription.PlanResponse;
import com.project.distributed_lovable.account_service.dto.subscription.SubscriptionResponse;
import com.project.distributed_lovable.account_service.entity.Plan;
import com.project.distributed_lovable.account_service.entity.Subscription;
import org.mapstruct.Mapper;

@Mapper(componentModel = "Spring")
public interface SubscriptionMapper {

    SubscriptionResponse toSubscriptionResponse(Subscription subscription);

    PlanResponse toPlanResponse(Plan plan);
}
