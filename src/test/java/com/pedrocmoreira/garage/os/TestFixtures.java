package com.pedrocmoreira.garage.os;

import com.pedrocmoreira.garage.os.domain.model.*;

import java.math.BigDecimal;

public final class TestFixtures {
    private TestFixtures() {}

    public static Customer customer() {
        return Customer.builder().id(1L).name("João Silva").document("52998224725")
                .documentType(Customer.DocumentType.CPF).email("joao@email.com").phone("11999999999").build();
    }

    public static Vehicle vehicle(Customer customer) {
        return Vehicle.builder().id(10L).plate("ABC1D23").make("Fiat").model("Uno").year(2020).color("Branco")
                .customer(customer).build();
    }

    public static CatalogService service() {
        return CatalogService.builder().id(1L).name("Troca de óleo").basePrice(new BigDecimal("150.00")).build();
    }

    public static CatalogPart part() {
        return CatalogPart.builder().id(2L).name("Óleo 5W30").unitPrice(new BigDecimal("38.00")).build();
    }

    public static ServiceOrder serviceOrder(StatusSO status) {
        Customer customer = customer();
        ServiceOrder so = ServiceOrder.builder().id(100L).number("OS-2026-00001")
                .customer(customer).vehicle(vehicle(customer)).status(status).observations("Barulho no motor").build();
        so.addServiceItem(ServiceItem.builder().service(service()).appliedPrice(new BigDecimal("150.00")).build());
        so.addPartItem(PartItem.builder().part(part()).quantity(4).unitPriceApplied(new BigDecimal("38.00")).build());
        return so;
    }
}
