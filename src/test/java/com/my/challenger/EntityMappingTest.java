//package com.my.challenger;
//import org.hibernate.engine.spi.SessionFactoryImplementor;
//import org.hibernate.metamodel.internal.MetamodelImplementor;
//import org.hibernate.metamodel.spi.MetamodelImplementor;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.TestPropertySource;
//
//import jakarta.persistence.EntityManagerFactory;
//
//import static org.assertj.core.api.Assertions.assertThatNoException;
//
//@SpringBootTest
//@TestPropertySource(properties = {
//        "spring.datasource.url=jdbc:h2:mem:testdb",
//        "spring.jpa.hibernate.ddl-auto=none"
//})
//class EntityMappingTest {
//
//    @Autowired
//    private EntityManagerFactory entityManagerFactory;
//
//    @Test
//    void validateEntityMappings() {
//        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
//        org.hibernate.metamodel.spi.MetamodelImplementor metamodel = sessionFactory.getMetamodel();
//
//        // This will trigger the same validation that causes your error
//        assertThatNoException().isThrownBy(() -> {
//            metamodel.initialize();
//        });
//    }
//}