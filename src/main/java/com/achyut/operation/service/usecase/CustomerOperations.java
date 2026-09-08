package com.achyut.operation.service.usecase;

import com.achyut.operation.api.ApiModels.CustomerRequest;
import com.achyut.operation.api.ApiModels.CustomerView;
import java.util.List;

public interface CustomerOperations {
    CustomerView create(CustomerRequest request);
    List<CustomerView> list();
}
