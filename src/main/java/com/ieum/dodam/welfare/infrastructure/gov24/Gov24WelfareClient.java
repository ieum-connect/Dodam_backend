package com.ieum.dodam.welfare.infrastructure.gov24;

import com.ieum.dodam.welfare.infrastructure.gov24.dto.Gov24PageResponse;
import com.ieum.dodam.welfare.infrastructure.gov24.dto.Gov24ServiceDetailItem;
import com.ieum.dodam.welfare.infrastructure.gov24.dto.Gov24ServiceListItem;
import com.ieum.dodam.welfare.infrastructure.gov24.dto.Gov24SupportConditionsItem;

public interface Gov24WelfareClient {
    Gov24PageResponse<Gov24ServiceListItem> serviceList(int page, int perPage, String modifiedFrom);
    Gov24PageResponse<Gov24ServiceDetailItem> serviceDetail(String serviceId);
    Gov24PageResponse<Gov24SupportConditionsItem> supportConditions(int page, int perPage, String serviceId);
}
