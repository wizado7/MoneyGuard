# MoneyGuard API Documentation

**Base URL**: `/api/v1`

## Содержание
1. [Аутентификация](#аутентификация)
2. [Управление профилем](#управление-профилем)
3. [Управление подпиской](#управление-подпиской)
4. [Управление категориями](#управление-категориями)
5. [Управление транзакциями](#управление-транзакциями)
6. [Управление лимитами](#управление-лимитами)
7. [Управление целями](#управление-целями)
8. [Отчеты](#отчеты)
9. [AI функции](#ai-функции)
10. [VPN Proxy мониторинг](#vpn-proxy-мониторинг)

---

## Аутентификация

### Регистрация

**Endpoint**: `POST /api/v1/auth/register`

**Request**:
```json
{
  "email": "user@example.com",
  "password": "Password123",
  "name": "Имя пользователя"
}
```

**Response** (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "ai_access_enabled": false
}
```

**Errors**:
- `409 Conflict` - Пользователь уже существует
- `400 Bad Request` - Некорректные данные

### Вход в систему

**Endpoint**: `POST /api/v1/auth/login`

**Request**:
```json
{
  "email": "user@example.com",
  "password": "Password123"
}
```

**Response** (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "ai_access_enabled": false
}
```

**Errors**:
- `401 Unauthorized` - Неверные учетные данные

### Выход из системы

**Endpoint**: `POST /api/v1/auth/logout`

**Headers**:
- `Authorization: Bearer {token}`

**Response** (200 OK)

### Обновление токена

**Endpoint**: `POST /api/v1/auth/refresh`

**Request**:
```json
{
  "refreshToken": "refresh_token_here"
}
```

**Response** (200 OK):
```json
{
  "token": "new_jwt_token",
  "ai_access_enabled": true
}
```

---

## Управление профилем

### Получение профиля

**Endpoint**: `GET /api/v1/profile`

**Headers**:
- `Authorization: Bearer {token}`

**Response** (200 OK):
```json
{
  "id": "1",
  "email": "user@example.com",
  "name": "Имя пользователя",
  "profileImage": "https://example.com/image.jpg"
}
```

### Обновление профиля

**Endpoint**: `PUT /api/v1/profile`

**Headers**:
- `Authorization: Bearer {token}`

**Request**:
```json
{
  "name": "Новое имя",
  "profileImage": "https://example.com/new-image.jpg"
}
```

**Response** (200 OK):
```json
{
  "id": "1",
  "email": "user@example.com",
  "name": "Новое имя",
  "profileImage": "https://example.com/new-image.jpg"
}
```

---

## Управление подпиской

### Получение информации о подписке

**Endpoint**: `GET /api/v1/subscription`

**Headers**:
- `Authorization: Bearer {token}`

**Response** (200 OK):
```json
{
  "ai_access_enabled": false,
  "features": [
    "Базовый учет доходов и расходов",
    "Неограниченное количество транзакций"
  ]
}
```

### Активация PREMIUM подписки

**Endpoint**: `POST /api/v1/subscription`

**Headers**:
- `Authorization: Bearer {token}`

**Response** (200 OK):
```json
{
  "ai_access_enabled": true,
  "features": [
    "Базовый учет доходов и расходов",
    "Неограниченное количество транзакций",
    "Доступ к AI-рекомендациям",
    "Персональная аналитика"
  ]
}
```

### Деактивация PREMIUM подписки

**Endpoint**: `DELETE /api/v1/subscription`

**Headers**:
- `Authorization: Bearer {token}`

**Response** (204 No Content)

---

## Управление категориями

### Получение списка категорий

**Endpoint**: `GET /api/v1/categories`

**Headers**:
- `Authorization: Bearer {token}`

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "name": "Продукты",
    "icon": "shopping_cart"
  },
  {
    "id": 2,
    "name": "Транспорт",
    "icon": "directions_car"
  }
]
```

### Создание категории

**Endpoint**: `POST /api/v1/categories`

**Headers**:
- `Authorization: Bearer {token}`

**Request**:
```json
{
  "name": "Мои расходы",
  "icon": "account_balance_wallet"
}
```

**Response** (201 Created):
```json
{
  "id": 16,
  "name": "Мои расходы", 
  "icon": "account_balance_wallet"
}
```

### Обновление категории

**Endpoint**: `PUT /api/v1/categories/{id}`

**Headers**:
- `Authorization: Bearer {token}`

**Request**:
```json
{
  "name": "Обновленное название",
  "icon": "update"
}
```

**Response** (200 OK):
```json
{
  "id": 16,
  "name": "Обновленное название",
  "icon": "update"
}
```

### Удаление категории

**Endpoint**: `DELETE /api/v1/categories/{id}`

**Headers**:
- `Authorization: Bearer {token}`

**Response** (204 No Content)

---

## Управление транзакциями

### Получение списка транзакций

**Endpoint**: `GET /api/v1/transactions`

**Headers**:
- `Authorization: Bearer {token}`

**Query Parameters**:
- `dateFrom` (optional): Дата начала (YYYY-MM-DD)
- `dateTo` (optional): Дата окончания (YYYY-MM-DD)
- `categoryName` (optional): Название категории

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "amount": -1000,
    "category": {
      "id": 1,
      "name": "Продукты",
      "icon": "shopping_cart"
    },
    "date": "2023-04-01",
    "description": "Покупка продуктов в магазине",
    "created_at": "2023-04-01T12:00:00"
  }
]
```

### Получение транзакции по ID

**Endpoint**: `GET /api/v1/transactions/{id}`

**Headers**:
- `Authorization: Bearer {token}`

**Response** (200 OK):
```json
{
  "id": 1,
  "amount": -1000,
  "category": {
    "id": 1,
    "name": "Продукты",
    "icon": "shopping_cart"
  },
  "date": "2023-04-01",
  "description": "Покупка продуктов в магазине",
  "created_at": "2023-04-01T12:00:00"
}
```

### Создание транзакции

**Endpoint**: `POST /api/v1/transactions`

**Headers**:
- `Authorization: Bearer {token}`

**Request**:
```json
{
  "amount": -1000,
  "categoryId": 1,
  "date": "2023-04-01",
  "description": "Покупка продуктов в магазине"
}
```

**Response** (201 Created):
```json
{
  "transaction": {
    "id": 1,
    "amount": -1000,
    "category": {
      "id": 1,
      "name": "Продукты",
      "icon": "shopping_cart"
    },
    "date": "2023-04-01",
    "description": "Покупка продуктов в магазине",
    "created_at": "2023-04-01T12:00:00"
  },
  "ai_advice": "Рекомендуется планировать покупки заранее для оптимизации расходов на продукты."
}
```

### Обновление транзакции

**Endpoint**: `PUT /api/v1/transactions/{id}`

**Headers**:
- `Authorization: Bearer {token}`

**Request**:
```json
{
  "amount": -1200,
  "categoryId": 1,
  "date": "2023-04-01",
  "description": "Покупка продуктов в магазине (обновлено)"
}
```

**Response** (200 OK):
```json
{
  "id": 1,
  "amount": -1200,
  "category": {
    "id": 1,
    "name": "Продукты",
    "icon": "shopping_cart"
  },
  "date": "2023-04-01",
  "description": "Покупка продуктов в магазине (обновлено)",
  "created_at": "2023-04-01T12:00:00"
}
```

### Удаление транзакции

**Endpoint**: `DELETE /api/v1/transactions/{id}`

**Headers**:
- `Authorization: Bearer {token}`

**Response** (204 No Content)

---

## Управление лимитами

### Получение списка лимитов

**Endpoint**: `GET /api/v1/limits`

**Headers**:
- `Authorization: Bearer {token}`

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "category": {
      "id": 1,
      "name": "Продукты",
      "icon": "shopping_cart"
    },
    "amount": 15000,
    "period": "MONTHLY",
    "current_spending": 5000
  }
]
```

### Создание лимита

**Endpoint**: `POST /api/v1/limits`

**Headers**:
- `Authorization: Bearer {token}`

**Request**:
```json
{
  "categoryId": 1,
  "amount": 15000,
  "period": "MONTHLY"
}
```

**Response** (201 Created):
```json
{
  "id": 1,
  "category": {
    "id": 1,
    "name": "Продукты",
    "icon": "shopping_cart"
  },
  "amount": 15000,
  "period": "MONTHLY",
  "current_spending": 0
}
```

### Обновление лимита

**Endpoint**: `PUT /api/v1/limits/{id}`

**Headers**:
- `Authorization: Bearer {token}`

**Request**:
```json
{
  "categoryId": 1,
  "amount": 18000,
  "period": "MONTHLY"
}
```

**Response** (200 OK):
```json
{
  "id": 1,
  "category": {
    "id": 1,
    "name": "Продукты", 
    "icon": "shopping_cart"
  },
  "amount": 18000,
  "period": "MONTHLY",
  "current_spending": 5000
}
```

### Удаление лимита

**Endpoint**: `DELETE /api/v1/limits/{id}`

**Headers**:
- `Authorization: Bearer {token}`

**Response** (204 No Content)

---

## Управление целями

### Получение списка целей

**Endpoint**: `GET /api/v1/goals`

**Headers**:
- `Authorization: Bearer {token}`

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "name": "Новый автомобиль",
    "targetAmount": 1000000,
    "currentAmount": 150000,
    "targetDate": "2024-12-31",
    "priority": "HIGH"
  }
]
```

### Получение цели по ID

**Endpoint**: `GET /api/v1/goals/{id}`

**Headers**:
- `Authorization: Bearer {token}`

**Response** (200 OK):
```json
{
  "id": 1,
  "name": "Новый автомобиль",
  "targetAmount": 1000000,
  "currentAmount": 150000,
  "targetDate": "2024-12-31",
  "priority": "HIGH"
}
```

### Получение детальной информации о цели

**Endpoint**: `GET /api/v1/goals/{id}/details`

**Headers**:
- `Authorization: Bearer {token}`

**Response** (200 OK):
```json
{
  "goal": {
    "id": 1,
    "name": "Новый автомобиль",
    "targetAmount": 1000000,
    "currentAmount": 150000,
    "targetDate": "2024-12-31",
    "priority": "HIGH"
  },
  "progress": 15.0,
  "monthlyContribution": 50000,
  "daysLeft": 365
}
```

### Создание цели

**Endpoint**: `POST /api/v1/goals`

**Headers**:
- `Authorization: Bearer {token}`

**Request**:
```json
{
  "name": "Новый ноутбук",
  "description": "Накопить на новый ноутбук",
  "targetAmount": 150000,
  "targetDate": "2023-09-01",
  "priority": "MEDIUM"
}
```

**Response** (201 Created):
```json
{
  "id": 3,
  "name": "Новый ноутбук",
  "description": "Накопить на новый ноутбук",
  "targetAmount": 150000,
  "currentAmount": 0,
  "targetDate": "2023-09-01",
  "priority": "MEDIUM"
}
```

### Обновление цели

**Endpoint**: `PUT /api/v1/goals/{id}`

**Headers**:
- `Authorization: Bearer {token}`

**Request**:
```json
{
  "name": "Новый ноутбук MacBook",
  "description": "Накопить на MacBook Pro",
  "targetAmount": 180000,
  "targetDate": "2023-10-01",
  "priority": "HIGH"
}
```

**Response** (200 OK):
```json
{
  "goal": {
    "id": 3,
    "name": "Новый ноутбук MacBook",
    "description": "Накопить на MacBook Pro",
    "targetAmount": 180000,
    "currentAmount": 0,
    "targetDate": "2023-10-01",
    "priority": "HIGH"
  },
  "monthlyContribution": 30000,
  "daysLeft": 183
}
```

### Удаление цели

**Endpoint**: `DELETE /api/v1/goals/{id}`

**Headers**:
- `Authorization: Bearer {token}`

**Response** (204 No Content)

---

## Отчеты

### Получение отчета

**Endpoint**: `GET /api/v1/reports`

**Headers**:
- `Authorization: Bearer {token}`

**Query Parameters**:
- `date_from` (optional): Дата начала (YYYY-MM-DD)
- `date_to` (optional): Дата окончания (YYYY-MM-DD)  
- `type` (optional): Тип отчета (daily, weekly, monthly, yearly). По умолчанию: monthly

**Response** (200 OK):
```json
{
  "period": "2023-04-01 to 2023-04-30",
  "type": "monthly",
  "income": 50000,
  "expenses": 25000,
  "balance": 25000,
  "categories": {
    "Продукты": 10000,
    "Транспорт": 5000,
    "Развлечения": 8000,
    "Прочее": 2000
  }
}
```

**Errors**:
- `400 Bad Request` - Некорректные параметры (неверный тип отчета, начальная дата позже конечной)

---

## AI функции

### Получение аналитики с AI

**Endpoint**: `GET /ai/analysis`

**Headers**:
- `Authorization: Bearer {token}`

**Response** (200 OK):
```json
{
  "period": "2023-01-01 - 2023-04-01",
  "categories": {
    "Продукты": 30000,
    "Транспорт": 15000,
    "Развлечения": 24000
  },
  "anomalies": [
    "Расходы на развлечения выросли на 60% по сравнению с предыдущим периодом"
  ],
  "forecast": {
    "balance": 20000,
    "risk_level": "low"
  }
}
```

### AI чат (с файлом)

**Endpoint**: `POST /ai/chat`

**Headers**:
- `Authorization: Bearer {token}`
- `Content-Type: multipart/form-data`

**Request (multipart/form-data)**:
- `message`: "Как оптимизировать мои расходы на продукты?"
- `image` (optional): файл изображения

**Response** (200 OK):
```json
{
  "message": "На основе анализа ваших трат, вот рекомендации по оптимизации расходов на продукты...",
  "advice": [
    "Составляйте список покупок заранее",
    "Сравнивайте цены в разных магазинах",
    "Покупайте сезонные продукты"
  ],
  "actions": [
    "Установить лимит на категорию 'Продукты'",
    "Создать финансовую цель"
  ]
}
```

### AI чат (JSON)

**Endpoint**: `POST /ai/chat`

**Headers**:
- `Authorization: Bearer {token}`
- `Content-Type: application/json`

**Request**:
```json
{
  "message": "Как мне сэкономить на транспорте?"
}
```

**Response** (200 OK):
```json
{
  "message": "Вот персональные рекомендации по экономии на транспорте...",
  "advice": [
    "Используйте общественный транспорт",
    "Рассмотрите совместные поездки",
    "Планируйте маршруты эффективно"
  ],
  "actions": [
    "Установить лимиты на категории расходов",
    "Создать финансовую цель"
  ]
}
```

**Errors**:
- `403 Forbidden` - AI доступ отключен (нужна PREMIUM подписка)
- `400 Bad Request` - Пустое сообщение

---

## VPN Proxy мониторинг

### Получение статуса прокси

**Endpoint**: `GET /api/vpn-proxy/status`

**Response** (200 OK):
```json
{
  "success": true,
  "currentProxy": "http://51.81.245.3:17981",
  "totalProxies": 5,
  "healthyProxies": 4,
  "proxyStatuses": {
    "http://51.81.245.3:17981": true,
    "http://138.68.60.8:80": true,
    "http://98.191.238.177:80": false,
    "http://45.77.55.173:8080": true,
    "http://167.99.83.205:8080": true
  }
}
```

### Получение текущего прокси

**Endpoint**: `GET /api/vpn-proxy/current`

**Response** (200 OK):
```json
{
  "success": true,
  "currentProxy": "http://51.81.245.3:17981",
  "isHealthy": true
}
```

### Принудительное обновление статуса прокси

**Endpoint**: `POST /api/vpn-proxy/refresh`

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Proxy health check completed",
  "currentProxy": "http://138.68.60.8:80",
  "totalProxies": 5,
  "healthyProxies": 4
}
```

---

## Коды ошибок

- `200 OK` - Успешный запрос
- `201 Created` - Ресурс успешно создан
- `204 No Content` - Успешный запрос без содержимого
- `400 Bad Request` - Некорректный запрос
- `401 Unauthorized` - Требуется аутентификация
- `403 Forbidden` - Доступ запрещен
- `404 Not Found` - Ресурс не найден
- `409 Conflict` - Конфликт (например, ресурс уже существует)
- `500 Internal Server Error` - Внутренняя ошибка сервера
