package com.cagdasalagoz.creditmodulechallenge.banking.service;

import com.cagdasalagoz.creditmodulechallenge.banking.dto.Loan;
import com.cagdasalagoz.creditmodulechallenge.banking.model.response.CreateLoanResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper(componentModel = "spring", uses = InstallmentMapper.class)
public interface LoanMapper {
    LoanMapper INSTANCE = Mappers.getMapper(LoanMapper.class);

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.name", target = "customerName")
    @Mapping(source = "customer.surname", target = "customerSurname")
    @Mapping(source = "installments", target = "installments")
    CreateLoanResponse toDto(Loan loan);

    List<CreateLoanResponse> toDtoList(List<Loan> loans);
}