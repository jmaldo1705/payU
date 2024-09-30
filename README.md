# Plataforma de Pago - Guía de Ejecución y Configuración

Este proyecto es una plataforma de pago básica desarrollada en Java 17 utilizando Spring Boot y Gradle. La aplicación permite procesar pagos y reembolsos, integrándose con servicios externos simulados para antifraude y procesamiento bancario.

## 1. Descripción del Proyecto

La **Plataforma de Pago** es una aplicación web que permite procesar transacciones de pago y reembolsos. Utiliza una base de datos en memoria H2 y simula interacciones con servicios externos de antifraude y bancarios.

---

## 2. Prerrequisitos

Asegúrate de tener instalados los siguientes componentes:

- **Java Development Kit (JDK) 17**  
  Verifica la versión instalada:

  ```bash
  java -version
  java version "17.0.x"
Clona el repositorio del proyecto en tu máquina local:
  git clone https://github.com/jmaldo1705/payU.git
)

La estructura básica del proyecto es la siguiente:
  
payment-app/
├── build.gradle
├── gradle/
├── gradlew
├── gradlew.bat
├── README.md
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── example/
    │   │           └── paymentapp/
    │   │               ├── PaymentAppApplication.java
    │   │               ├── advice/
    │   │               ├── config/
    │   │               ├── controller/
    │   │               ├── exception/
    │   │               ├── mock/
    │   │               ├── model/
    │   │               ├── repository/
    │   │               └── service/
    │   └── resources/
    │       ├── application.properties
    │       └── static/
    └── test/
        └── java/


Utiliza Gradle Wrapper para construir y ejecutar la aplicación.

./gradlew clean build


Uso de la Aplicación

Procesar un Pago

Endpoint:

URL: http://localhost:8080/api/payments
Método: POST
Headers:
Content-Type: application/json
Cuerpo de la Solicitud:

json:

{
  "cardNumber": "4111111111111111",
  "cardHolderName": "Juan Pérez",
  "amount": 500.00,
  "currency": "USD",
  "expirationDate": "2025-12",
  "cvv": "123"
}

Reembolso

Endpoint:

URL: http://localhost:8080/api/payments/refunds
Método: POST
Headers:
Content-Type: application/json
Cuerpo de la Solicitud:

json:

{
  "originalTransactionId": 1,
  "amount": 500.00
}

  
