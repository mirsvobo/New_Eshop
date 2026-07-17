package org.example.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.example.model.TaxMode;

@Data
public class CheckoutFormDataDto {

    @NotBlank(message = "Jméno je povinné.")
    private String firstName;

    @NotBlank(message = "Příjmení je povinné.")
    private String lastName;

    @NotBlank(message = "E-mail je povinný.")
    @Email(message = "Neplatný formát e-mailu.")
    private String email;

    @NotBlank(message = "Telefon je povinný.")
    private String phone;

    @NotBlank(message = "Fakturační ulice je povinná.")
    private String billingStreet;
    @NotBlank(message = "Fakturační město je povinné.")
    private String billingCity;
    @NotBlank(message = "Fakturační PSČ je povinné.")
    @Pattern(regexp = "^\\d{3} ?\\d{2}$", message = "PSČ musí mít 5 číslic.")
    private String billingZipCode;

    private String companyName;

    @Pattern(regexp = "^$|^\\d{8}$", message = "IČO musí mít přesně 8 číslic.")
    private String ico;

    @Pattern(regexp = "^$|^CZ\\d{8,10}$", message = "DIČ musí začínat 'CZ' a následovat 8-10 číslic.")
    private String dic;

    private boolean shipToDifferentAddress;
    private String shippingFirstName;
    private String shippingLastName;
    private String shippingPhone;
    private String shippingStreet;
    private String shippingCity;
    private String shippingZipCode;

    private String couponCode;
    private TaxMode taxMode = TaxMode.STANDARD;
    private boolean affidavitSigned = false;

    public boolean isBusinessValidationOk() {
        boolean hasIco = ico != null && !ico.isBlank();
        boolean hasDic = dic != null && !dic.isBlank();
        return hasIco == hasDic;
    }
}