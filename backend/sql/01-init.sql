SET NAMES utf8mb4;
DROP DATABASE IF EXISTS demo;
CREATE DATABASE demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE demo;

CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(50) PRIMARY KEY,
    risk_level VARCHAR(20) NOT NULL,
    risk_type VARCHAR(100) NOT NULL,
    description TEXT,
    vulnerabilities TEXT, -- JSON string
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS scenarios (
    id VARCHAR(50) PRIMARY KEY,
    type VARCHAR(20) NOT NULL, -- VOICE, SMS, EMAIL
    title VARCHAR(100) NOT NULL,
    sender VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    attacker_action VARCHAR(250),
    warning_explanation TEXT,
    steps TEXT NOT NULL,          -- JSON string of steps array
    stage_details TEXT,           -- JSON string of stageDetails array
    sms_email_report TEXT,        -- JSON string of smsEmailReport object
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS training_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    scenario_id VARCHAR(50) NOT NULL,
    action_type VARCHAR(50) NOT NULL, -- CLICK_LINK, ENTERED_DATA, HUNG_UP_SUCCESS, BLOCKED_SMS
    call_duration_seconds INT DEFAULT 0,
    risky_behavior_detected BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

