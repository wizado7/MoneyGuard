# MoneyGuard API Documentation

## Содержание
1. [Аутентификация и авторизация](#аутентификация-и-авторизация)
2. [Управление профилем](#управление-профилем)
3. [Управление доступом к ИИ](#управление-доступом-к-ии)
4. [Управление категориями](#управление-категориями)
5. [Управление транзакциями](#управление-транзакциями)
6. [Управление лимитами](#управление-лимитами)
7. [Управление целями](#управление-целями)
8. [Аналитика и отчеты](#аналитика-и-отчеты)

## Аутентификация и авторизация

### Регистрация пользователя

**Endpoint**: `/api/v1/auth/register`

**Request**:
```json
{
  "email": "ex@ex.com",
  "password": "Password123",
  "name": "Имя Пользователя"
}
```

**Response** (201 Created):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "1",
    "email": "ex@ex.com",
    "name": "Имя Пользователя"
  },
  "ai_access_enabled": false
}
```

### Вход в систему

**Endpoint**: `/api/v1/auth/login`

**Request**:
```json
{
  "email": "ex@ex.com",
  "password": "Password123"
}
```

**Response** (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "1",
    "email": "ex@ex.com",
    "name": "Имя Пользователя"
  },
  "ai_access_enabled": false
}
```

### Выход из системы

**Endpoint**: `/api/v1/auth/logout`

**Headers**:
- Authorization: Bearer {token}

**Response** (204 No Content)

### Смена пароля

**Endpoint**: `/api/v1/auth/change-password`

**Headers**:
- Authorization: Bearer {token}

**Request**:
```json
{
  "currentPassword": "Password123",
  "newPassword": "Newpassword123"
}
```

**Response** (204 No Content)

## Управление профилем

### Получение профиля

**Endpoint**: `/api/v1/profile`

**Headers**:
- Authorization: Bearer {token}

**Response** (200 OK):
```json
{
  "id": "1",
  "email": "ex@ex.com",
  "name": "Имя Пользователя",
  "profileImage": "https://example.com/image.jpg"
}
```

### Обновление профиля

**Endpoint**: `/api/v1/profile`

**Headers**:
- Authorization: Bearer {token}

**Request**:
```json
{
  "name": "Новое Имя",
  "profileImage": "https://example.com/new-image.jpg"
}
```

**Response** (200 OK):
```json
{
  "id": "1",
  "email": "ex@ex.com",
  "name": "Новое Имя",
  "profileImage": "https://example.com/new-image.jpg"
}
```

## Управление доступом к ИИ

### Получение информации о доступе к ИИ

**Endpoint**: `/api/v1/subscription`

**Headers**:
- Authorization: Bearer {token}

**Response** (200 OK):
```json
{
  "ai_access_enabled": false,
  "features": [
    "Базовый учет доходов и расходов",
    "Неограниченное количество категорий",
    "Неограниченное количество транзакций"
  ]
}
```

### Включение доступа к ИИ

**Endpoint**: `/api/v1/subscription`

**Headers**:
- Authorization: Bearer {token}

**Response** (200 OK):
```json
{
  "ai_access_enabled": true,
  "features": [
    "Базовый учет доходов и расходов",
    "Неограниченное количество категорий",
    "Неограниченное количество транзакций",
    "Доступ к AI-рекомендациям"
  ]
}
```

### Отключение доступа к ИИ

**Endpoint**: `/api/v1/subscription`

**Headers**:
- Authorization: Bearer {token}

**Response** (204 No Content)

## Управление категориями

### Получение списка категорий

**Endpoint**: `/api/v1/categories`

**Headers**:
- Authorization: Bearer {token}

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
  },
  {
    "id": 16,
    "name": "Мои расходы",
    "icon": "account_balance_wallet"
  }
]
```

### Создание категории

**Endpoint**: `/api/v1/categories`

**Headers**:
- Authorization: Bearer {token}

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

**Endpoint**: `/api/v1/categories/{id}`

**Headers**:
- Authorization: Bearer {token}

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

**Endpoint**: `/api/v1/categories/{id}`

**Headers**:
- Authorization: Bearer {token}

**Response** (204 No Content)

## Управление транзакциями

### Получение списка транзакций

**Endpoint**: `/api/v1/transactions`

**Headers**:
- Authorization: Bearer {token}

**Query Parameters**:
- period (optional): day, week, month, year, all

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "amount": -1000,
    "category": "Продукты",
    "date": "2023-04-01",
    "description": "Покупка продуктов в магазине",
    "created_at": "2023-04-01T12:00:00"
  },
  {
    "id": 2,
    "amount": 50000,
    "category": "Зарплата",
    "date": "2023-04-05",
    "description": "Зарплата за март",
    "created_at": "2023-04-05T10:00:00"
  }
]
```

### Создание транзакции

**Endpoint**: `/api/v1/transactions`

**Headers**:
- Authorization: Bearer {token}

**Request**:
```json
{
  "amount": -1000,
  "category": "Продукты",
  "date": "2023-04-01",
  "description": "Покупка продуктов в магазине",
  "goal_id": null
}
```

**Response** (201 Created):
```json
{
  "id": 1,
  "amount": -1000,
  "category": "Продукты",
  "date": "2023-04-01",
  "description": "Покупка продуктов в магазине",
  "goal_id": null,
  "created_at": "2023-04-01T12:00:00"
}
```

### Обновление транзакции

**Endpoint**: `/api/v1/transactions/{id}`

**Headers**:
- Authorization: Bearer {token}

**Request**:
```json
{
  "amount": -1800,
  "category": "Транспорт",
  "date": "2023-04-02",
  "description": "Такси (обновлено)",
  "goal_id": null
}
```

**Response** (200 OK):
```json
{
  "id": 3,
  "amount": -1800,
  "category": "Транспорт",
  "date": "2023-04-02",
  "description": "Такси (обновлено)",
  "created_at": "2023-04-02T15:30:00"
}
```

### Удаление транзакции

**Endpoint**: `/api/v1/transactions/{id}`

**Headers**:
- Authorization: Bearer {token}

**Response** (204 No Content)

## Управление лимитами

### Получение списка лимитов

**Endpoint**: `/api/v1/limits`

**Headers**:
- Authorization: Bearer {token}

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
  },
  {
    "id": 2,
    "category": {
      "id": 2,
      "name": "Транспорт",
      "icon": "directions_car"
    },
    "amount": 5000,
    "period": "MONTHLY",
    "current_spending": 1800
  }
]
```

### Создание лимита

**Endpoint**: `/api/v1/limits`

**Headers**:
- Authorization: Bearer {token}

**Request**:
```json
{
  "category_id": 3,
  "amount": 10000,
  "period": "MONTHLY"
}
```

**Response** (201 Created):
```json
{
  "id": 3,
  "category": {
    "id": 3,
    "name": "Развлечения",
    "icon": "movie"
  },
  "amount": 10000,
  "period": "MONTHLY",
  "current_spending": 0
}
```

### Обновление лимита

**Endpoint**: `/api/v1/limits/{id}`

**Headers**:
- Authorization: Bearer {token}

**Request**:
```json
{
  "category_id": 3,
  "amount": 12000,
  "period": "MONTHLY"
}
```

**Response** (200 OK):
```json
{
  "id": 3,
  "category": {
    "id": 3,
    "name": "Развлечения",
    "icon": "movie"
  },
  "amount": 12000,
  "period": "MONTHLY",
  "current_spending": 0
}
```

### Удаление лимита

**Endpoint**: `/api/v1/limits/{id}`

**Headers**:
- Authorization: Bearer {token}

**Response** (204 No Content)

## Управление целями

### Получение списка целей

**Endpoint**: `/api/v1/goals`

**Headers**:
- Authorization: Bearer {token}

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "name": "Новый автомобиль",
    "target_amount": 1000000,
    "current_amount": 150000,
    "target_date": "2024-12-31",
    "priority": "HIGH"
  },
  {
    "id": 2,
    "name": "Отпуск",
    "target_amount": 200000,
    "current_amount": 50000,
    "target_date": "2023-07-01",
    "priority": "MEDIUM"
  }
]
```

### Получение детальной информации о цели

**Endpoint**: `/api/v1/goals/{id}`

**Headers**:
- Authorization: Bearer {token}

**Response** (200 OK):
```json
{
  "goal": {
    "id": 1,
    "name": "Новый автомобиль",
    "target_amount": 1000000,
    "current_amount": 150000,
    "target_date": "2024-12-31",
    "priority": "HIGH"
  },
  "transactions": [
    {
      "id": 5,
      "amount": 50000,
      "category": "Накопления",
      "date": "2023-03-01",
      "description": "Перевод на цель",
      "created_at": "2023-03-01T10:00:00"
    },
    {
      "id": 8,
      "amount": 100000,
      "category": "Накопления",
      "date": "2023-04-01",
      "description": "Перевод на цель",
      "created_at": "2023-04-01T10:00:00"
    }
  ],
  "progress": 15
}
```

### Создание цели

**Endpoint**: `/api/v1/goals`

**Headers**:
- Authorization: Bearer {token}

**Request**:
```json
{
  "name": "Новый ноутбук",
  "description": "Накопить на новый ноутбук",
  "target_amount": 150000,
  "target_date": "2023-09-01",
  "priority": "MEDIUM"
}
```

**Response** (201 Created):
```json
{
  "id": 3,
  "name": "Новый ноутбук",
  "description": "Накопить на новый ноутбук",
  "target_amount": 150000,
  "current_amount": 0,
  "target_date": "2023-09-01",
  "priority": "MEDIUM",
  "days_left": 153,
  "daily_contribution": 980.39,
  "monthly_contribution": 29411.76,
  "created_at": "2023-04-01T12:00:00"
}
```

### Обновление цели

**Endpoint**: `/api/v1/goals/{id}`

**Headers**:
- Authorization: Bearer {token}

**Request**:
```json
{
  "name": "Новый ноутбук",
  "description": "Накопить на новый ноутбук MacBook Pro",
  "target_amount": 180000,
  "target_date": "2023-10-01",
  "priority": "HIGH"
}
```

**Response** (200 OK):
```json
{
  "id": 3,
  "name": "Новый ноутбук",
  "description": "Накопить на новый ноутбук MacBook Pro",
  "target_amount": 180000,
  "current_amount": 0,
  "target_date": "2023-10-01",
  "priority": "HIGH",
  "days_left": 183,
  "daily_contribution": 983.61,
  "monthly_contribution": 29508.20,
  "created_at": "2023-04-01T12:00:00"
}
```

### Удаление цели

**Endpoint**: `/api/v1/goals/{id}`

**Headers**:
- Authorization: Bearer {token}

**Response** (204 No Content)

## Аналитика и отчеты

### Получение отчета за период

**Endpoint**: `/api/v1/reports`

**Headers**:
- Authorization: Bearer {token}

**Query Parameters**:
- period: Период отчета (DAILY, WEEKLY, MONTHLY, YEARLY)
- dateFrom (optional): Дата начала периода (формат: YYYY-MM-DD)
- dateTo (optional): Дата окончания периода (формат: YYYY-MM-DD)

**Response** (200 OK):
```json
{
  "period": "MONTHLY",
  "income": 50000,
  "expenses": 25000,
  "balance": 25000,
  "categories": [
    {
      "category": "Продукты",
      "amount": 10000,
      "percentage": 40
    },
    {
      "category": "Транспорт",
      "amount": 5000,
      "percentage": 20
    },
    {
      "category": "Развлечения",
      "amount": 8000,
      "percentage": 32
    },
    {
      "category": "Прочее",
      "amount": 2000,
      "percentage": 8
    }
  ]
}
```

### Получение аналитики

**Endpoint**: `/api/v1/analytics`

**Headers**:
- Authorization: Bearer {token}

**Query Parameters**:
- period: Период анализа (MONTHLY, YEARLY)

**Response** (200 OK):
```json
{
  "period": "MONTHLY",
  "categories": {
    "Продукты": 10000,
    "Транспорт": 5000,
    "Развлечения": 8000,
    "Прочее": 2000
  },
  "anomalies": [
    "Расходы на развлечения выросли на 60% по сравнению с предыдущим месяцем"
  ],
  "forecast": {
    "balance": 20000,
    "risk_level": "low"
  }
}
```

### Получение AI-рекомендаций

**Endpoint**: `/api/v1/ai/chat`

**Headers**:
- Authorization: Bearer {token}

**Request**:
```json
{
  "message": "Как мне оптимизировать расходы на продукты?"
}
```

**Response** (200 OK):
```json
{
  "message": "Вот несколько рекомендаций по оптимизации расходов на продукты:",
  "advice": [
    "Составляйте список покупок заранее и придерживайтесь его",
    "Сравнивайте цены в разных магазинах",
    "Покупайте сезонные продукты",
    "Используйте программы лояльности и купоны"
  ],
  "actions": [
    "Установить лимит на категорию 'Продукты'",
    "Создать подкатегории для лучшего отслеживания расходов"
  ]
}
``` 
