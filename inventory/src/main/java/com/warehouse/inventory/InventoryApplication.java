package com.warehouse.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class InventoryApplication {

	public static void main(String[] args) {

		System.out.println(new BCryptPasswordEncoder(12).encode("password"));

		SpringApplication.run(InventoryApplication.class, args);
	}
}