package com.pedrocmoreira.garage.os.bdd;

import io.cucumber.core.cli.Main;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Executa as especificações BDD (Gherkin em português) dentro do ciclo do JUnit,
 * gerando o relatório HTML em target/cucumber-report.html.
 */
class CucumberBddTest {

    @Test
    @DisplayName("BDD - fluxo completo da OS com Saga e compensações")
    void runFeatures() {
        byte exitStatus = Main.run(new String[]{
                "--glue", "com.pedrocmoreira.garage.os.bdd",
                "--plugin", "pretty",
                "--plugin", "html:target/cucumber-report.html",
                "--plugin", "json:target/cucumber-report.json",
                "classpath:features"
        }, Thread.currentThread().getContextClassLoader());
        assertThat(exitStatus).as("cenários BDD devem passar").isZero();
    }
}
