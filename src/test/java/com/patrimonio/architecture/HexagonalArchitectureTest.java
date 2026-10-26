package com.patrimonio.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reglas de arquitectura hexagonal verificadas con ArchUnit.
 *
 * <p>Se usa la API core de ArchUnit dentro de pruebas JUnit normales, sin el
 * motor {@code archunit-junit6}: ese motor depende de JUnit Platform 6.1.x,
 * mientras que el BOM de Spring Boot 4.1.2 administra JUnit 6.0.3. La API core
 * evita mezclar versiones de la plataforma y sigue soportando Java 25 (ArchUnit
 * 1.5.1 reconoce hasta class files de Java 27).</p>
 *
 * <p>Solo se importan las clases de producción; las de prueba quedan excluidas
 * con {@link ImportOption.DoNotIncludeTests}.</p>
 */
@Tag("architecture")
class HexagonalArchitectureTest {

    private static final JavaClasses CLASES = new ClassFileImporter()
        .withImportOption(new ImportOption.DoNotIncludeTests())
        .importPackages("com.patrimonio");

    @Test
    void elDominioSoloDependeDeJavaYDelPropioProyecto() {
        ArchRule regla = classes().that().resideInAPackage("..domain..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("java..", "com.patrimonio..")
            .because("el dominio debe ser Java puro: sin Spring, JPA, Hibernate, "
                + "Jackson ni Jakarta Validation");

        regla.check(CLASES);
    }

    @Test
    void laAplicacionNoDependeDeLosAdaptadores() {
        ArchRule regla = noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..adapter.in.web..", "..adapter.out.persistence..")
            .because("la aplicación define puertos y los adaptadores dependen de ella");

        regla.check(CLASES);
    }

    @Test
    void elAdaptadorWebNoDependeDePersistenciaNiDePuertosDeSalida() {
        ArchRule regla = noClasses().that().resideInAPackage("..adapter.in.web..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..adapter.out.persistence..", "..application.port.out..")
            .because("el controlador solo debe hablar con el puerto de entrada");

        regla.check(CLASES);
    }

    @Test
    void elAdaptadorDePersistenciaNoDependeDelAdaptadorWeb() {
        ArchRule regla = noClasses().that().resideInAPackage("..adapter.out.persistence..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.in.web..")
            .because("la persistencia no debe conocer la capa HTTP");

        regla.check(CLASES);
    }

    @Test
    void cuentasSoloUsaLaApiPublicaDeAcceso() {
        ArchRule regla = noClasses().that().resideInAPackage("com.patrimonio.cuentas..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "com.patrimonio.acceso.domain..",
                "com.patrimonio.acceso.application..",
                "com.patrimonio.acceso.adapter..")
            .because("entre módulos solo se puede importar el paquete api del otro módulo");

        regla.check(CLASES);
    }

    @Test
    void accesoNoDependeDeCuentas() {
        ArchRule regla = noClasses().that().resideInAPackage("com.patrimonio.acceso..")
            .should().dependOnClassesThat().resideInAPackage("com.patrimonio.cuentas..")
            .because("la dependencia entre acceso y cuentas es unidireccional");

        regla.check(CLASES);
    }

    @Test
    void laApiPublicaNoDependeDeAdaptadoresNiDeServicios() {
        ArchRule regla = noClasses().that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..adapter..", "..application.service..")
            .because("los contratos públicos no deben arrastrar implementaciones");

        regla.check(CLASES);
    }

    @Test
    void sharedNoDependeDeModulosDeNegocio() {
        ArchRule regla = noClasses().that().resideInAPackage("com.patrimonio.shared..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "com.patrimonio.acceso..", "com.patrimonio.cuentas..")
            .because("el shared kernel no puede depender de ningún módulo de negocio");

        regla.check(CLASES);
    }

    @Test
    void noHayCiclosEntreModulos() {
        ArchRule regla = slices().matching("com.patrimonio.(*)..")
            .should().beFreeOfCycles()
            .because("las dependencias entre módulos deben ser acíclicas");

        regla.check(CLASES);
    }

    @Test
    void losControladoresDependenDelPuertoDeEntrada() {
        ArchRule regla = classes().that().areAnnotatedWith(RestController.class)
            .should().dependOnClassesThat().resideInAPackage("..application.port.in..")
            .because("el adaptador de entrada invoca el caso de uso, no la persistencia");

        regla.check(CLASES);
    }

    @Test
    void losAdaptadoresDeSalidaDependenDeContratosInternos() {
        ArchRule regla = classes().that().haveSimpleNameEndingWith("Adapter")
            .and().resideInAPackage("..adapter.out.persistence..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..application.port.out..", "..api.port..")
            .because("la infraestructura implementa los puertos definidos hacia dentro");

        regla.check(CLASES);
    }
}
