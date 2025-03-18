CREATE TABLE "User" (
    ID SERIAL PRIMARY KEY,
    Name VARCHAR(50) NOT NULL,
    Surname VARCHAR(50) NOT NULL,
    Email VARCHAR(255) NOT NULL UNIQUE,
    Phone VARCHAR(20),
    AvatarLink TEXT
);

CREATE TABLE "Transaction" (
    ID SERIAL PRIMARY KEY,                 
    UserID INT NOT NULL,
    Amount DECIMAL(10, 2) NOT NULL,         -- Сумма транзакции
    Category VARCHAR(50) NOT NULL,          -- Категория транзакции
    Date DATE NOT NULL,                     -- Дата транзакции
    Description TEXT,                       -- Описание транзакции (необязательное поле)
    FOREIGN KEY (UserID) REFERENCES "User"(ID) ON DELETE CASCADE
);

CREATE TABLE "Goal" (
    ID SERIAL PRIMARY KEY,
    UserID INT NOT NULL,
    Name VARCHAR(100) NOT NULL,             -- Название цели
    TargetAmount DECIMAL(10, 2) NOT NULL,   -- Целевая сумма
    CurrentProgress DECIMAL(10, 2) DEFAULT 0, -- Текущий прогресс
    EndDate DATE NOT NULL,                  -- Дата окончания цели
    FOREIGN KEY (UserID) REFERENCES "User"(ID) ON DELETE CASCADE
);


CREATE TABLE "Report" (
    ID SERIAL PRIMARY KEY,
    UserID INT NOT NULL,
    Period VARCHAR(50) NOT NULL,            -- Период отчета
    Data JSON NOT NULL,                     -- Данные отчета в формате JSON
    FOREIGN KEY (UserID) REFERENCES "User"(ID) ON DELETE CASCADE
);


CREATE INDEX idx_transaction_user ON "Transaction"(UserID);      -- для поиска транзакций по пользователю
CREATE INDEX idx_goal_user ON "Goal"(UserID);                    -- для поиска целей по пользователю
CREATE INDEX idx_report_user ON "Report"(UserID);                -- для поиска отчетов по пользователю
CREATE INDEX idx_transaction_date ON "Transaction"(Date);        -- для поиска транзакций по дате
CREATE INDEX idx_goal_enddate ON "Goal"(EndDate);                -- для поиска целей по дате окончания