package de.starima.pfw.base;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StarimaBaseApplication {

    public static void main(String[] args) {
        /* Empty main function because only the applicationContext needs to get initialized for testcases */
        //SpringApplication.run(RcnBaseApplication.class, args);    // can be uncommented to validate the entities against the onprem-database
	}
}<?xml version="1.0" encoding="UTF-8"?>