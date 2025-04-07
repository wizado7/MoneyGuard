# MoneyGuard - Приложение для управления личными финансами

MoneyGuard - это приложение для управления личными финансами, которое позволяет пользователям отслеживать доходы и расходы, устанавливать бюджеты, создавать финансовые цели и получать аналитику по своим финансам.

## Функциональность

- Регистрация и аутентификация пользователей
- Управление профилем пользователя
- Доступ к AI-рекомендациям (опционально)
- Создание и управление категориями доходов и расходов
- Учет транзакций (доходы и расходы)
- Установка лимитов расходов по категориям
- Создание и отслеживание финансовых целей
- Аналитика и отчеты по финансам

## Технологии

- Java 17
- Spring Boot 3.x
- Spring Security с JWT аутентификацией
- Spring Data JPA
- PostgreSQL
- Docker и Docker Compose
- Swagger/OpenAPI для документации API

## Запуск приложения

### Требования

- Docker и Docker Compose
- JDK 17 (для локальной разработки)
- Maven (для локальной разработки)

### Запуск с помощью Docker Compose

1. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/wizado7/MoneyGuard.git
   cd moneyguard
   git checkout -b backend origin/backend
   ```

2. Запустите приложение с помощью Docker Compose (не забудьте .env):
   ```bash
   docker-compose up -d
   ```

3. Приложение будет доступно по адресу:
   ```
   http://localhost:8080
   ```

4. Swagger UI будет доступен по адресу:
   ```
   http://localhost:8080/v1/swagger-ui/index.html#/
   ```

### Локальный запуск для разработки

1. Клонируйте репозиторий:
   ```bash
   git clone https://github.com/wizado7/MoneyGuard.git
   cd moneyguard
   git checkout -b backend origin/backend
   ```

2. Запустите PostgreSQL с помощью Docker:
   ```bash
   docker-compose up -d db
   ```

3. Запустите приложение с помощью Maven:
   ```bash
   ./mvnw spring-boot:run
   ```

## Структура проекта

- `src/main/java/src/main/controller` - REST контроллеры
- `src/main/java/src/main/service` - Бизнес-логика
- `src/main/java/src/main/repository` - Репозитории для работы с базой данных
- `src/main/java/src/main/model` - Модели данных
- `src/main/java/src/main/dto` - Data Transfer Objects
- `src/main/java/src/main/config` - Конфигурационные классы
- `src/main/java/src/main/security` - Классы для обеспечения безопасности
- `src/main/java/src/main/exception` - Обработка исключений
- `src/main/resources` - Ресурсы приложения
- `src/test` - Тесты

## Документация API

Полная документация API доступна через Swagger UI после запуска приложения:
```
http://localhost:8080/v1/swagger-ui/index.html#/
```

Также вы можете ознакомиться с документацией в файле [API.md](API.md) или [OpenAPI Documentation](https://redocly.github.io/redoc/?url=https://raw.githubusercontent.com/wizado7/MoneyGuard/backend/backend/src/main/resources/openapi/openapi.yaml).

## Тестирование

Для запуска тестов используйте:
```bash
./mvnw test
```


