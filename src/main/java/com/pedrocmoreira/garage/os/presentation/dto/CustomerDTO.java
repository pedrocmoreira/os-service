package com.pedrocmoreira.garage.os.presentation.dto;

import com.pedrocmoreira.garage.os.domain.model.Customer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class CustomerDTO {
    private CustomerDTO() {}
    public record Request(@NotBlank String document, @NotBlank String name, String phone, @Email String email) {}
    public record Response(Long id, String document, Customer.DocumentType documentType, String name, String phone, String email) {
        public static Response from(Customer c) {
            return new Response(c.getId(), c.getDocument(), c.getDocumentType(), c.getName(), c.getPhone(), c.getEmail());
        }
    }
}
