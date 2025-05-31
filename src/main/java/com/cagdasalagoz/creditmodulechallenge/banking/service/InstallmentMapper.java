package com.cagdasalagoz.creditmodulechallenge.banking.service;

import com.cagdasalagoz.creditmodulechallenge.banking.dto.LoanInstallment;
import com.cagdasalagoz.creditmodulechallenge.banking.model.response.InstallmentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper(componentModel = "spring")
public interface InstallmentMapper {
    InstallmentMapper INSTANCE = Mappers.getMapper(InstallmentMapper.class);

    @Mapping(source = "loan.id", target = "loanId")
    InstallmentResponse toDto(LoanInstallment installment);

    List<InstallmentResponse> toDtoList(List<LoanInstallment> installments);
}