package com.achyut.operation.customer;

import com.achyut.operation.api.ApiModels.CustomerRequest;
import com.achyut.operation.api.ApiModels.CustomerView;
import com.achyut.operation.api.OperationsMapper;
import com.achyut.operation.service.usecase.CustomerOperations;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService implements CustomerOperations {
    private final CustomerRepository repository;
    private final OperationsMapper mapper;

    @Override
    public CustomerView create(CustomerRequest r) {
        Customer customer = Customer.builder().name(r.name()).companyName(r.companyName()).phone(r.phone())
            .email(r.email()).billingAddress(r.billingAddress()).build();
        return mapper.customer(repository.save(customer));
    }

    @Override
    public List<CustomerView> list() {
        return repository.findAll().stream().map(mapper::customer).toList();
    }
}
