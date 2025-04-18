-- Создание таблиц для базы данных MoneyGuard

-- Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ai_access_enabled BOOLEAN DEFAULT FALSE,
    subscription_type VARCHAR(50),
    subscription_expiry DATE
);

-- Удаляем старую таблицу, если существует
DROP TABLE IF EXISTS categories CASCADE;

-- Таблица категорий
CREATE TABLE IF NOT EXISTS categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    icon VARCHAR(50),
    parent_id INTEGER REFERENCES categories(id) ON DELETE CASCADE,
    is_income BOOLEAN NOT NULL DEFAULT false,
    is_system BOOLEAN NOT NULL DEFAULT false, 
    color VARCHAR(7),
    icon_name VARCHAR(50),
    UNIQUE (name)
);

-- Таблица целей
CREATE TABLE IF NOT EXISTS goals (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    target_amount DECIMAL(15, 2) NOT NULL,
    current_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    target_date DATE NOT NULL,
    priority VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Таблица транзакций (после users, categories и goals)
CREATE TABLE IF NOT EXISTS transactions (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id INTEGER NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    goal_id INTEGER REFERENCES goals(id) ON DELETE SET NULL,
    amount DECIMAL(15, 2) NOT NULL,
    date DATE NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Таблица лимитов
CREATE TABLE IF NOT EXISTS limits (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id INTEGER NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    amount DECIMAL(15, 2) NOT NULL,
    period VARCHAR(20) NOT NULL,
    UNIQUE (user_id, category_id)
);

-- Таблица подписок
CREATE TABLE IF NOT EXISTS subscriptions (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL CHECK (type IN ('FREE', 'PREMIUM')),
    expires_at DATE,
    UNIQUE (user_id)
);

-- Индексы
CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_category_id ON transactions(category_id);
CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(date);
CREATE INDEX IF NOT EXISTS idx_goals_user_id ON goals(user_id);
CREATE INDEX IF NOT EXISTS idx_limits_user_id ON limits(user_id);

-- Вставка категорий по умолчанию
INSERT INTO categories (name, icon, is_income, is_system, color, icon_name) VALUES
  ('Продукты', 'shopping_cart', FALSE, TRUE, '#4CAF50', 'shopping_cart'),
  ('Транспорт', 'directions_car', FALSE, TRUE, '#2196F3', 'directions_car'),
  ('Развлечения', 'local_play', FALSE, TRUE, '#9C27B0', 'local_play'),
  ('Здоровье', 'local_hospital', FALSE, TRUE, '#F44336', 'local_hospital'),
  ('Одежда', 'checkroom', FALSE, TRUE, '#FF9800', 'checkroom'),
  ('Рестораны', 'restaurant', FALSE, TRUE, '#795548', 'restaurant'),
  ('Подарки (расход)', 'card_giftcard', FALSE, TRUE, '#E91E63', 'card_giftcard'),
  ('Счета', 'receipt_long', FALSE, TRUE, '#607D8B', 'receipt_long'),
  ('Образование', 'school', FALSE, TRUE, '#3F51B5', 'school'),
  ('Путешествия', 'flight_takeoff', FALSE, TRUE, '#00BCD4', 'flight_takeoff'),
  ('Дом', 'home', FALSE, TRUE, '#8BC34A', 'home'),
  ('Другое (расход)', 'help_outline', FALSE, TRUE, '#9E9E9E', 'help_outline'),
  ('Зарплата', 'account_balance_wallet', TRUE, TRUE, '#FFEB3B', 'account_balance_wallet'),
  ('Фриланс', 'work', TRUE, TRUE, '#009688', 'work'),
  ('Подарки (доход)', 'card_giftcard', TRUE, TRUE, '#CDDC39', 'card_giftcard'),
  ('Инвестиции', 'trending_up', TRUE, TRUE, '#03A9F4', 'trending_up'),
  ('Другое (доход)', 'help_outline', TRUE, TRUE, '#9E9E9E', 'help_outline')
ON CONFLICT (name) DO NOTHING;

-- Обновление категорий
UPDATE categories SET is_income = true WHERE name IN ('Зарплата', 'Подарки', 'Инвестиции', 'Подработка');
